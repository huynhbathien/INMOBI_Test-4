package com.mycompany.service;

import java.util.List;

import com.mycompany.dto.response.BuyTurnsResponse;
import com.mycompany.dto.response.GameProfileResponse;
import com.mycompany.dto.response.GuessResponse;
import com.mycompany.dto.response.LeaderboardItemResponse;
import com.mycompany.dto.response.LeaderboardMeResponse;

public interface GameService {

    GuessResponse guess(String username, int guessNumber);

    BuyTurnsResponse buyTurns(String username);

    List<LeaderboardItemResponse> leaderboard();

    GameProfileResponse me(String username);

    LeaderboardMeResponse leaderboardMe(String username);
}
