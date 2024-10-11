package io.github.denrzv.audioreview.repository;

import io.github.denrzv.audioreview.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Find a category by its name.
     *
     * @param name the category name
     * @return Optional containing the Category if found
     */
    Optional<Category> findByNameEqualsIgnoreCase(String name);

    boolean existsByName(String name);
    
    /**
     * Check if a category with the given shortcut exists.
     *
     * @param shortcut the category shortcut
     * @return true if exists, false otherwise
     */
    boolean existsByShortcut(String shortcut);

    Optional<Category> findByName(String currentCategory);
}