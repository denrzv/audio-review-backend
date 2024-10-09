package io.github.denrzv.audioreview.controller;

import io.github.denrzv.audioreview.dto.AudioFileResponse;
import io.github.denrzv.audioreview.dto.ClassificationRequest;
import io.github.denrzv.audioreview.service.ClassificationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
}