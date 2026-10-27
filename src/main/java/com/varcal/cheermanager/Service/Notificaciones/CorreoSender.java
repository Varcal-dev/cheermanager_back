package com.varcal.cheermanager.Service.Notificaciones;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envio de correo por SMTP. Se configura SOLO con variables de entorno (Spring las toma solas):
 *   SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD,
 *   SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true, SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true, MAIL_FROM.
 * Sin SPRING_MAIL_HOST el canal no esta disponible.
 */
@Component
public class CorreoSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public CorreoSender(ObjectProvider<JavaMailSender> mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public boolean disponible() {
        return mailSender.getIfAvailable() != null;
    }

    /** Lanza excepcion si falla (el llamador la registra como FALLIDO). */
    public void enviar(String para, String asunto, String texto) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("SMTP no configurado");
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(para);
        msg.setSubject(asunto);
        msg.setText(texto);
        sender.send(msg);
    }
}
