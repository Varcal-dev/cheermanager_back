package com.varcal.cheermanager.Service.Funcionalidad;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Funcionalidad.LogAccesoDTO;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Funcionalidad.LogAcceso;
import com.varcal.cheermanager.repository.Funcionalidad.LogAccesoRepository;

@Service
public class LogAccesoService {

    @Autowired
    private LogAccesoRepository logAccesoRepository;

    // Se llama desde AuthController, no desde AuthService, porque la IP del
    // request solo está disponible a nivel de controller (HttpServletRequest).
    // usuario puede ser null (ej. intento de login con un email que no existe).
    public void registrarAcceso(Usuario usuario, String emailIntento, String accion, String ipOrigen, String detalle) {
        LogAcceso log = new LogAcceso();
        log.setUsuario(usuario);
        log.setEmailIntento(emailIntento);
        log.setAccion(accion);
        log.setIpOrigen(ipOrigen);
        log.setDetalle(detalle);
        log.setFecha(LocalDateTime.now());
        logAccesoRepository.save(log);
    }

    public Page<LogAccesoDTO> listarPorUsuario(Integer usuarioId, Pageable pageable) {
        return logAccesoRepository.findByUsuarioIdOrderByFechaDesc(usuarioId, pageable).map(this::toDTO);
    }

    public Page<LogAccesoDTO> listarPorAccion(String accion, Pageable pageable) {
        return logAccesoRepository.findByAccionOrderByFechaDesc(accion, pageable).map(this::toDTO);
    }

    public Page<LogAccesoDTO> listarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        return logAccesoRepository.findByFechaBetweenOrderByFechaDesc(desde, hasta, pageable).map(this::toDTO);
    }

    // Útil para detectar patrones de ataque: cuántos intentos fallidos ha
    // tenido un email en una ventana de tiempo reciente, sin importar si
    // existe usuario asociado.
    public long contarIntentosFallidosRecientes(String email, int minutosHaciaAtras) {
        LocalDateTime desde = LocalDateTime.now().minusMinutes(minutosHaciaAtras);
        return logAccesoRepository.findByEmailIntentoAndFechaAfter(email, desde).stream()
                .filter(l -> "LOGIN_FALLIDO".equals(l.getAccion()))
                .collect(Collectors.counting());
    }

    private LogAccesoDTO toDTO(LogAcceso log) {
        LogAccesoDTO dto = new LogAccesoDTO();
        dto.setId(log.getId());
        dto.setEmailIntento(log.getEmailIntento());
        dto.setFecha(log.getFecha());
        dto.setAccion(log.getAccion());
        dto.setIpOrigen(log.getIpOrigen());
        dto.setDetalle(log.getDetalle());
        if (log.getUsuario() != null) {
            dto.setUsuarioId(log.getUsuario().getId());
            dto.setUsername(log.getUsuario().getUsername());
        }
        return dto;
    }
}