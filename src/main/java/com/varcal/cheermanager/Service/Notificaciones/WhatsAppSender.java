package com.varcal.cheermanager.Service.Notificaciones;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Envio por la API en la nube de WhatsApp Business (Meta). Un mensaje iniciado por el negocio SOLO se puede
 * enviar con una PLANTILLA aprobada por Meta. La plantilla debe tener 3 variables de cuerpo:
 *   {{1}} nombre del deportista, {{2}} cantidad de facturas vencidas, {{3}} saldo total.
 * Sin app.whatsapp.token / phone-number-id el canal no esta disponible y el sistema ofrece enlaces wa.me
 * para enviar el mensaje a mano.
 */
@Component
public class WhatsAppSender {

    private final String token;
    private final String phoneNumberId;
    private final String plantilla;
    private final String idioma;
    private final String version;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WhatsAppSender(@Value("${app.whatsapp.token:}") String token,
                          @Value("${app.whatsapp.phone-number-id:}") String phoneNumberId,
                          @Value("${app.whatsapp.template:recordatorio_mora}") String plantilla,
                          @Value("${app.whatsapp.language:es}") String idioma,
                          @Value("${app.whatsapp.api-version:v21.0}") String version) {
        this.token = token;
        this.phoneNumberId = phoneNumberId;
        this.plantilla = plantilla;
        this.idioma = idioma;
        this.version = version;
    }

    public boolean disponible() {
        return !token.isBlank() && !phoneNumberId.isBlank();
    }

    /** telefono en formato internacional sin '+' (ej. 573001234567). Lanza excepcion si la API rechaza. */
    public void enviarPlantilla(String telefono, List<String> variables) throws Exception {
        List<Map<String, String>> parametros = new ArrayList<>();
        for (String v : variables) {
            parametros.add(Map.of("type", "text", "text", v));
        }
        Map<String, Object> cuerpo = Map.of(
                "messaging_product", "whatsapp",
                "to", telefono,
                "type", "template",
                "template", Map.of(
                        "name", plantilla,
                        "language", Map.of("code", idioma),
                        "components", List.of(Map.of("type", "body", "parameters", parametros))));

        HttpRequest req = HttpRequest.newBuilder(
                        URI.create("https://graph.facebook.com/" + version + "/" + phoneNumberId + "/messages"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(cuerpo)))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            String detalle = res.body() == null ? "" : res.body();
            throw new RuntimeException("WhatsApp respondio " + res.statusCode() + ": "
                    + (detalle.length() > 200 ? detalle.substring(0, 200) : detalle));
        }
    }
}
