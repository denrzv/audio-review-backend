package io.github.denrzv.audioreview.service;

import io.github.denrzv.audioreview.dto.AudioFileResponse;
import io.github.denrzv.audioreview.model.AudioFile;
import io.github.denrzv.audioreview.model.Category;
import io.github.denrzv.audioreview.model.User;
import io.github.denrzv.audioreview.repository.AudioFileRepository;
import io.github.denrzv.audioreview.repository.CategoryRepository;
import io.github.denrzv.audioreview.repository.UserRepository;
import jakarta.transaction.TransactionScoped;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AudioFileService {

    private final AudioFileRepository audioFileRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public AudioFileResponse uploadFile(MultipartFile file) {
        // Get the current logged-in user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String categoryName = extractCategoryFromFileName(Objects.requireNonNull(file.getOriginalFilename()));

        // Find initial category based on file naming convention
        Category initialCategory = categoryRepository.findByNameEqualsIgnoreCase(categoryName)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Retrieve "Unclassified" category from the database
        Category unclassifiedCategory = categoryRepository.findByNameEqualsIgnoreCase("Unclassified")
                .orElseThrow(() -> new RuntimeException("Unclassified category not found"));

        // Store the file in the filesystem
        String filePath = fileStorageService.storeFile(file);

        // Save file metadata in the database with the retrieved "Unclassified" category
        AudioFile audioFile = AudioFile.builder()
                .filename(file.getOriginalFilename())
                .filepath(filePath)
                .initialCategory(initialCategory)
                .currentCategory(unclassifiedCategory)
                .uploadedBy(user)
                .uploadedAt(LocalDateTime.now())
                .build();

        AudioFile savedFile = audioFileRepository.save(audioFile);

        return new AudioFileResponse(
                savedFile.getId(),
                savedFile.getFilename(),
                savedFile.getInitialCategory().getName(),
                savedFile.getUploadedAt(),
                savedFile.getUploadedBy().getUsername(),
                unclassifiedCategory.getName(), savedFile.getFilepath()
        );
    }

    public AudioFile getFileByFilename(String filename) {
        return audioFileRepository.findByFilename(filename)
                .orElseThrow(() -> new RuntimeException("File not found with filename: " + filename));
    }

    public Map<String, Object> getAllFiles(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page, pageSize);
        Page<AudioFile> pagedFiles = audioFileRepository.findAll(pageable);

        List<AudioFileResponse> files = new ArrayList<>();
        for (AudioFile file : pagedFiles.getContent()) {
            AudioFileResponse audioFileResponse = new AudioFileResponse(
                    file.getId(),
                    file.getFilename(),
                    file.getInitialCategory().getName(),
                    file.getUploadedAt(),
                    file.getUploadedBy().getUsername(),
                    file.getCurrentCategory() != null ? file.getCurrentCategory().getName() : "Unclassified",
                    file.getFilepath()
            );
            files.add(audioFileResponse);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", files);
        response.put("total", pagedFiles.getTotalElements()); // Total count of files
        return response;
    }

    private String extractCategoryFromFileName(String fileName) {
        Pattern pattern = Pattern.compile("^(voice|silent|answering_machine)_.*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fileName.toLowerCase());
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return "undefined";
    }

    public Map<String, Object> getDashboardStats() {
        long totalFiles = audioFileRepository.count();

        Map<String, Long> filesByInitialCategory = audioFileRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        file -> file.getInitialCategory().getName(),
                        Collectors.counting()
                ));

        Map<String, Long> filesByCurrentCategory = audioFileRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        file -> file.getCurrentCategory().getName(),
                        Collectors.counting()
                ));

        Map<String, Long> filesByUser = audioFileRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        file -> file.getUploadedBy().getUsername(),
                        Collectors.counting()
                ));

        long reclassifiedCount = audioFileRepository.findAll().stream()
                .filter(file -> !file.getCurrentCategory().getName().equals("Unclassified"))
                .count();

        long filesToClassifyCount = audioFileRepository.findAll().stream()
                .filter(file -> file.getCurrentCategory().getName().equals("Unclassified"))
                .count();

        return Map.of(
                "totalFiles", totalFiles,
                "filesByInitialCategory", filesByInitialCategory,
                "filesByCurrentCategory", filesByCurrentCategory,
                "filesByUser", filesByUser,
                "reclassifiedCount", reclassifiedCount,
                "filesToClassifyCount", filesToClassifyCount
        );
    }


    @Transactional
    public void deleteFileById(Long id) {
        audioFileRepository.deleteById(id);
    }

    @Transactional
    public AudioFileResponse updateFileProperties(Long id, AudioFileResponse updatedFile) {
        AudioFile file = audioFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        file.setFilename(updatedFile.getFilename());
        file.setCurrentCategory(new Category(updatedFile.getCurrentCategory()));
        audioFileRepository.save(file);

        return new AudioFileResponse(file.getId(), file.getFilename(), file.getInitialCategory().getName(),
                file.getUploadedAt(), file.getUploadedBy().getUsername(),
                file.getCurrentCategory().getName(), file.getFilepath());
    }

    @Transactional
    public void deleteAllFiles() {
        audioFileRepository.deleteAll();
    }

    @Transactional
    public void deleteMultipleFiles(List<Long> fileIds) {
        audioFileRepository.deleteAllById(fileIds);
    }

    @Transactional
    public void updateMultipleFiles(List<Long> fileIds, String currentCategoryName) {
        Category category = categoryRepository.findByNameEqualsIgnoreCase(currentCategoryName)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        List<AudioFile> files = audioFileRepository.findAllById(fileIds);
        for (AudioFile file : files) {
            file.setCurrentCategory(category);
        }
        audioFileRepository.saveAll(files);
    }
}
