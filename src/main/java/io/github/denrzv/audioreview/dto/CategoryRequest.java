package io.github.denrzv.audioreview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {
    
    @NotBlank(message = "Category name is mandatory")
    @Size(max = 50, message = "Category name must not exceed 50 characters")
    private String name;
    
    @NotBlank(message = "Shortcut is mandatory")
    @Pattern(regexp = "^[A-Za-z]$", message = "Shortcut must be a single alphabetic character")
    private String shortcut;
}