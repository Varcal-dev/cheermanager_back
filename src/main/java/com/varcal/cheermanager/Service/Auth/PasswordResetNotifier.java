package com.varcal.cheermanager.Service.Auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Entrega el enlace de recuperacion de contrasena.
 *  - Con SMTP configurado (spring.mail.host): envia un correo.
 *  - Sin SMTP: escribe el enlace en el log del SERVIDOR (solo para desarrollo).
 * Nunca se devuelve el token en la respuesta HTTP.
 */
@Component
public class PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotifier.class);

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.frontend.reset-password-url}")
    private String resetUrl;

    @Value("${app.mail.from}")
    private String from;

    public PasswordResetNotifier(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void enviar(String email, String username, String token) {
        String link = resetUrl + "?token=" + token;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("[SIN SMTP] Recuperacion de contrasena para {}: {}", email, link);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(email);
            msg.setSubject("Recuperacion de contrasena - CheerManager");
            msg.setText("Hola " + username + ",\n\nRecibimos una solicitud para restablecer tu contrasena.\n"
                    + "Abre este enlace (vence pronto): " + link
                    + "\n\nSi no fuiste tu, ignora este mensaje.");
            sender.send(msg);
        } catch (Exception e) {
            // No se propaga: la respuesta al usuario debe ser identica exista o no el correo.
            log.error("No se pudo enviar el correo de recuperacion a {}", email, e);
        }
    }
}
