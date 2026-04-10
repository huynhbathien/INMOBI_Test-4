package com.mycompany.service.Impl;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.dto.response.BuyTurnsResponse;
import com.mycompany.dto.response.GameProfileResponse;
import com.mycompany.dto.response.GuessResponse;
import com.mycompany.dto.response.LeaderboardItemResponse;
import com.mycompany.dto.response.LeaderboardMeResponse;
import com.mycompany.entity.UserEntity;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.enums.EnumGame;
import com.mycompany.mapstruct.GameMapper;
import com.mycompany.repository.UserRepository;
import com.mycompany.service.GameService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 5;
    private static final int TURN_PACKAGE = 5;

    private final UserRepository userRepository;
    private final GameMapper gameMapper;
    private final SecureRandom random = new SecureRandom();

    @Value("${game.win-rate:0.05}")
    private double winRate;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "leaderboardTop10", allEntries = true)
    public GuessResponse guess(String username, int guessNumber) {
        UserEntity user = getUserForUpdate(username);

        if (user.getTurns() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, EnumGame.NO_TURNS_LEFT.getMessage());
        }

        int serverNumber = resolveServerNumber(guessNumber);
        user.setTurns(user.getTurns() - 1);

        boolean win = serverNumber == guessNumber;
        if (win) {
            user.setScore(user.getScore() + 1);
        }

        userRepository.save(user);

        return GuessResponse.builder()
                .guessNumber(guessNumber)
                .serverNumber(serverNumber)
                .win(win)
                .score(user.getScore())
                .turns(user.getTurns())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "leaderboardTop10", allEntries = true)
    public BuyTurnsResponse buyTurns(String username) {
        UserEntity user = getUserForUpdate(username);

        user.setTurns(user.getTurns() + TURN_PACKAGE);
        userRepository.save(user);

        return BuyTurnsResponse.builder()
                .addedTurns(TURN_PACKAGE)
                .turns(user.getTurns())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "leaderboardTop10", key = "'default'")
    public List<LeaderboardItemResponse> leaderboard() {
        return userRepository.findTopLeaderboard(PageRequest.of(0, 10)).stream()
                .map(gameMapper::toLeaderboardItemResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GameProfileResponse me(String username) {
        return gameMapper.toGameProfileResponse(getUser(username));
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardMeResponse leaderboardMe(String username) {
        UserEntity user = getUser(username);
        long rank = userRepository.findRankByScoreAndId(user.getScore(), user.getId());

        return LeaderboardMeResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .score(user.getScore())
                .turns(user.getTurns())
                .rank(rank)
                .build();
    }

    private UserEntity getUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));
        ensureUserIsActive(user);
        return user;
    }

    private UserEntity getUserForUpdate(String username) {
        UserEntity user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        EnumAuthError.USER_NOT_FOUND.getMessage()));
        ensureUserIsActive(user);
        return user;
    }

    private void ensureUserIsActive(UserEntity user) {
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    EnumAuthError.ACCOUNT_DISABLED.getMessage());
        }
    }

    int resolveServerNumber(int guessNumber) {
        double normalizedWinRate = Math.max(0d, Math.min(1d, winRate));

        if (normalizedWinRate <= 0d) {
            return randomNumberExcluding(guessNumber);
        }
        if (normalizedWinRate >= 1d) {
            return guessNumber;
        }

        if (random.nextDouble() < normalizedWinRate) {
            return guessNumber;
        }
        return randomNumberExcluding(guessNumber);
    }

    private int randomNumberExcluding(int excluded) {
        int value = random.nextInt(MAX_NUMBER - MIN_NUMBER) + MIN_NUMBER;
        if (value >= excluded) {
            return value + 1;
        }
        return value;
    }
}
