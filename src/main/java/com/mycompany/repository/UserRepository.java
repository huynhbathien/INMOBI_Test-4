package com.mycompany.repository;

import com.mycompany.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByGoogleId(String googleId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.active = true AND u.username = :username")
    Optional<UserEntity> findActiveUserByUsername(@Param("username") String username);

    @Query("SELECT u FROM UserEntity u WHERE u.active = true AND u.email = :email")
    Optional<UserEntity> findActiveUserByEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.username = :username")
    Optional<UserEntity> findByUsernameForUpdate(@Param("username") String username);

    interface LeaderboardProjection {
        String getUsername();

        String getEmail();

        Integer getScore();
    }

    @Query("SELECT u.username AS username, u.email AS email, u.score AS score " +
            "FROM UserEntity u WHERE u.active = true ORDER BY u.score DESC, u.id ASC")
    List<LeaderboardProjection> findTopLeaderboard(Pageable pageable);

        @Query("SELECT COUNT(u) + 1 FROM UserEntity u " +
            "WHERE u.active = true AND (u.score > :score OR (u.score = :score AND u.id < :userId))")
        long findRankByScoreAndId(@Param("score") int score, @Param("userId") Long userId);

    // Admin queries
    Page<UserEntity> findAll(Pageable pageable);

    Page<UserEntity> findByRole(String role, Pageable pageable);

    Page<UserEntity> findByActive(boolean active, Pageable pageable);

    Page<UserEntity> findByRoleAndActive(String role, boolean active, Pageable pageable);

    long countByActive(boolean active);

    long countByRole(String role);
}
