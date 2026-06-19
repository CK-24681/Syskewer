package com.syskewer.api.service.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.product.CategoryUpdateDto;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.product.Category;
import com.syskewer.api.repository.product.CategoryRepository;

@Service
public class CategoryService {
    private final CategoryRepository repository;
    
    public CategoryService(CategoryRepository repository) { this.repository = repository; }
    
    // --- ALTERAMOS O MÉTODO CREATE ---
    public Category create(String name, Integer parentId) {
        Category category = new Category();
        category.setName(name);

        // Se mandou um parentId, a gente vincula na hora de criar
        if (parentId != null) {
            Category parent = repository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria pai não encontrada com ID: " + parentId));
            category.setParent(parent);
        }

        return repository.save(category);
    }
    
    public List<Category> listAll() { return repository.findAll(); }

    public Category update(Integer id, CategoryUpdateDto dto) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));

        if (dto.name() != null && !dto.name().isBlank()) {
            category.setName(dto.name());
        }

        if (dto.parentId() != null) {
            Category parent = repository.findById(dto.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria pai não encontrada com ID: " + dto.parentId()));
            category.setParent(parent);
        }

        return repository.save(category);
    }
}