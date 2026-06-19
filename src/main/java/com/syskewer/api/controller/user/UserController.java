package com.syskewer.api.controller.user;

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

import com.syskewer.api.dto.user.UserRecordDto;
import com.syskewer.api.dto.user.UserResponseDto;
import com.syskewer.api.dto.user.UserUpdateDto;
import com.syskewer.api.model.user.Role;
import com.syskewer.api.model.user.User;
import com.syskewer.api.service.user.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Usuários", description = "Gestão de usuários e permissões")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Lista todos os usuarios cadastrados
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UserResponseDto>> listAll() {
        return ResponseEntity.ok(userService.listAll());
    }

    // Cadastra um novo usuario no sistema
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponseDto> register(@RequestBody @Valid UserRecordDto userDto) {
        User userEntity = new User();
        userEntity.setName(userDto.name());
        userEntity.setUsername(userDto.username());
        userEntity.setEmail(userDto.email());
        userEntity.setPassword(userDto.password());
        
        // Associa o perfil pelo ID que veio da requisicao
        Role role = new Role();
        role.setId(userDto.roleId());
        userEntity.setRole(role);
        
        userEntity.setActive(true);

        UserResponseDto savedUser = userService.registerUser(userEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // Atualiza os dados de um usuario existente
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponseDto> patch(@PathVariable Integer id, @RequestBody @Valid UserUpdateDto dto) {
        return ResponseEntity.ok(userService.patchUser(id, dto));
    }

    // Desativa o usuario no sistema (soft delete)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}
