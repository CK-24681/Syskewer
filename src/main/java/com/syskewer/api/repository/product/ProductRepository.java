package com.syskewer.api.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.product.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByNameIgnoreCase(String name);
}