package io.github.denrzv.audioreview.controller;

import io.github.denrzv.audioreview.dto.AudioFileResponse;
import io.github.denrzv.audioreview.dto.ClassificationRequest;
import io.github.denrzv.audioreview.dto.ClassificationResponse;
import io.github.denrzv.audioreview.model.User;
import io.github.denrzv.audioreview.repository.UserRepository;
import io.github.denrzv.audioreview.service.ClassificationService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ConcurrentModificationException;

@RestController
@RequestMapping("/classification")
@AllArgsConstructor
public class ClassificationController {

    private ClassificationService classificationService;
    private UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(ClassificationController.class);

    @GetMapping("/random")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AudioFileResponse> getRandomUnclassifiedFile() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            AudioFileResponse response = classificationService.getRandomUnclassifiedFile(user.getId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            logger.error("User retrieval failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalStateException ex) {
            logger.warn("No unclassified files available: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception ex) {
            logger.error("Unexpected error occurred while fetching unclassified file: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AudioFileResponse> classifyFile(@PathVariable Long fileId,
                                                          @RequestBody ClassificationRequest request) {
        try {
            return ResponseEntity.ok(classificationService.classifyFile(fileId, request));
        } catch (ConcurrentModificationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<ClassificationResponse>> getClassificationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Page<ClassificationResponse> history = classificationService.getClassificationHistoryForUser(username, page, pageSize);
        return ResponseEntity.ok(history);
    }
}