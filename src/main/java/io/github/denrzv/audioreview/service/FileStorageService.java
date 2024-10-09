package io.github.denrzv.audioreview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

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


    private String extractCategoryFromFileName(String fileName) {
        Pattern pattern = Pattern.compile("^(voice|silent|answering_machine)_.*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fileName.toLowerCase());
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return "undefined";
    }
}
