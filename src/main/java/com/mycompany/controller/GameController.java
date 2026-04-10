package com.mycompany.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.dto.APIResponse;
import com.mycompany.dto.request.GuessRequest;
import com.mycompany.dto.response.BuyTurnsResponse;
import com.mycompany.dto.response.GameProfileResponse;
import com.mycompany.dto.response.GuessResponse;
import com.mycompany.dto.response.LeaderboardItemResponse;
import com.mycompany.dto.response.LeaderboardMeResponse;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.enums.EnumGame;
import com.mycompany.service.GameService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/guess")
    public APIResponse<Object> guess(@Valid @RequestBody GuessRequest request) {
        GuessResponse response = gameService.guess(resolveAuthenticatedUsername(), request.getGuessNumber());
        return APIResponse.success(EnumGame.GUESS_PROCESSED.getCode(), EnumGame.GUESS_PROCESSED.getMessage(), response);
    }

    @PostMapping("/buy-turns")
    public APIResponse<Object> buyTurns() {
        BuyTurnsResponse response = gameService.buyTurns(resolveAuthenticatedUsername());
        return APIResponse.success(EnumGame.TURNS_PURCHASED.getCode(), EnumGame.TURNS_PURCHASED.getMessage(), response);
    }

    @GetMapping("/leaderboard")
    public APIResponse<Object> leaderboard() {
        List<LeaderboardItemResponse> response = gameService.leaderboard();
        return APIResponse.success(EnumGame.LEADERBOARD_FETCHED.getCode(), EnumGame.LEADERBOARD_FETCHED.getMessage(),
                response);
    }

    @GetMapping("/me")
    public APIResponse<Object> me() {
        GameProfileResponse response = gameService.me(resolveAuthenticatedUsername());
        return APIResponse.success(EnumGame.PROFILE_FETCHED.getCode(), EnumGame.PROFILE_FETCHED.getMessage(), response);
    }

    @GetMapping("/leaderboard/me")
    public APIResponse<Object> leaderboardMe() {
        LeaderboardMeResponse response = gameService.leaderboardMe(resolveAuthenticatedUsername());
        return APIResponse.success(EnumGame.LEADERBOARD_ME_FETCHED.getCode(),
                EnumGame.LEADERBOARD_ME_FETCHED.getMessage(),
                response);
    }

    private String resolveAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.UNAUTHORIZED.getMessage());
        }
        return auth.getName();
    }
}
