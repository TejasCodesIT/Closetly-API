package com.closetly.closetly_backend.user.repository;

import com.closetly.closetly_backend.user.entity.User;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = { "roles" })
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByVerificationToken(String verificationToken);

    long countByCreatedAtAfter(LocalDateTime dateTime);
}
