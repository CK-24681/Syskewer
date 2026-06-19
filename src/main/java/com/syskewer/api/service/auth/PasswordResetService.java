package com.syskewer.api.service.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syskewer.api.model.auth.PasswordResetToken;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.auth.PasswordResetTokenRepository;
import com.syskewer.api.service.user.UserService;
import com.syskewer.api.exception.ResourceNotFoundException;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final long TOKEN_EXPIRY_MINUTES = 15;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserService userService,
            EmailService emailService, PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // Cria o token de recuperacao de senha e envia por e-mail
    public void generateResetToken(String email) {
        try {
            User user = userService.findByEmail(email);

            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

            String hashedToken = hashToken(token);
            PasswordResetToken resetToken = new PasswordResetToken(hashedToken, user, expiryDate);
            tokenRepository.save(resetToken);

            String resetLink = "http://localhost:3000/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        } catch (ResourceNotFoundException e) {
            // Abafa para evitar enumeração de usuários
        }
    }

    // Valida o token e altera a senha do usuario
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String hashedToken = hashToken(token);
        PasswordResetToken resetToken = tokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new RuntimeException("Token de recuperação inválido!"));

        if (resetToken.isExpired()) {
            tokenRepository.deleteByToken(hashedToken);
            throw new RuntimeException("Token de recuperação expirou!");
        }

        User user = resetToken.getUser();
        String encodedPassword = passwordEncoder.encode(newPassword);
        userService.updatePassword(user, encodedPassword);

        emailService.sendPasswordResetConfirmation(user.getEmail());

        tokenRepository.deleteByToken(hashedToken);
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash do token", e);
        }
    }
}
