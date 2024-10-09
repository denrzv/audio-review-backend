package io.github.denrzv.audioreview.repository;

import io.github.denrzv.audioreview.model.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {
    
    boolean existsByInitialCategoryId(Long id);
    
    boolean existsByCurrentCategoryId(Long id);

    Optional<AudioFile> findByFilename(String filename);
}