package io.github.denrzv.audioreview.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ClassificationService {

    private AudioFileRepository audioFileRepository;

    private ClassificationRepository classificationRepository;

    private CategoryRepository categoryRepository;

    private UserRepository userRepository;
    private final Path fileStorageLocation = Paths.get("uploads");

    public AudioFileResponse getRandomUnclassifiedFile() {
        List<AudioFile> unclassifiedFiles = audioFileRepository.findAll().stream()
                .filter(file -> file.getCurrentCategory().getName().equalsIgnoreCase("Unclassified"))
                .toList();

        if (unclassifiedFiles.isEmpty()) {
            throw new RuntimeException("No unclassified files available.");
        }

        AudioFile file = unclassifiedFiles.get(new Random().nextInt(unclassifiedFiles.size()));

        // Construct full URL for filePath
        String filePath = String.format("http://localhost:8080/admin/audio/files/%s", file.getFilename());

        return new AudioFileResponse(file.getId(), file.getFilename(),
                file.getInitialCategory().getName(), file.getUploadedAt(), file.getUploadedBy().getUsername(),
                file.getCurrentCategory() == null ? "Unclassified" : file.getCurrentCategory().getName(),
                filePath);
    }

    public AudioFileResponse classifyFile(Long fileId, ClassificationRequest request) {
        AudioFile file = audioFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Category newCategory = categoryRepository.findByNameEqualsIgnoreCase(request.getCategory())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Get the current logged-in user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save classification history
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

        return new AudioFileResponse(file.getId(), file.getFilename(),
                file.getInitialCategory().getName(), file.getUploadedAt(), file.getUploadedBy().getUsername(),
                newCategory.getName(), file.getFilepath());
    }

    public Page<ClassificationResponse> getClassificationHistoryForUser(String username, int page, int pageSize) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Classification> classifications = classificationRepository.findByUserOrderByClassifiedAtDesc(user, pageable);

        return classifications.map(classification -> new ClassificationResponse(
                classification.getAudioFile().getId(),
                classification.getAudioFile().getFilename(),
                String.format("http://localhost:8080/admin/audio/files/%s", classification.getAudioFile().getFilename()),
                classification.getNewCategory().getName(),
                classification.getClassifiedAt()));
    }
}