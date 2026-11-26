package com.varcal.cheermanager.config;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Respuesta de error uniforme: { timestamp, status, error, message, path }.
 * (mismos campos que el JSON por defecto de Spring, para no romper el frontend).
 *
 * - Los errores "de negocio" que lanzan los services con un mensaje legible
 *   (p. ej. "El pago excede el saldo pendiente") se devuelven con ese mensaje, pero solo si lo
 *   escribimos nosotros (ver {@link ErrorPublico}): 400 para RuntimeException/IllegalArgumentException,
 *   409 para IllegalStateException, y el estado propio para ResponseStatusException.
 * - Errores de BD, de libreria o de infraestructura NUNCA exponen su mensaje: se registran en el
 *   log y se devuelve un texto generico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("Datos invalidos");
        return body(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Map<String, Object>> peticionMalFormada(Exception ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "Peticion mal formada", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> prohibido(AccessDeniedException ex, HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta accion", req);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(EntityNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, "Recurso no encontrado", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violacion de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT,
                "La operacion viola una restriccion de datos (duplicado o registro en uso)", req);
    }

    @ExceptionHandler({ OptimisticLockingFailureException.class, PessimisticLockingFailureException.class })
    public ResponseEntity<Map<String, Object>> concurrencia(Exception ex, HttpServletRequest req) {
        log.warn("Conflicto de concurrencia en {}", req.getRequestURI());
        return body(HttpStatus.CONFLICT, "El registro fue modificado por otra operacion; intenta de nuevo", req);
    }

    @ExceptionHandler({ NullPointerException.class, org.springframework.dao.DataAccessException.class })
    public ResponseEntity<Map<String, Object>> interno(Exception ex, HttpServletRequest req) {
        log.error("Error interno en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorPublico.GENERICO, req);
    }

    /**
     * ResponseStatusException (RolService, JerarquiaAccesoService...): respeta el estado que eligio el servicio
     * (403, 409...) y muestra solo su motivo. Sin este manejador la atrapaba {@link #negocio} y salia como 400
     * con el texto {@code 409 CONFLICT "motivo"}. Un 5xx no muestra el motivo.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> conEstado(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null || status.is5xxServerError()) {
            log.error("Error interno ({}) en {} {}", ex.getStatusCode(), req.getMethod(), req.getRequestURI(), ex);
            return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorPublico.GENERICO, req);
        }
        String motivo = ex.getReason();
        return body(status, motivo == null || motivo.isBlank() ? status.getReasonPhrase() : motivo, req);
    }

    /**
     * IllegalStateException: los services la usan para "no se puede hacer eso ahora" (sede duplicada, roster
     * confirmado...), asi que si el mensaje es nuestro sale como 409 con ese texto, igual que ya hacen los
     * controladores que la capturan. Las de infraestructura o de libreria siguen siendo 500 genericos.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> estadoIlegal(IllegalStateException ex, HttpServletRequest req) {
        if (ErrorPublico.esPublico(ex)) {
            return body(HttpStatus.CONFLICT, ex.getMessage(), req);
        }
        log.error("Error interno en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorPublico.GENERICO, req);
    }

    /**
     * Errores de negocio lanzados como RuntimeException("mensaje legible") por los services. Solo se muestra el
     * mensaje si lo escribimos nosotros ({@link ErrorPublico}). Un argumento invalido o una fecha ilegible que
     * vienen de una libreria son del cliente: 400 con texto generico. Cualquier otra cosa es un fallo nuestro:
     * 500 generico y el detalle al log.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> negocio(RuntimeException ex, HttpServletRequest req) {
        if (ErrorPublico.esPublico(ex)) {
            return body(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
        }
        if (ex instanceof IllegalArgumentException || ex instanceof DateTimeException) {
            log.warn("Valor invalido en {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getClass().getSimpleName());
            return body(HttpStatus.BAD_REQUEST, "Peticion invalida", req);
        }
        log.error("Error interno en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorPublico.GENERICO, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generico(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", LocalDateTime.now().toString());
        m.put("status", status.value());
        m.put("error", status.getReasonPhrase());
        m.put("message", message);
        m.put("path", req.getRequestURI());
        m.put("success", false);
        return ResponseEntity.status(status).body(m);
    }
}