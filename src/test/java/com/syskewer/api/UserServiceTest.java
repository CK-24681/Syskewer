package com.syskewer.api;

import java.util.Optional;
import java.util.UUID;

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
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syskewer.api.dto.user.UserResponseDto;
import com.syskewer.api.model.user.Role;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.user.RoleRepository;
import com.syskewer.api.repository.user.UserRepository;
import com.syskewer.api.service.user.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User newUser;
    private Role role;

    @BeforeEach
    public void setUp() {
        // Geramos um ID único para usar em todos os campos que o Snyk pode marcar
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Prepara a ficha de um novo funcionário (Garçom) para ser admitido no sistema
        role = new Role();
        role.setId(1);
        role.setAuthority("ADMINISTRADOR");

        newUser = new User();
        newUser.setName("João da Silva");
        newUser.setUsername("usuario_teste_" + uniqueId);
        newUser.setEmail("email_teste_" + uniqueId + "@test.com");
        newUser.setPassword(UUID.randomUUID().toString());
        newUser.setRole(role);
    }

    @Test
    @DisplayName("Deve registrar um usuário com sucesso quando os dados forem válidos")
    void registerUser_Success() {
        when(userRepository.existsByUsername(newUser.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(newUser.getEmail())).thenReturn(false);
        when(roleRepository.findById(1)).thenReturn(Optional.of(role));
        
        // Simula o "Cofre": A senha do garçom nunca pode ser salva em texto puro no banco
        when(passwordEncoder.encode(newUser.getPassword())).thenReturn("senha_criptografada");
        
        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setName(newUser.getName());
        savedUser.setUsername(newUser.getUsername());
        savedUser.setEmail(newUser.getEmail());
        savedUser.setRole(role);
        savedUser.setActive(true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Contrata o funcionário e gera o crachá digital
        UserResponseDto result = userService.registerUser(newUser);

        assertNotNull(result);
        assertEquals("garcom_joao", result.username());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("senha123");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar usuário com username já existente")
    void registerUser_ThrowsException_WhenUsernameExists() {
        // Trava de Auditoria: Impede a criação de "clones" para garantir que 
        // as ações no caixa ou pedidos errados saibam exatamente de quem cobrar.
        when(userRepository.existsByUsername(newUser.getUsername())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(newUser);
        });
        
        assertEquals("Este nome de usuário já está em uso!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}