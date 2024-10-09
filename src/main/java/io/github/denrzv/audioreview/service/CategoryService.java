package io.github.denrzv.audioreview.service;

import io.github.denrzv.audioreview.dto.CategoryRequest;
import io.github.denrzv.audioreview.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    
    /**
     * Create a new category.
     *
     * @param categoryRequest the category data
     * @return the created category response
     */
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    
    /**
     * Retrieve all categories.
     *
     * @return list of category responses
     */
    List<CategoryResponse> getAllCategories();
    
    /**
     * Retrieve a category by its ID.
     *
     * @param id the category ID
     * @return the category response
     */
    CategoryResponse getCategoryById(Long id);
    
    /**
     * Update an existing category.
     *
     * @param id              the category ID
     * @param categoryRequest the updated category data
     * @return the updated category response
     */
    CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest);
    
    /**
     * Delete a category by its ID.
     *
     * @param id the category ID
     */
    void deleteCategory(Long id);
}