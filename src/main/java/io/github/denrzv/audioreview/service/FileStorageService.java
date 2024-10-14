package io.github.denrzv.audioreview.service;

import io.github.denrzv.audioreview.dto.CategoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;
    private final CategoryService categoryService;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir, CategoryService categoryService) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.categoryService = categoryService;

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            String category = extractCategoryFromFileName(originalFileName);
            String dateFolder = LocalDate.now().toString();
            Path categoryDir = fileStorageLocation.resolve(category).resolve(dateFolder).normalize();

            Files.createDirectories(categoryDir);

            Path targetLocation = categoryDir.resolve(originalFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Store the relative path for the database
            return fileStorageLocation.relativize(targetLocation).toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName, ex);
        }
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
