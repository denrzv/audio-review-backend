package io.github.denrzv.audioreview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationResponse {
    private Long fileId;
    private String filename;
    private String filePath;
    private String currentCategory;
    private LocalDateTime classifiedAt;
}