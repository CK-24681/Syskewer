package com.syskewer.api;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syskewer.api.dto.user.UserResponseDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.model.user.Role;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.user.RoleRepository;
import com.syskewer.api.repository.user.UserRepository;
import com.syskewer.api.service.user.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso")
    void registerUser_Success() {
        Role role = new Role();
        role.setId(2);
        role.setAuthority("ROLE_GARCOM"); 

        User user = new User();
        user.setName("João Garçom");
        user.setUsername("joao");
        user.setEmail("joao@bar.com");
        user.setPassword("senha123");
        user.setRole(role);

        when(roleRepository.findById(2)).thenReturn(Optional.of(role));
        
        when(userRepository.findByEmail("joao@bar.com")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("joao@bar.com")).thenReturn(false);
        when(userRepository.findByUsername("joao")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("joao")).thenReturn(false);
        
        when(passwordEncoder.encode("senha123")).thenReturn("hash123");
        
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = (User) i.getArguments()[0];
            u.setId(10); 
            return u;
        });

        UserResponseDto response = userService.registerUser(user);

        assertEquals("João Garçom", response.name());
        assertEquals("joao", response.username());
    }

    @Test
    @DisplayName("Deve bloquear o cadastro se o email já estiver em uso")
    void registerUser_ThrowsException_WhenEmailExists() {
        Role role = new Role();
        role.setId(2);

        User user = new User();
        user.setName("João Meliante");
        user.setEmail("joao@bar.com");
        user.setUsername("joao_fake");
        user.setPassword("senha123"); 
        user.setRole(role); 
        
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));
        
        when(userRepository.findByEmail("joao@bar.com")).thenReturn(Optional.of(new User()));
        when(userRepository.existsByEmail("joao@bar.com")).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> userService.registerUser(user));
        
        // A MÁGICA ACONTECE AQUI: Alinhamos o texto do teste com o texto real do sistema!
        assertEquals("Este e-mail já está cadastrado!", exception.getMessage());
    }
}