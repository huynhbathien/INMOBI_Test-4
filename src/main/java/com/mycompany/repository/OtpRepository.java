package com.mycompany.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mycompany.entity.OtpEntity;
import com.mycompany.enums.OtpType;

@Repository
public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    Optional<OtpEntity> findTopByEmailAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(String email, OtpType otpType);

    @Modifying
    @Query("DELETE FROM OtpEntity o WHERE o.email = :email AND o.otpType = :otpType")
    void deleteByEmailAndOtpType(@Param("email") String email, @Param("otpType") OtpType otpType);

    @Modifying
    @Query("DELETE FROM OtpEntity o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
}
