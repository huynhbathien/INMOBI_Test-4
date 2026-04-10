package com.mycompany.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class VerifyOtpRequestDTO {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    String email;

    @NotBlank(message = "OTP code cannot be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6-digit number")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    String otpCode;
}
