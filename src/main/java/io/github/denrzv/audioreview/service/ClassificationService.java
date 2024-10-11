package io.github.denrzv.audioreview.service;

import io.github.denrzv.audioreview.config.AppConfig;
import io.github.denrzv.audioreview.dto.AudioFileResponse;
import io.github.denrzv.audioreview.dto.ClassificationRequest;
import io.github.denrzv.audioreview.dto.ClassificationResponse;
import io.github.denrzv.audioreview.model.AudioFile;
import io.github.denrzv.audioreview.model.Category;
import io.github.denrzv.audioreview.model.Classification;
import io.github.denrzv.audioreview.model.User;
import io.github.denrzv.audioreview.repository.AudioFileRepository;
import io.github.denrzv.audioreview.repository.CategoryRepository;
import io.github.denrzv.audioreview.repository.ClassificationRepository;
import io.github.denrzv.audioreview.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ClassificationService {

    private AudioFileRepository audioFileRepository;

    private ClassificationRepository classificationRepository;

    private CategoryRepository categoryRepository;

    private UserRepository userRepository;
    private AppConfig appConfig;
    private static final Logger logger = LoggerFactory.getLogger(ClassificationService.class);


    @Transactional
    public AudioFileResponse getRandomUnclassifiedFile(Long userId) {

        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(Long.parseLong(appConfig.getUserLockMinutes()));

        // Attempt to find an unclassified file, prioritizing those locked by the current user
        AudioFile file = audioFileRepository.findRandomUnclassifiedUnlockedFile(userId, expirationTime)
                .orElseThrow(() -> new IllegalStateException("No unclassified files available."));

        // Lock the file if it’s not already locked by the current user
        if (!userId.equals(file.getLockedBy())) {
            file = audioFileRepository.findByIdWithLock(file.getId())
                    .orElseThrow(() -> new RuntimeException("File not found for locking"));

            file.setLockedBy(userId);
            file.setLockedAt(LocalDateTime.now());
            audioFileRepository.save(file);
        }

        String filePath = String.format("%s/admin/audio/files/%s", appConfig.getFileServerUrl(), file.getFilename());

        return new AudioFileResponse(
                file.getId(),
                file.getFilename(),
                file.getInitialCategory().getName(),
                file.getUploadedAt(),
                file.getUploadedBy().getUsername(),
                file.getCurrentCategory().getName(),
                filePath
        );
    }

    @Transactional
    public AudioFileResponse classifyFile(Long fileId, ClassificationRequest request) {
        try {
            // Fetch and lock the file using a separate locking query
            AudioFile file = audioFileRepository.findByIdWithLock(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            // Fetch the category without saving it directly to avoid cascade issues
            Category newCategory = categoryRepository.findByNameEqualsIgnoreCase(request.getCategory())
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Classification classification = Classification.builder()
                    .audioFile(file)
                    .user(user)
                    .previousCategory(file.getCurrentCategory())
                    .newCategory(newCategory)
                    .classifiedAt(LocalDateTime.now())
                    .build();
            classificationRepository.save(classification);

            file.setCurrentCategory(newCategory);
            audioFileRepository.save(file);

            // Unlock the file after classification
            unlockFile(fileId);

            return new AudioFileResponse(
                    file.getId(), file.getFilename(),
                    file.getInitialCategory().getName(), file.getUploadedAt(),
                    file.getUploadedBy().getUsername(), newCategory.getName(),
                    file.getFilepath()
            );
        } catch (OptimisticLockingFailureException ex) {
            logger.error("This file was modified by another user. Please try again. Request: " + request);
            throw new ConcurrentModificationException("This file was modified by another user. Please try again.");
        }
    }

    public void unlockFile(Long fileId) {
        Optional<AudioFile> fileOpt = audioFileRepository.findById(fileId);
        fileOpt.ifPresent(file -> {
            file.setLockedBy(null);
            file.setLockedAt(null);
            audioFileRepository.save(file);
        });
    }

    public Page<ClassificationResponse> getClassificationHistoryForUser(String username, int page, int pageSize) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Classification> classifications = classificationRepository.findByUserOrderByClassifiedAtDesc(user, pageable);

        return classifications.map(classification -> new ClassificationResponse(
                classification.getAudioFile().getId(),
                classification.getAudioFile().getFilename(),
                String.format("%s/admin/audio/files/%s", appConfig.getFileServerUrl(), classification.getAudioFile().getFilename()),
                classification.getNewCategory().getName(),
                classification.getClassifiedAt()));
    }
}