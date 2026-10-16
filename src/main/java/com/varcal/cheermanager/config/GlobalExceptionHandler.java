package com.varcal.cheermanager.config;

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

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Respuesta de error uniforme: { timestamp, status, error, message, path }.
 * (mismos campos que el JSON por defecto de Spring, para no romper el frontend).
 *
 * - Los RuntimeException "de negocio" que lanzan los services con un mensaje legible
 *   (p. ej. "El pago excede el saldo pendiente") se devuelven como 400 con ese mensaje.
 * - Errores de BD/infraestructura NUNCA exponen su mensaje: se registran en el log y se
 *   devuelve un texto generico.
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

    @ExceptionHandler({ NullPointerException.class, IllegalStateException.class,
            org.springframework.dao.DataAccessException.class })
    public ResponseEntity<Map<String, Object>> interno(Exception ex, HttpServletRequest req) {
        log.error("Error interno en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req);
    }

    /** Errores de negocio lanzados como RuntimeException("mensaje legible") por los services. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> negocio(RuntimeException ex, HttpServletRequest req) {
        String m = ex.getMessage();
        if (m == null || m.isBlank()) {
            log.error("RuntimeException sin mensaje en {}", req.getRequestURI(), ex);
            return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req);
        }
        return body(HttpStatus.BAD_REQUEST, m, req);
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
