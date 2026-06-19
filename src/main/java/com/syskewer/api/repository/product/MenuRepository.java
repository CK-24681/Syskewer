package com.syskewer.api.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syskewer.api.model.product.Menu;

public interface MenuRepository extends JpaRepository<Menu, Integer> {
    boolean existsByNameIgnoreCase(String name);
}
