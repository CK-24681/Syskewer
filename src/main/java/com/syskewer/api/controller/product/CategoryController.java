package com.syskewer.api.controller.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable; // <-- Import novo
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.dto.product.CategoryRequestDto;
import com.syskewer.api.dto.product.CategoryUpdateDto;
import com.syskewer.api.model.product.Category;
import com.syskewer.api.service.product.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
@Tag(name = "Categorias", description = "Gestão de categorias de produtos")
public class CategoryController {
    private final CategoryService service;
    
    public CategoryController(CategoryService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Criar nova categoria")
    public ResponseEntity<Category> create(@RequestBody @Valid CategoryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto.name(), dto.parentId()));
    }

    @GetMapping
    @Operation(summary = "Listar todas as categorias")
    public ResponseEntity<List<Category>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar categoria existente")
    public ResponseEntity<Category> update(@PathVariable Integer id, @RequestBody CategoryUpdateDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
}