package com.syskewer.api.service.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.syskewer.api.model.auth.PasswordResetToken;
import com.syskewer.api.model.user.User;
import com.syskewer.api.repository.auth.PasswordResetTokenRepository;
import com.syskewer.api.service.user.UserService;

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

    /**
     * @param email e-mail cadastrado no sistema
     */
    public void generateResetToken(String email) {
        User user = userService.findByEmail(email);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        tokenRepository.save(resetToken);

        String resetLink = "http://localhost:3000/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    /**
     * @param token token recebido por e-mail
     * @param newPassword nova senha em texto plano
     */
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token de recuperação inválido!"));

        if (resetToken.isExpired()) {
            tokenRepository.deleteByToken(token);
            throw new RuntimeException("Token de recuperação expirou!");
        }

        User user = resetToken.getUser();
        String encodedPassword = passwordEncoder.encode(newPassword);
        userService.updatePassword(user, encodedPassword);

        emailService.sendPasswordResetConfirmation(user.getEmail());

        tokenRepository.deleteByToken(token);
    }
}
