package com.syskewer.api;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syskewer.api.dto.product.ProductRecordDto;
import com.syskewer.api.model.product.Category;
import com.syskewer.api.model.product.Product;
import com.syskewer.api.repository.product.CategoryRepository;
import com.syskewer.api.repository.product.PrepLocationRepository;
import com.syskewer.api.repository.product.ProductRepository;
import com.syskewer.api.service.product.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PrepLocationRepository prepLocationRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRecordDto productDto;

    @BeforeEach
    public void setUp() {
        // Receita de um novo espetinho que o bar quer começar a vender hoje
        productDto = new ProductRecordDto(
            "Espetinho de Carne", 
            new BigDecimal("10.00"), 
            1, 
            null, 
            true
        );
    }

    @Test
    @DisplayName("Deve criar um produto com sucesso vinculando a categoria correta")
    void saveProduct_Success() {
        Category category = new Category();
        category.setId(1); 
        category.setName("Espetos");

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(productRepository.existsByNameIgnoreCase("Espetinho de Carne")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // Insere o espetinho oficialmente no cardápio do restaurante
        Product result = productService.saveProduct(productDto);

        assertNotNull(result);
        assertEquals("Espetinho de Carne", result.getName());
        assertEquals(new BigDecimal("10.00"), result.getPrice());
        
        verify(productRepository, times(1)).save(any(Product.class));
        verifyNoInteractions(prepLocationRepository); 
    }

    @Test
    @DisplayName("Deve falhar ao criar produto com categoria inexistente")
    void saveProduct_ThrowsException_WhenCategoryNotFound() {
        // Bloqueio de Cardápio: Impede o cadastro de um "produto órfão".
        // Se um item não tiver categoria, o garçom nunca vai conseguir achá-lo no tablet.
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.saveProduct(productDto);
        });

        assertEquals("Categoria não encontrada!", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }
}