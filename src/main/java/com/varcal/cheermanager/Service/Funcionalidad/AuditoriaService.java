package com.varcal.cheermanager.Service.Funcionalidad;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.varcal.cheermanager.DTO.Funcionalidad.AuditoriaDTO;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Funcionalidad.Auditoria;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Funcionalidad.AuditoriaRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private UserRepository userRepository;

    // --- Lado de escritura -------------------------------------------------
    // Lo llama AuditoriaAspect en cada INSERT/UPDATE/DELETE detectado sobre
    // los repositorios del proyecto. CRÍTICO: nunca debe propagar una
    // excepción — un fallo al auditar (ej. no hay usuario autenticado en el
    // contexto, o un problema de serialización) jamás puede tumbar la
    // operación de negocio real que la disparó. Por eso todo el cuerpo está
    // envuelto en try/catch.
    public void registrar(String tablaAfectada, Auditoria.AccionEnum accion, Object registroId,
            String valoresAnteriores, String nuevosValores) {
        try {
            Usuario usuario = obtenerUsuarioActual();
            // Sin usuario autenticado (ej. un job interno, o una llamada que
            // se coló sin JWT) no hay a quién atribuir el cambio, y
            // usuario_id es NOT NULL en la tabla — mejor no auditar esa
            // escritura puntual que reventar el insert de auditoría.
            if (usuario == null) {
                return;
            }

            Auditoria a = new Auditoria();
            a.setUsuario(usuario);
            a.setTablaAfectada(tablaAfectada);
            a.setAccion(accion);
            a.setRegistroId(convertirAEntero(registroId));
            a.setValoresAnteriores(valoresAnteriores);
            a.setNuevosValores(nuevosValores);
            a.setIpUsuario(obtenerIpActual());
            a.setFecha(LocalDateTime.now());
            auditoriaRepository.save(a);
        } catch (Exception e) {
            // best-effort — silencioso a propósito, ver comentario arriba.
        }
    }

    private Integer convertirAEntero(Object valor) {
        if (valor instanceof Integer i) return i;
        if (valor instanceof Number n) return n.intValue();
        if (valor == null) return null;
        try {
            return Integer.valueOf(valor.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Usuario obtenerUsuarioActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object detailsObj = auth.getDetails();
            if (!(detailsObj instanceof Map<?, ?> details)) return null;
            Object userIdObj = details.get("userId");
            if (!(userIdObj instanceof Number)) return null;
            Long userId = ((Number) userIdObj).longValue();
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String obtenerIpActual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    // --- Lado de lectura -----------------------------------------------

    public Page<AuditoriaDTO> listar(String tablaAfectada, String accion, Integer usuarioId,
            LocalDateTime desde, LocalDateTime hasta, String q, Pageable pageable) {

        Specification<Auditoria> spec = Specification.where(null);

        if (tablaAfectada != null && !tablaAfectada.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tablaAfectada"), tablaAfectada));
        }
        if (accion != null && !accion.isBlank()) {
            Auditoria.AccionEnum accionEnum;
            try {
                accionEnum = Auditoria.AccionEnum.valueOf(accion.toUpperCase());
            } catch (IllegalArgumentException e) {
                accionEnum = null;
            }
            if (accionEnum != null) {
                final Auditoria.AccionEnum accionFinal = accionEnum;
                spec = spec.and((root, query, cb) -> cb.equal(root.get("accion"), accionFinal));
            }
        }
        if (usuarioId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId));
        }
        if (desde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), desde));
        }
        if (hasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), hasta));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("tablaAfectada")), like),
                    cb.like(cb.lower(root.get("usuario").get("username")), like),
                    cb.like(cb.lower(root.get("ipUsuario")), like)));
        }

        return auditoriaRepository.findAll(spec, pageable).map(this::toDTO);
    }

    public List<String> listarTablasAfectadas() {
        return auditoriaRepository.listarTablasAfectadas();
    }

    private AuditoriaDTO toDTO(Auditoria a) {
        AuditoriaDTO dto = new AuditoriaDTO();
        dto.setId(a.getId());
        dto.setTablaAfectada(a.getTablaAfectada());
        dto.setAccion(a.getAccion() != null ? a.getAccion().name() : null);
        dto.setRegistroId(a.getRegistroId());
        dto.setValoresAnteriores(a.getValoresAnteriores());
        dto.setNuevosValores(a.getNuevosValores());
        dto.setFecha(a.getFecha());
        dto.setIpUsuario(a.getIpUsuario());
        if (a.getUsuario() != null) {
            dto.setUsuarioId(a.getUsuario().getId());
            dto.setUsuarioUsername(a.getUsuario().getUsername());
        }
        return dto;
    }
}