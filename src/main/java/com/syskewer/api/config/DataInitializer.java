package com.syskewer.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.syskewer.api.model.user.Role;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.user.RoleRepository;
import com.syskewer.api.repository.user.UserRepository;

/** Cria o admin padrão na primeira subida, se ainda não existir. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${api.security.admin.default-password:admin123}")
    private String defaultAdminPassword;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        Role adminRole = roleRepository.findByAuthority("Administrador")
                .orElseThrow(() -> new RuntimeException("Role Administrador não encontrada no banco. Verifique o Flyway."));

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setName("Administrador Padrão");
            admin.setUsername("admin");
            admin.setEmail("admin@syskewer.com");
            admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
            admin.setRole(adminRole);
            admin.setActive(true);

            userRepository.save(admin);
        }
    }
}
