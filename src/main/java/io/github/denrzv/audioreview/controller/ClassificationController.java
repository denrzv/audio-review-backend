package io.github.denrzv.audioreview.controller;

import io.github.denrzv.audioreview.dto.AudioFileResponse;
import io.github.denrzv.audioreview.dto.ClassificationRequest;
import io.github.denrzv.audioreview.dto.ClassificationResponse;
import io.github.denrzv.audioreview.service.ClassificationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/classification")
@AllArgsConstructor
public class ClassificationController {

    private ClassificationService classificationService;

    @GetMapping("/random")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AudioFileResponse> getRandomUnclassifiedFile() {
        return ResponseEntity.ok(classificationService.getRandomUnclassifiedFile());
    }

    @PostMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AudioFileResponse> classifyFile(@PathVariable Long fileId,
                                                          @RequestBody ClassificationRequest request) {
        return ResponseEntity.ok(classificationService.classifyFile(fileId, request));
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