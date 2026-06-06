package com.syskewer.api.service.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.product.ProductRecordDto;
import com.syskewer.api.dto.product.ProductResponseDto;
import com.syskewer.api.dto.product.ProductUpdateDto;
import com.syskewer.api.model.product.Category;
import com.syskewer.api.model.product.PrepLocation;
import com.syskewer.api.model.product.Product;
import com.syskewer.api.repository.product.CategoryRepository;
import com.syskewer.api.repository.product.PrepLocationRepository;
import com.syskewer.api.repository.product.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final PrepLocationRepository prepLocationRepository;

    public ProductService(ProductRepository repository, CategoryRepository categoryRepository,
            PrepLocationRepository prepLocationRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.prepLocationRepository = prepLocationRepository;
    }

    /** @return cardápio completo */
    public List<ProductResponseDto> listAll() {
        return repository.findAll().stream()
                .map(ProductResponseDto::new)
                .toList();
    }

    /**
     * @param dto dados do produto
     * @return produto criado
     */
    public Product saveProduct(ProductRecordDto dto) {
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada!"));

        PrepLocation prepLocation = null;
        if (dto.prepLocationId() != null) {
            prepLocation = prepLocationRepository.findById(dto.prepLocationId())
                    .orElseThrow(() -> new RuntimeException("Local de preparo não encontrado!"));
        }

        Product product = new Product();
        product.setName(dto.name().trim().replaceAll(" +", " "));
        product.setPrice(dto.price());
        product.setCategory(category);
        product.setPrepLocation(prepLocation);
        product.setActive(true);
        product.setInStock(dto.inStock() != null ? dto.inStock() : Boolean.TRUE);

        if (repository.existsByNameIgnoreCase(product.getName())) {
            throw new RuntimeException("Já existe um produto com o nome: " + product.getName());
        }

        return repository.save(product);
    }

    /**
     * @param id id do produto
     * @param dto campos a atualizar
     * @return produto atualizado
     */
    public Product patchProduct(Integer id, ProductUpdateDto dto) {
        Product existingProduct = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));

        if (dto.name() != null) {
            String cleanedName = dto.name().trim().replaceAll(" +", " ");
            if (!cleanedName.equalsIgnoreCase(existingProduct.getName()) &&
                    repository.existsByNameIgnoreCase(cleanedName)) {
                throw new RuntimeException("Já existe um produto com o nome: " + cleanedName);
            }
            existingProduct.setName(cleanedName);
        }

        if (dto.price() != null) {
            existingProduct.setPrice(dto.price());
        }

        if (dto.categoryId() != null) {
            Category category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada!"));
            existingProduct.setCategory(category);
        }

        if (dto.prepLocationId() != null) {
            PrepLocation prepLocation = prepLocationRepository.findById(dto.prepLocationId())
                    .orElseThrow(() -> new RuntimeException("Local de preparo não encontrado!"));
            existingProduct.setPrepLocation(prepLocation);
        }

        if (dto.inStock() != null) {
            existingProduct.setInStock(dto.inStock());
        }

        return repository.save(existingProduct);
    }

    /** Soft delete — pedidos antigos mantêm referência ao produto. */
    public void deactivateProduct(Integer id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado!"));

        product.setActive(false);
        repository.save(product);
    }
}
