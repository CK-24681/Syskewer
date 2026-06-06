package com.syskewer.api.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syskewer.api.model.product.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByNameIgnoreCase(String name);
}