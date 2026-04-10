package com.mycompany.dto.request;

import com.mycompany.enums.OtpType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ResendOtpRequestDTO {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    String email;

    @NotNull(message = "OTP type cannot be null")
    OtpType otpType;
}
