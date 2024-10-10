package io.github.denrzv.audioreview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "classifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "audio_file_id")
    private AudioFile audioFile;

    @ManyToOne
    @JoinColumn(name = "previous_category_id")
    private Category previousCategory;

    @ManyToOne
    @JoinColumn(name = "new_category_id")
    private Category newCategory;

    @Column(nullable = false)
    private LocalDateTime classifiedAt;
}