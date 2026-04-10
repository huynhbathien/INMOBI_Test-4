package com.mycompany.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum EnumGame {
    GUESS_PROCESSED(500, "Guess processed"),
    TURNS_PURCHASED(501, "Turns purchased"),
    LEADERBOARD_FETCHED(502, "Top 10 leaderboard"),
    PROFILE_FETCHED(503, "Current user game profile"),
    LEADERBOARD_ME_FETCHED(504, "Current user leaderboard profile"),
    NO_TURNS_LEFT(540, "No turns left. Please buy more turns.");

    int code;
    String message;

    EnumGame(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
