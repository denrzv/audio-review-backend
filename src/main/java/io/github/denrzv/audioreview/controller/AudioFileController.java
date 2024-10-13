package io.github.denrzv.audioreview.controller;

import io.github.denrzv.audioreview.dto.AudioFileResponse;
import io.github.denrzv.audioreview.model.AudioFile;
import io.github.denrzv.audioreview.service.AudioFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/audio")
@AllArgsConstructor
public class AudioFileController {

    private final AudioFileService audioFileService;
    private final Path fileStorageLocation = Paths.get("uploads");
    private static final Logger logger = LoggerFactory.getLogger(AudioFileController.class);

    @GetMapping("/files/{filename:.+}")
    public void serveFile(@PathVariable String filename, HttpServletResponse response) {
        try {
            // Decode the filename to handle special characters
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);

            AudioFile audioFile = audioFileService.getFileByFilename(decodedFilename);
            if (audioFile == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Path filePath = fileStorageLocation.resolve(audioFile.getFilepath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String mimeType = Files.probeContentType(filePath);
            response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + decodedFilename + "\"");

            try (InputStream inputStream = resource.getInputStream();
                 OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                if (e.getMessage().contains("Broken pipe")) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
                throw e;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.error("Error serving file '{}': {}", filename, e.getMessage(), e);
        }
    }


    /**
     * Endpoint to upload audio files.
     * Only accessible by ADMIN role.
     *
     * @param file the audio file to be uploaded
     * @return ResponseEntity with metadata of the uploaded file
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AudioFileResponse> uploadFile(@RequestParam("file") MultipartFile file) {

        if (!file.getContentType().startsWith("audio/")) {
            throw new RuntimeException("Invalid file type. Only audio files are allowed.");
        }

        AudioFileResponse response = audioFileService.uploadFile(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve all uploaded audio files.
     * Only accessible by ADMIN role.
     *
     * @return List of all uploaded audio file responses
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String filename) {
        Map<String, Object> response = audioFileService.getAllFiles(page, pageSize, filename);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        audioFileService.deleteFileById(id);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AudioFileResponse> updateFile(@PathVariable Long id, @RequestBody AudioFileResponse updatedFile) {
        AudioFileResponse response = audioFileService.updateFileProperties(id, updatedFile);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllFiles() {
        audioFileService.deleteAllFiles();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/multiple")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMultipleFiles(@RequestBody Map<String, List<Long>> request) {
        List<Long> fileIds = request.get("ids");
        audioFileService.deleteMultipleFiles(fileIds);
        return ResponseEntity.ok("Files deleted successfully");
    }

    @PutMapping("/multiple")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMultipleFiles(@RequestBody Map<String, Object> request) {
        List<Long> fileIds = (List<Long>) request.get("ids");
        String currentCategory = (String) request.get("currentCategory");
        audioFileService.updateMultipleFiles(fileIds, currentCategory);
        return ResponseEntity.ok("Files updated successfully");
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = audioFileService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

}
