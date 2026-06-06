package com.syskewer.api.controller.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.dto.product.ProductRecordDto;
import com.syskewer.api.dto.product.ProductResponseDto;
import com.syskewer.api.dto.product.ProductUpdateDto;
import com.syskewer.api.model.product.Product;
import com.syskewer.api.service.product.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** @return cardápio */
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    /** @param dto novo produto */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductResponseDto> create(@RequestBody @Valid ProductRecordDto dto) {
        Product savedProduct = service.saveProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProductResponseDto(savedProduct));
    }

    /** @param id id do produto */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductResponseDto> patch(@PathVariable Integer id, @RequestBody ProductUpdateDto dto) {
        Product updatedProduct = service.patchProduct(id, dto);
        return ResponseEntity.ok(new ProductResponseDto(updatedProduct));
    }

    /** @param id id do produto */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }
}
