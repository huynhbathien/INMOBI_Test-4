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
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    String email;

    @NotBlank(message = "OTP code cannot be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6-digit number")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    String otpCode;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character (@$!%*?&)")
    String newPassword;

    @NotBlank(message = "Confirm password cannot be blank")
    String confirmPassword;
}
