package io.github.denrzv.audioreview.service;

import io.github.denrzv.audioreview.dto.CategoryRequest;
import io.github.denrzv.audioreview.dto.CategoryResponse;
import io.github.denrzv.audioreview.exception.ResourceAlreadyExistsException;
import io.github.denrzv.audioreview.exception.ResourceNotFoundException;
import io.github.denrzv.audioreview.model.Category;
import io.github.denrzv.audioreview.repository.AudioFileRepository;
import io.github.denrzv.audioreview.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    
    private CategoryRepository categoryRepository;
    private AudioFileRepository audioFileRepository;
    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);


    /**
     * Create a new category with unique name and shortcut.
     *
     * @param categoryRequest the category data
     * @return the created category response
     */
    @Override
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        // Check if category name or shortcut already exists
        if (categoryRepository.existsByName(categoryRequest.getName())) {
            throw new ResourceAlreadyExistsException("Category with name '" + categoryRequest.getName() + "' already exists.");
        }
        
        if (categoryRepository.existsByShortcut(categoryRequest.getShortcut())) {
            throw new ResourceAlreadyExistsException("Category with shortcut '" + categoryRequest.getShortcut() + "' already exists.");
        }
        
        // Create and save the new category
        Category category = Category.builder()
                .name(categoryRequest.getName())
                .shortcut(categoryRequest.getShortcut())
                .build();
        
        Category savedCategory = categoryRepository.save(category);
        
        return mapToResponse(savedCategory);
    }
    
    /**
     * Retrieve all categories.
     *
     * @return list of category responses
     */
    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    /**
     * Retrieve a category by its ID.
     *
     * @param id the category ID
     * @return the category response
     */
    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));
        return mapToResponse(category);
    }
    
    /**
     * Update an existing category.
     *
     * @param id              the category ID
     * @param categoryRequest the updated category data
     * @return the updated category response
     */
    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        logger.info("Creating category with name: {} and shortcut: {}", categoryRequest.getName(), categoryRequest.getShortcut());

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));
        
        // Check for unique name if changed
        if (!category.getName().equalsIgnoreCase(categoryRequest.getName()) &&
                categoryRepository.existsByName(categoryRequest.getName())) {
            throw new ResourceAlreadyExistsException("Category with name '" + categoryRequest.getName() + "' already exists.");
        }
        
        // Check for unique shortcut if changed
        if (!category.getShortcut().equalsIgnoreCase(categoryRequest.getShortcut()) &&
                categoryRepository.existsByShortcut(categoryRequest.getShortcut())) {
            throw new ResourceAlreadyExistsException("Category with shortcut '" + categoryRequest.getShortcut() + "' already exists.");
        }
        
        // Update category fields
        category.setName(categoryRequest.getName());
        category.setShortcut(categoryRequest.getShortcut());
        
        Category updatedCategory = categoryRepository.save(category);
        
        return mapToResponse(updatedCategory);
    }
    
    /**
     * Delete a category by its ID.
     *
     * @param id the category ID
     */
    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));

        // Check if category is referenced in audio_files
        boolean isReferencedInAudioFiles = audioFileRepository.existsByInitialCategoryId(id) ||
                audioFileRepository.existsByCurrentCategoryId(id);

        if (isReferencedInAudioFiles) {
            throw new ResourceAlreadyExistsException("Cannot delete category. It is associated with audio files.");
        }

        categoryRepository.delete(category);
    }
    
    /**
     * Map Category entity to CategoryResponse DTO.
     *
     * @param category the Category entity
     * @return CategoryResponse DTO
     */
    private CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getShortcut()
        );
    }
}