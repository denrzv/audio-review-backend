package io.github.denrzv.audioreview.repository;

import io.github.denrzv.audioreview.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Find user by username.
     *
     * @param username the username
     * @return Optional containing the User if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a username already exists.
     *
     * @param username the username
     * @return true if exists, false otherwise
     */
    boolean existsByUsername(String username);
}