package io.github.denrzv.audioreview.repository;

import io.github.denrzv.audioreview.model.Classification;
import io.github.denrzv.audioreview.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassificationRepository extends JpaRepository<Classification, Long> {
    Page<Classification> findByUserOrderByClassifiedAtDesc(User user, Pageable pageable);
}