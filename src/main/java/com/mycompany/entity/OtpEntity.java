package com.mycompany.entity;

import java.time.LocalDateTime;

import com.mycompany.enums.OtpType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "otps", indexes = {
        @Index(name = "idx_otp_email_type", columnList = "email, otp_type"),
        @Index(name = "idx_otp_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
public class OtpEntity extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpType otpType;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}
