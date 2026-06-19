package com.syskewer.api.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.product.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
