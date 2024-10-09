package io.github.denrzv.audioreview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationRequest {
    
    @NotBlank(message = "Category is required")
    private String category;
}