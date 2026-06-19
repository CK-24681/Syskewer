package com.syskewer.api.service.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.product.CategoryUpdateDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.product.Category;
import com.syskewer.api.repository.product.CategoryRepository;

// Servico para gerenciar as categorias de produtos no cardapio
@Service
public class CategoryService {
    private final CategoryRepository repository;
    
    public CategoryService(CategoryRepository repository) { this.repository = repository; }
    
    // Cadastra uma nova categoria permitindo vincular a uma categoria pai
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
    
    // Lista todas as categorias cadastradas
    public List<Category> listAll() { return repository.findAll(); }

    // Atualiza o nome ou a categoria pai de uma categoria existente
    public Category update(Integer id, CategoryUpdateDto dto) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));

        if (dto.name() != null) {
            if (dto.name().trim().isEmpty()) {
                throw new BusinessRuleException("O nome da categoria não pode ser vazio.");
            }
            category.setName(dto.name().trim());
        }

        if (dto.parentId() != null) {
            if (dto.parentId().equals(id)) {
                throw new BusinessRuleException("Uma categoria não pode ser sua própria categoria pai.");
            }
            Category parent = repository.findById(dto.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria pai não encontrada com ID: " + dto.parentId()));
            category.setParent(parent);
        }

        return repository.save(category);
    }
}