package com.syskewer.api.service.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syskewer.api.dto.product.MenuRecordDto;
import com.syskewer.api.dto.product.MenuResponseDto;
import com.syskewer.api.dto.product.MenuUpdateDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.product.Category;
import com.syskewer.api.model.product.PrepLocation;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.model.product.Product;
import com.syskewer.api.repository.product.CategoryRepository;
import com.syskewer.api.repository.product.PrepLocationRepository;
import com.syskewer.api.repository.product.MenuRepository;
import com.syskewer.api.repository.product.ProductRepository;

@Service
public class MenuService {

    private final MenuRepository repository;
    private final CategoryRepository categoryRepository;
    private final PrepLocationRepository prepLocationRepository;
    private final ProductRepository productRepository;

    public MenuService(MenuRepository repository, CategoryRepository categoryRepository,
            PrepLocationRepository prepLocationRepository, ProductRepository productRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.prepLocationRepository = prepLocationRepository;
        this.productRepository = productRepository;
    }

    // Retorna o cardapio completo
    public List<MenuResponseDto> listAll() {
        return repository.findAll().stream()
                .map(MenuResponseDto::new)
                .toList();
    }

    // Salva um novo item no cardapio
    public Menu saveMenu(MenuRecordDto dto) {
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada!"));

        PrepLocation prepLocation = null;
        if (dto.prepLocationId() != null) {
            prepLocation = prepLocationRepository.findById(dto.prepLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Local de preparo não encontrado!"));
        }

        Menu menu = new Menu();
        menu.setName(dto.name().trim().replaceAll(" +", " "));
        menu.setPrice(dto.price());
        menu.setCategory(category);
        menu.setPrepLocation(prepLocation);
        menu.setActive(true);

        if (dto.productIds() != null && !dto.productIds().isEmpty()) {
            List<Product> products = productRepository.findAllById(dto.productIds());
            menu.setProducts(products);
        }

        if (repository.existsByNameIgnoreCase(menu.getName())) {
            throw new BusinessRuleException("Já existe um item no cardápio com o nome: " + menu.getName());
        }

        return repository.save(menu);
    }

    // Atualiza os dados de um item do cardapio existente
    public Menu patchMenu(Integer id, MenuUpdateDto dto) {
        Menu existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de cardápio não encontrado!"));

        if (dto.name() != null) {
            String cleanedName = dto.name().trim().replaceAll(" +", " ");
            if (cleanedName.isEmpty()) {
                throw new BusinessRuleException("O nome do item não pode ser vazio.");
            }
            if (!cleanedName.equalsIgnoreCase(existing.getName()) &&
                    repository.existsByNameIgnoreCase(cleanedName)) {
                throw new BusinessRuleException("Já existe um item no cardápio com o nome: " + cleanedName);
            }
            existing.setName(cleanedName);
        }

        if (dto.price() != null) {
            if (dto.price().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("O preço do item deve ser maior que zero.");
            }
            existing.setPrice(dto.price());
        }

        if (dto.categoryId() != null) {
            Category category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada!"));
            existing.setCategory(category);
        }

        if (dto.prepLocationId() != null) {
            PrepLocation prepLocation = prepLocationRepository.findById(dto.prepLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Local de preparo não encontrado!"));
            existing.setPrepLocation(prepLocation);
        }

        if (dto.productIds() != null) {
            List<Product> products = productRepository.findAllById(dto.productIds());
            existing.setProducts(products);
        }

        return repository.save(existing);
    }

    // Desativa o item do cardapio para nao aparecer no menu (soft delete)
    public void deactivateMenu(Integer id) {
        Menu menu = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de cardápio não encontrado!"));

        menu.setActive(false);
        repository.save(menu);
    }
}
