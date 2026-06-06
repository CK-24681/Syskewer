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
import com.syskewer.api.model.user.User;
import com.syskewer.api.service.user.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** @return lista de usuários (somente admin) */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UserResponseDto>> listAll() {
        return ResponseEntity.ok(userService.listAll());
    }

    /** @param userDto dados do novo usuário */
    @PostMapping
    public ResponseEntity<UserResponseDto> register(@RequestBody @Valid UserRecordDto userDto) {
        User userEntity = new User();
        userEntity.setName(userDto.name());
        userEntity.setUsername(userDto.username());
        userEntity.setEmail(userDto.email());
        userEntity.setPassword(userDto.password());
        userEntity.setRole(userDto.role());
        userEntity.setActive(true);

        UserResponseDto savedUser = userService.registerUser(userEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    /** @param id id do usuário */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserResponseDto> patch(@PathVariable Integer id, @RequestBody UserUpdateDto dto) {
        return ResponseEntity.ok(userService.patchUser(id, dto));
    }

    /** Soft delete — desativa o usuário. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}
