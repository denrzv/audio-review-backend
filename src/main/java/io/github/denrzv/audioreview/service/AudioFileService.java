package io.github.denrzv.audioreview.service;

import io.github.denrzv.audioreview.dto.AudioFileResponse;
import io.github.denrzv.audioreview.dto.CategoryResponse;
import io.github.denrzv.audioreview.model.AudioFile;
import io.github.denrzv.audioreview.model.Category;
import io.github.denrzv.audioreview.model.User;
import io.github.denrzv.audioreview.repository.AudioFileRepository;
import io.github.denrzv.audioreview.repository.CategoryRepository;
import io.github.denrzv.audioreview.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class AudioFileService {

    private final AudioFileRepository audioFileRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CategoryService categoryService;
    private static final String CATEGORY_NOT_FOUND = "Category not found";
    private static final String FILE_NOT_FOUND = "File not found";
    private static final String UNCLASSIFIED = "Unclassified";

    @Transactional
    public AudioFileResponse uploadFile(MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String categoryName = extractCategoryFromFileName(file.getOriginalFilename());

        // Find initial category based on file naming convention
        Category initialCategory = categoryRepository.findByNameEqualsIgnoreCase(categoryName)
                .orElseThrow(() -> new RuntimeException(CATEGORY_NOT_FOUND));

        Category unclassifiedCategory = categoryRepository.findByNameEqualsIgnoreCase(UNCLASSIFIED)
                .orElseThrow(() -> new RuntimeException("Unclassified category not found"));

        String filePath = fileStorageService.storeFile(file);

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

    public Map<String, Object> getAllFiles(int page, int pageSize, String filename) {
        PageRequest pageable = PageRequest.of(page, pageSize);
        Page<AudioFile> pagedFiles;

        if (filename != null && !filename.isEmpty()) {
            pagedFiles = audioFileRepository.findByFilenameContainingIgnoreCase(filename, pageable);
        } else {
            pagedFiles = audioFileRepository.findAll(pageable);
        }

        List<AudioFileResponse> files = pagedFiles.getContent().stream().map(file ->
                new AudioFileResponse(
                        file.getId(),
                        file.getFilename(),
                        file.getInitialCategory().getName(),
                        file.getUploadedAt(),
                        file.getUploadedBy().getUsername(),
                        file.getCurrentCategory().getName(),
                        file.getFilepath()
                )
        ).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("data", files);
        response.put("total", pagedFiles.getTotalElements());

        return response;
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
                .filter(file -> !file.getCurrentCategory().getName().equals(UNCLASSIFIED))
                .count();

        long filesToClassifyCount = audioFileRepository.findAll().stream()
                .filter(file -> file.getCurrentCategory().getName().equals(UNCLASSIFIED))
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
        AudioFile file = audioFileRepository.findByIdWithLock(id)
                .orElseThrow(() -> new RuntimeException(FILE_NOT_FOUND));

        file.setFilename(updatedFile.getFilename());

        // Fetch category by name without adding it directly to avoid cascade issues
        Category category = categoryRepository.findByName(updatedFile.getCurrentCategory())
                .orElseThrow(() -> new RuntimeException(CATEGORY_NOT_FOUND));

        file.setCurrentCategory(category);

        audioFileRepository.save(file);

        return new AudioFileResponse(
                file.getId(),
                file.getFilename(),
                file.getInitialCategory().getName(),
                file.getUploadedAt(),
                file.getUploadedBy().getUsername(),
                file.getCurrentCategory().getName(),
                file.getFilepath()
        );
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
                .orElseThrow(() -> new RuntimeException(CATEGORY_NOT_FOUND));
        List<AudioFile> files = audioFileRepository.findAllById(fileIds);
        for (AudioFile file : files) {
            file.setCurrentCategory(category);
        }
        audioFileRepository.saveAll(files);
    }

    public List<AudioFileResponse> getAllFilesWithoutPagination(String filename) {
        List<AudioFile> files;
        if (filename != null && !filename.isEmpty()) {
            files = audioFileRepository.findByFilenameContainingIgnoreCase(filename);
        } else {
            files = audioFileRepository.findAll();
        }

        return files.stream().map(file -> new AudioFileResponse(
                file.getId(),
                file.getFilename(),
                file.getInitialCategory().getName(),
                file.getUploadedAt(),
                file.getUploadedBy().getUsername(),
                file.getCurrentCategory().getName(),
                file.getFilepath()
        )).toList();
    }

    public String extractCategoryFromFileName(String fileName) {
        List<String> categoryNames = categoryService.getAllCategories().stream()
                .map(CategoryResponse::getName)
                .toList();

        return categoryNames.stream()
                .filter(category -> fileName.toLowerCase().contains(category.toLowerCase()))
                .findFirst()
                .orElse("undefined");
    }
}
