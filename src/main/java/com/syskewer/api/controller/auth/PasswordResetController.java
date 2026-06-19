package com.syskewer.api.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.dto.auth.ForgotPasswordRequest;
import com.syskewer.api.dto.auth.ForgotPasswordResponse;
import com.syskewer.api.dto.auth.ResetPasswordRequest;
import com.syskewer.api.dto.auth.ResetPasswordResponse;
import com.syskewer.api.service.auth.PasswordResetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // Envia o link de recuperacao de senha por e-mail
    @PostMapping("/forgot")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        try {
            passwordResetService.generateResetToken(request.email());
            return ResponseEntity.ok(new ForgotPasswordResponse("E-mail de recuperação enviado com sucesso!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ForgotPasswordResponse("Erro: " + e.getMessage()));
        }
    }

    // Redefine a senha com base no token recebido
    @PostMapping("/reset")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.token(), request.newPassword());
            return ResponseEntity.ok(new ResetPasswordResponse("Palavra-passe redefinida com sucesso!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ResetPasswordResponse("Erro: " + e.getMessage()));
        }
    }
}
