package com.mycompany.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    String email;
}
