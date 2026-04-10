package com.mycompany.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuessRequest {

    @NotNull(message = "guessNumber is required")
    @Min(value = 1, message = "guessNumber must be between 1 and 5")
    @Max(value = 5, message = "guessNumber must be between 1 and 5")
    private Integer guessNumber;
}
