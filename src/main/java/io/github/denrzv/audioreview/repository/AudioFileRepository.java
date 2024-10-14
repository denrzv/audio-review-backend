package io.github.denrzv.audioreview.repository;

import io.github.denrzv.audioreview.model.AudioFile;
import jakarta.persistence.LockModeType;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {

    boolean existsByInitialCategoryId(Long id);

    boolean existsByCurrentCategoryId(Long id);

    Optional<AudioFile> findByFilename(String filename);

    Page<AudioFile> findByFilenameContainingIgnoreCase(String filename, Pageable pageable);
    List<AudioFile> findByFilenameContainingIgnoreCase(String filename);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @NonNull
    Optional<AudioFile> findById(@NonNull Long id);

    @Query("SELECT f FROM AudioFile f " +
            "WHERE f.currentCategory.name = 'Unclassified' " +
            "AND ((f.lockedBy = :userId) " + // Prioritize files locked by the current user
            "OR (f.lockedBy IS NULL OR f.lockedAt < :expirationTime)) " + // Otherwise, find unlocked or expired locks
            "ORDER BY function('RANDOM') " +
    "LIMIT 1")
    Optional<AudioFile> findRandomUnclassifiedUnlockedFile(
            @Param("userId") Long userId,
            @Param("expirationTime") LocalDateTime expirationTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM AudioFile f WHERE f.id = :id")
    Optional<AudioFile> findByIdWithLock(@Param("id") Long id);
}