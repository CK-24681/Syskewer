package com.syskewer.api.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syskewer.api.model.product.PrepLocation;

@Repository
public interface PrepLocationRepository extends JpaRepository<PrepLocation, Integer> {
}
