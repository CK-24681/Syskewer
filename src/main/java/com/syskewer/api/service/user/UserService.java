package com.syskewer.api.service.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.syskewer.api.dto.user.UserResponseDto;
import com.syskewer.api.dto.user.UserUpdateDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.user.Role;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.user.RoleRepository;
import com.syskewer.api.repository.user.UserRepository;

// Servico para gerenciar as regras de negocio de usuarios
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Retorna a lista de todos os usuarios cadastrados
    public List<UserResponseDto> listAll() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // Cadastra um novo usuario validando se o username e email ja existem
    public UserResponseDto registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessRuleException("Este nome de usuário já está em uso!");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessRuleException("Este e-mail já está cadastrado!");
        }

        if (user.getRole() == null || user.getRole().getId() == null) {
            throw new BusinessRuleException("O perfil (Role) do usuário é obrigatório!");
        }
        Role role = roleRepository.findById(user.getRole().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado no sistema!"));
        user.setRole(role);

        String senhaCriptografada = passwordEncoder.encode(user.getPassword());
        user.setPassword(senhaCriptografada);

        User savedUser = userRepository.save(user);
        return convertToResponseDto(savedUser);
    }

    // Atualiza de forma parcial os dados de um usuario
    public UserResponseDto patchUser(Integer id, UserUpdateDto dto) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));

        if (dto.name() != null) {
            if (dto.name().trim().isEmpty()) {
                throw new BusinessRuleException("O nome do usuário não pode ser vazio.");
            }
            existingUser.setName(dto.name().trim());
        }
        if (dto.email() != null) {
            String trimmedEmail = dto.email().trim();
            if (trimmedEmail.isEmpty()) {
                throw new BusinessRuleException("O e-mail do usuário não pode ser vazio.");
            }
            if (!existingUser.getEmail().equalsIgnoreCase(trimmedEmail) && userRepository.existsByEmail(trimmedEmail)) {
                throw new BusinessRuleException("Este e-mail já está em uso!");
            }
            existingUser.setEmail(trimmedEmail);
        }
        if (dto.roleId() != null) {
            Role role = roleRepository.findById(dto.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado!"));
            existingUser.setRole(role);
        }

        User updatedUser = userRepository.save(existingUser);
        return convertToResponseDto(updatedUser);
    }

    // Desativa o usuario (soft delete)
    public void deactivateUser(Integer id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
        user.setActive(false);
        userRepository.save(user);
    }

    // Busca um usuario pelo e-mail
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
    }

    // Atualiza a senha do usuario no banco
    public void updatePassword(User user, String newEncodedPassword) {
        user.setPassword(newEncodedPassword);
        userRepository.save(user);
    }

    // Converte a entidade User para o DTO de resposta
    private UserResponseDto convertToResponseDto(User user) {
        return new UserResponseDto(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().getAuthority(),
            user.getActive()
        );
    }
}