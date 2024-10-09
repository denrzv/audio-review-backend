package io.github.denrzv.audioreview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audio_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false, unique = true)
    private String filepath;

    @ManyToOne
    @JoinColumn(name = "initial_category_id", nullable = false)
    private Category initialCategory;

    @ManyToOne
    @JoinColumn(name = "current_category_id")
    private Category currentCategory;

    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;
}