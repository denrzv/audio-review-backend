package io.github.denrzv.audioreview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioFileResponse {
    private Long id;
    private String filename;
    private String initialCategory;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
    private String currentCategory;
    private String filePath;
}