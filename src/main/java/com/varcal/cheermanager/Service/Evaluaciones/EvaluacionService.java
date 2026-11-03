package com.varcal.cheermanager.Service.Evaluaciones;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionActualizarDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionPorCriterioDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionResponseDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.PaginaEvaluacionesDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.PromedioCriterioDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.RegistroEvaluacionMasivoDTO;
import com.varcal.cheermanager.Models.Evaluaciones.CriterioEvaluacion;
import com.varcal.cheermanager.Models.Evaluaciones.Evaluacion;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.repository.Evaluaciones.CriterioEvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class EvaluacionService {

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private CriterioEvaluacionRepository criterioEvaluacionRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private DeportistaPerteneceGrupoRepository grupoDeportistaRepository;

    @Transactional
    public EvaluacionResponseDTO registrar(EvaluacionDTO dto) {
        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + dto.getDeportistaId()));

        CriterioEvaluacion criterio = criterioEvaluacionRepository.findById(dto.getCriterioId())
                .orElseThrow(() -> new RuntimeException("Criterio no encontrado con ID: " + dto.getCriterioId()));

        return toResponseDTO(guardarNueva(deportista, criterio, dto.getFecha(), dto.getPuntajeObtenido()));
    }

    /** Valida (escala, fecha, duplicado del mismo dia) y guarda una evaluacion nueva. */
    private Evaluacion guardarNueva(Deportista deportista, CriterioEvaluacion criterio, LocalDate fechaIn, Integer puntaje) {
        EvaluacionReglas.validarPuntaje(puntaje);
        LocalDate fecha = EvaluacionReglas.validarFecha(fechaIn, LocalDate.now());
        if (evaluacionRepository.existsByDeportistaIdAndCriterioEvaluacionIdAndFecha(deportista.getId(),
                criterio.getId(), fecha)) {
            throw new IllegalStateException("Ya existe una evaluacion de '" + criterio.getNombre()
                    + "' para este deportista en esa fecha: corrigela en lugar de crear otra");
        }
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setDeportista(deportista);
        evaluacion.setCriterioEvaluacion(criterio);
        evaluacion.setFecha(fecha);
        evaluacion.setPuntajeObtenido(puntaje);
        return evaluacionRepository.save(evaluacion);
    }

    // Caso de uso real: el entrenador evalua a un deportista en varios criterios de una sola sesion. Un error en un
    // criterio del lote no bloquea el resto (errores por criterioId).
    @Transactional
    public ResultadoRegistroMasivo registrarMasivo(RegistroEvaluacionMasivoDTO dto) {
        if (dto.getRegistros() == null) {
            throw new IllegalArgumentException("Indica al menos un registro");
        }
        EvaluacionReglas.validarLote(dto.getRegistros().size());
        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + dto.getDeportistaId()));

        ResultadoRegistroMasivo resultado = new ResultadoRegistroMasivo();
        for (RegistroEvaluacionMasivoDTO.RegistroIndividual r : dto.getRegistros()) {
            try {
                CriterioEvaluacion criterio = criterioEvaluacionRepository.findById(r.getCriterioId())
                        .orElseThrow(() -> new RuntimeException("Criterio no encontrado con ID: " + r.getCriterioId()));
                resultado.getRegistradas().add(toResponseDTO(
                        guardarNueva(deportista, criterio, dto.getFecha(), r.getPuntajeObtenido())));
            } catch (RuntimeException e) {
                resultado.getErrores().put(r.getCriterioId(), e.getMessage());
            }
        }
        return resultado;
    }

    // S7: un criterio evaluado a varios deportistas (p. ej. todo un grupo) el mismo dia. Errores por deportistaId.
    @Transactional
    public ResultadoRegistroMasivo registrarPorCriterio(EvaluacionPorCriterioDTO dto) {
        if (dto.getCriterioId() == null) {
            throw new IllegalArgumentException("Indica el criterio");
        }
        if (dto.getRegistros() == null) {
            throw new IllegalArgumentException("Indica al menos un registro");
        }
        EvaluacionReglas.validarLote(dto.getRegistros().size());
        CriterioEvaluacion criterio = criterioEvaluacionRepository.findById(dto.getCriterioId())
                .orElseThrow(() -> new RuntimeException("Criterio no encontrado con ID: " + dto.getCriterioId()));

        ResultadoRegistroMasivo resultado = new ResultadoRegistroMasivo();
        for (EvaluacionPorCriterioDTO.Registro r : dto.getRegistros()) {
            try {
                Deportista deportista = deportistaRepository.findById(r.getDeportistaId())
                        .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + r.getDeportistaId()));
                resultado.getRegistradas().add(toResponseDTO(
                        guardarNueva(deportista, criterio, dto.getFecha(), r.getPuntajeObtenido())));
            } catch (RuntimeException e) {
                resultado.getErrores().put(r.getDeportistaId(), e.getMessage());
            }
        }
        return resultado;
    }

    // S7: corregir puntaje y/o fecha de una evaluacion ya registrada.
    @Transactional
    public EvaluacionResponseDTO actualizar(Integer id, EvaluacionActualizarDTO dto) {
        Evaluacion e = evaluacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluacion no encontrada con ID: " + id));
        EvaluacionReglas.validarPuntaje(dto.getPuntajeObtenido());
        LocalDate fecha = EvaluacionReglas.validarFecha(dto.getFecha() != null ? dto.getFecha() : e.getFecha(),
                LocalDate.now());
        if (evaluacionRepository.existsByDeportistaIdAndCriterioEvaluacionIdAndFechaAndIdNot(
                e.getDeportista().getId(), e.getCriterioEvaluacion().getId(), fecha, id)) {
            throw new IllegalStateException("Ya existe otra evaluacion de este criterio para el deportista en esa fecha");
        }
        e.setFecha(fecha);
        e.setPuntajeObtenido(dto.getPuntajeObtenido());
        return toResponseDTO(evaluacionRepository.save(e));
    }

    // S7: filtros opcionales + paginacion (la pantalla de Evaluaciones ya no baja todo el historial del club).
    @Transactional(readOnly = true)
    public PaginaEvaluacionesDTO buscar(Integer deportistaId, Integer grupoId, Integer categoriaId, Integer criterioId,
            LocalDate desde, LocalDate hasta, String texto, Integer pagina, Integer tamano) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        int tam = EvaluacionReglas.tamanoPagina(tamano);
        int pag = pagina == null ? 0 : Math.max(0, pagina);
        Page<Evaluacion> p = evaluacionRepository.buscar(deportistaId, grupoId, categoriaId, criterioId, desde, hasta,
                EvaluacionReglas.patronBusqueda(texto), PageRequest.of(pag, tam));
        PaginaEvaluacionesDTO out = new PaginaEvaluacionesDTO();
        out.setContenido(p.getContent().stream().map(this::toResponseDTO).collect(Collectors.toList()));
        out.setTotal(p.getTotalElements());
        out.setPagina(pag);
        out.setTamano(tam);
        out.setTotalPaginas(p.getTotalPages());
        return out;
    }

    @Transactional(readOnly = true)
    public List<PromedioCriterioDTO> promedios(Integer grupoId, Integer categoriaId, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        List<PromedioCriterioDTO> out = new ArrayList<>();
        for (Object[] f : evaluacionRepository.promediosPorCriterio(grupoId, categoriaId, desde, hasta)) {
            PromedioCriterioDTO d = new PromedioCriterioDTO();
            d.setCriterioId(((Number) f[0]).intValue());
            d.setCriterio(String.valueOf(f[1]));
            d.setCategoria(String.valueOf(f[2]));
            d.setPromedio(Math.round(((Number) f[3]).doubleValue() * 100.0) / 100.0);
            d.setEvaluaciones(((Number) f[4]).longValue());
            d.setDeportistas(((Number) f[5]).longValue());
            out.add(d);
        }
        return out;
    }

    /** Deportistas que hoy pertenecen al grupo (para armar el formulario de captura). Ordenados por nombre. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> deportistasDelGrupo(Integer grupoId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (DeportistaPerteneceGrupo m : grupoDeportistaRepository.findByGrupoIdAndFechaFinIsNull(grupoId)) {
            Map<String, Object> f = new HashMap<>();
            f.put("deportistaId", m.getDeportista().getId());
            f.put("nombre", m.getDeportista().getPersona().getNombre() + " " + m.getDeportista().getPersona().getApellidos());
            out.add(f);
        }
        out.sort(Comparator.comparing(f -> String.valueOf(f.get("nombre")).toLowerCase()));
        return out;
    }

    public List<EvaluacionResponseDTO> listarPorDeportista(Integer deportistaId) {
        if (!deportistaRepository.existsById(deportistaId)) {
            throw new RuntimeException("Deportista no encontrado con ID: " + deportistaId);
        }
        return evaluacionRepository.findByDeportistaIdOrderByFechaDesc(deportistaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Vista general sin filtros: se conserva por compatibilidad, pero la pantalla de Evaluaciones usa buscar().
    public List<EvaluacionResponseDTO> obtenerTodas() {
        return evaluacionRepository.findAll().stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Progresion de un deportista en un criterio especifico a lo largo del tiempo (para graficar).
    public List<EvaluacionResponseDTO> progresionPorCriterio(Integer deportistaId, Integer criterioId) {
        return evaluacionRepository.findByDeportistaIdAndCriterioEvaluacionIdOrderByFechaAsc(deportistaId, criterioId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EvaluacionResponseDTO> listarPorDeportistaYRango(Integer deportistaId, LocalDate desde, LocalDate hasta) {
        return evaluacionRepository.findByDeportistaIdAndFechaBetweenOrderByFechaDesc(deportistaId, desde, hasta)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Ultima "foto" del deportista en todos los criterios de una categoria.
    public List<EvaluacionResponseDTO> ultimaEvaluacionPorCategoria(Integer deportistaId, Integer categoriaId) {
        return evaluacionRepository.findUltimaEvaluacionPorCategoria(deportistaId, categoriaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public Double promedioPorCriterio(Integer deportistaId, Integer criterioId) {
        Double promedio = evaluacionRepository.promedioPorCriterio(deportistaId, criterioId);
        return promedio != null ? promedio : 0.0;
    }

    public void eliminar(Integer id) {
        if (!evaluacionRepository.existsById(id)) {
            throw new RuntimeException("Evaluacion no encontrada con ID: " + id);
        }
        evaluacionRepository.deleteById(id);
    }

    private EvaluacionResponseDTO toResponseDTO(Evaluacion e) {
        EvaluacionResponseDTO dto = new EvaluacionResponseDTO();
        dto.setId(e.getId());
        dto.setDeportistaId(e.getDeportista().getId());
        dto.setNombreDeportista(e.getDeportista().getPersona().getNombre() + " "
                + e.getDeportista().getPersona().getApellidos());
        dto.setCriterioId(e.getCriterioEvaluacion().getId());
        dto.setNombreCriterio(e.getCriterioEvaluacion().getNombre());
        dto.setCategoria(e.getCriterioEvaluacion().getCategoria().getCategoria());
        dto.setFecha(e.getFecha());
        dto.setPuntajeObtenido(e.getPuntajeObtenido());
        return dto;
    }

    /** registradas + errores. La clave de errores es el criterioId (registro por deportista) o el deportistaId (por criterio). */
    public static class ResultadoRegistroMasivo {
        private final List<EvaluacionResponseDTO> registradas = new ArrayList<>();
        private final Map<Integer, String> errores = new HashMap<>();

        public List<EvaluacionResponseDTO> getRegistradas() {
            return registradas;
        }

        public Map<Integer, String> getErrores() {
            return errores;
        }
    }
}
