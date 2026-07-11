package com.varcal.cheermanager.Service.Dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenAdministracionDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenAdministracionDTO.RegistroAuditoriaDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenEvaluacionDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenFinanzasDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenHorariosDTO;
import com.varcal.cheermanager.Models.Funcionalidad.Auditoria;
import com.varcal.cheermanager.Models.Funcionalidad.LogAcceso;
import com.varcal.cheermanager.repository.Funcionalidad.AuditoriaRepository;
import com.varcal.cheermanager.repository.Funcionalidad.LogAccesoRepository;

@Service
public class DashboardAdministracionService {

    private static final int VENTANA_DIAS_USO_SISTEMA = 7;
    private static final int MAX_AUDITORIA_RECIENTE = 20;

    // Se reutilizan los dashboards ya construidos en vez de recalcular la
    // misma lógica de negocio dos veces — Administración es un agregador,
    // no una fuente de verdad propia.
    @Autowired
    private DashboardFinanzasService dashboardFinanzasService;

    @Autowired
    private DashboardHorariosService dashboardHorariosService;

    @Autowired
    private DashboardEvaluacionService dashboardEvaluacionService;

    @Autowired
    private LogAccesoRepository logAccesoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    public ResumenAdministracionDTO obtenerResumen() {
        ResumenAdministracionDTO dto = new ResumenAdministracionDTO();

        ResumenFinanzasDTO finanzas = dashboardFinanzasService.obtenerResumen();
        dto.setRecaudoMesActual(finanzas.getRecaudoMesActual());
        dto.setCarteraPendienteTotal(finanzas.getCarteraPendienteTotal());

        ResumenHorariosDTO horarios = dashboardHorariosService.obtenerResumen(30);
        dto.setPorcentajeAsistenciaGeneral30Dias(horarios.getPorcentajeAsistenciaGeneral());

        calcularSaludCompetitiva(dto);
        calcularUsoDelSistema(dto);
        dto.setAuditoriaReciente(obtenerAuditoriaReciente());

        return dto;
    }

    private void calcularSaludCompetitiva(ResumenAdministracionDTO dto) {
        ResumenEvaluacionDTO evaluacion = dashboardEvaluacionService.obtenerResumen();
        List<Double> puntajes = evaluacion.getUltimosPuntajesPorGrupo().stream()
                .map(g -> g.getPuntajeTotal() != null ? g.getPuntajeTotal().doubleValue() : null)
                .filter(p -> p != null)
                .collect(Collectors.toList());

        dto.setGruposConEvaluacionReciente(puntajes.size());
        dto.setPuntajePromedioClub(puntajes.isEmpty() ? null
                : redondear(puntajes.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
    }

    private void calcularUsoDelSistema(ResumenAdministracionDTO dto) {
        LocalDateTime desde = LocalDateTime.now().minusDays(VENTANA_DIAS_USO_SISTEMA);
        LocalDateTime hasta = LocalDateTime.now();

        // Pageable.unpaged() trae todo sin metadata de paginación — a la
        // escala de un club, el volumen de logs de 7 días es perfectamente
        // manejable en memoria.
        List<LogAcceso> logs = logAccesoRepository
                .findByFechaBetweenOrderByFechaDesc(desde, hasta, Pageable.unpaged())
                .getContent();

        dto.setLoginsExitosos7Dias(logs.stream().filter(l -> "LOGIN_EXITOSO".equals(l.getAccion())).count());
        dto.setLoginsFallidos7Dias(logs.stream().filter(l -> "LOGIN_FALLIDO".equals(l.getAccion())).count());

        Set<Integer> usuariosDistintos = logs.stream()
                .filter(l -> "LOGIN_EXITOSO".equals(l.getAccion()) && l.getUsuario() != null)
                .map(l -> l.getUsuario().getId())
                .collect(Collectors.toSet());
        dto.setUsuariosActivos7Dias(usuariosDistintos.size());
    }

    // NOTA: si esto devuelve siempre una lista vacía, es porque no
    // encontramos en el proyecto ningún Service/Aspect que efectivamente
    // escriba en la tabla `auditoria` — el Model existe (y ahora también su
    // Repository), pero nada la está poblando todavía. Eso es un pendiente
    // aparte, no un bug de este dashboard.
    private List<RegistroAuditoriaDTO> obtenerAuditoriaReciente() {
        LocalDateTime desde = LocalDateTime.now().minusDays(30);
        List<Auditoria> registros = auditoriaRepository.findByFechaAfterOrderByFechaDesc(
                desde, Pageable.ofSize(MAX_AUDITORIA_RECIENTE));

        return registros.stream().map(a -> {
            RegistroAuditoriaDTO item = new RegistroAuditoriaDTO();
            item.setUsuario(a.getUsuario() != null ? a.getUsuario().getUsername() : "Desconocido");
            item.setTablaAfectada(a.getTablaAfectada());
            item.setAccion(a.getAccion().name());
            item.setRegistroId(a.getRegistroId());
            item.setFecha(a.getFecha());
            return item;
        }).collect(Collectors.toList());
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}