package com.syskewer.api.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * @param to e-mail do destinatário
     * @param resetLink URL com token de 15 minutos
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Recuperação de Senha - Syskewer");
            message.setText("""
                    Olá,

                    Você solicitou a recuperação de sua senha. \
                    Clique no link abaixo para redefinir sua senha:

                    %s

                    Este link é válido por 15 minutos.

                    Se você não solicitou a recuperação de senha, ignore este e-mail.

                    Atenciosamente,
                    Equipe Syskewer""".formatted(resetLink));

            mailSender.send(message);
        } catch (MailException e) {
            throw new RuntimeException("Erro ao enviar e-mail de recuperação de senha", e);
        }
    }

    /** @param to e-mail do usuário que redefiniu a senha */
    public void sendPasswordResetConfirmation(String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Senha Redefinida - Syskewer");
            message.setText("""
                    Olá,

                    Sua senha foi redefinida com sucesso.

                    Se você não realizou esta ação, entre em contato com o suporte imediatamente.

                    Atenciosamente,
                    Equipe Syskewer""");

            mailSender.send(message);
        } catch (MailException e) {
            throw new RuntimeException("Erro ao enviar confirmação de redefinição de senha", e);
        }
    }
}
