package io.github.denrzv.audioreview.controller;

import io.github.denrzv.audioreview.dto.AudioFileResponse;
import io.github.denrzv.audioreview.model.AudioFile;
import io.github.denrzv.audioreview.service.AudioFileService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final static Logger logger = LoggerFactory.getLogger(AudioFileController.class);

    /**
     * Serve an audio file by filename, accessible to both USER and ADMIN roles.
     */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            AudioFile audioFile = audioFileService.getFileByFilename(filename);
            if (audioFile == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = fileStorageLocation.resolve(audioFile.getFilepath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine MIME type
            String mimeType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mimeType != null ? mimeType : "application/octet-stream")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving file '{}': {}", filename, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .body(null);
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

    /**
     * Endpoint to update properties of an audio file.
     * Accessible only by ADMIN role.
     */
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
