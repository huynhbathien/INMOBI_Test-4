package com.mycompany.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuessResponse {
    private int guessNumber;
    private int serverNumber;
    private boolean win;
    private int score;
    private int turns;
}
