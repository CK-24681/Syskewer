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

import com.syskewer.api.dto.product.MenuRecordDto;
import com.syskewer.api.model.product.Category;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.repository.product.CategoryRepository;
import com.syskewer.api.repository.product.PrepLocationRepository;
import com.syskewer.api.repository.product.MenuRepository;
import com.syskewer.api.repository.product.ProductRepository;
import com.syskewer.api.service.product.MenuService;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PrepLocationRepository prepLocationRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private MenuService menuService;

    private MenuRecordDto menuDto;

    @BeforeEach
    public void setUp() {
        menuDto = new MenuRecordDto(
            "Espetinho de Carne", 
            new BigDecimal("10.00"), 
            1, 
            null, 
            null
        );
    }

    @Test
    @DisplayName("Deve criar um item do cardápio com sucesso vinculando a categoria correta")
    void saveProduct_Success() {
        Category category = new Category();
        category.setId(1); 
        category.setName("Espetos");

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(menuRepository.existsByNameIgnoreCase("Espetinho de Carne")).thenReturn(false);
        when(menuRepository.save(any(Menu.class))).thenAnswer(i -> i.getArguments()[0]);

        Menu result = menuService.saveMenu(menuDto);

        assertNotNull(result);
        assertEquals("Espetinho de Carne", result.getName());
        assertEquals(new BigDecimal("10.00"), result.getPrice());
        
        verify(menuRepository, times(1)).save(any(Menu.class));
        verifyNoInteractions(prepLocationRepository); 
    }

    @Test
    @DisplayName("Deve falhar ao criar item com categoria inexistente")
    void saveProduct_ThrowsException_WhenCategoryNotFound() {
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            menuService.saveMenu(menuDto);
        });

        assertEquals("Categoria não encontrada!", exception.getMessage());
        verify(menuRepository, never()).save(any(Menu.class));
    }
}
