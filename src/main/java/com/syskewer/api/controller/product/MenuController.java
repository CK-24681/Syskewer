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

import com.syskewer.api.dto.product.MenuRecordDto;
import com.syskewer.api.dto.product.MenuResponseDto;
import com.syskewer.api.dto.product.MenuUpdateDto;
import com.syskewer.api.model.product.Menu;
import com.syskewer.api.service.product.MenuService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cardapio")
public class MenuController {

    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    // Retorna o cardapio com todos os itens
    @GetMapping
    public ResponseEntity<List<MenuResponseDto>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    // Cadastra um novo item no cardapio
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MenuResponseDto> create(@RequestBody @Valid MenuRecordDto dto) {
        Menu savedMenu = service.saveMenu(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MenuResponseDto(savedMenu));
    }

    // Patcheia um item de cardapio existente
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MenuResponseDto> patch(@PathVariable Integer id, @RequestBody @Valid MenuUpdateDto dto) {
        Menu updatedMenu = service.patchMenu(id, dto);
        return ResponseEntity.ok(new MenuResponseDto(updatedMenu));
    }

    // Desativa um item do cardapio (soft delete)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deactivateMenu(id);
        return ResponseEntity.noContent().build();
    }
}
