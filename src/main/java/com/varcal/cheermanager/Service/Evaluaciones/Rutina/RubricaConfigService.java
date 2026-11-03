package com.varcal.cheermanager.Service.Evaluaciones.Rutina;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.DriverSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EscalonDriverNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TablaCantidadNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeDriverNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeSeccionNivel;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.DriverSubCriterioRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.EscalonDriverNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.NivelCompetenciaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.SubCriterioRubricaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TablaCantidadNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeDriverNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeSeccionNivelRepository;

@Service
public class RubricaConfigService {

    private static final Logger log = LoggerFactory.getLogger(RubricaConfigService.class);

    /** La rubrica es global: todo cambio queda en el log con el usuario de plataforma que lo hizo. */
    private static void auditar(String accion, Object detalle) {
        log.info("RUBRICA {} por usuario {}: {}", accion, FacturaService.usuarioActualId(), detalle);
    }

    @Autowired private NivelCompetenciaRepository nivelRepo;
    @Autowired private SubCriterioRubricaRepository subCriterioRepo;
    @Autowired private TablaCantidadNivelRepository tablaCantidadRepo;
    @Autowired private TopeSeccionNivelRepository topeSeccionRepo;
    @Autowired private DriverSubCriterioRepository driverRepo;
    @Autowired private TopeDriverNivelRepository topeDriverRepo;
    @Autowired private EscalonDriverNivelRepository escalonDriverRepo;

    // ── Niveles ──────────────────────────────────────────────────────────────
    public List<NivelCompetencia> listarNiveles() {
        return nivelRepo.findAll();
    }

    public NivelCompetencia guardarNivel(NivelCompetencia nivel) {
        nivel.setNombre(RubricaValidacion.nombreNivel(nivel.getNombre(), nivel.getModalidad()));
        nivel.setModalidad(nivel.getModalidad().trim());
        if (nivel.getActivo() == null) {
            nivel.setActivo(true);
        }
        nivelRepo.findByNombre(nivel.getNombre()).ifPresent(otro -> {
            if (!otro.getId().equals(nivel.getId())) {
                throw new IllegalStateException("Ya existe un nivel llamado '" + nivel.getNombre() + "'");
            }
        });
        NivelCompetencia g = nivelRepo.save(nivel);
        auditar(nivel.getId() == null ? "crea nivel" : "modifica nivel", g.getNombre() + " activo=" + g.getActivo());
        return g;
    }

    // Un nivel que ya tiene configuracion o evaluaciones (de cualquier club) no se borra: se desactiva.
    public void eliminarNivel(Integer id) {
        if (!nivelRepo.existsById(id)) {
            throw new RuntimeException("Nivel no encontrado con ID: " + id);
        }
        try {
            nivelRepo.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "El nivel tiene configuracion o evaluaciones asociadas: desactivalo en lugar de eliminarlo");
        }
        auditar("elimina nivel", id);
    }

    // ── Sub-criterios ────────────────────────────────────────────────────────
    public List<SubCriterioRubrica> listarSubCriterios() {
        return subCriterioRepo.findAll();
    }

    public SubCriterioRubrica guardarSubCriterio(SubCriterioRubrica sc) {
        RubricaValidacion.subCriterio(sc.getNombre(), sc.getSeccion(), sc.getTipoCalculo());
        sc.setNombre(sc.getNombre().trim());
        subCriterioRepo.findAll().stream()
                .filter(o -> o.getNombre().equalsIgnoreCase(sc.getNombre()) && !o.getId().equals(sc.getId()))
                .findAny().ifPresent(o -> {
                    throw new IllegalStateException("Ya existe un sub-criterio llamado '" + sc.getNombre() + "'");
                });
        SubCriterioRubrica g = subCriterioRepo.save(sc);
        auditar(sc.getId() == null ? "crea sub-criterio" : "modifica sub-criterio", g.getNombre());
        return g;
    }

    // ── Tabla de cantidad ────────────────────────────────────────────────────
    public List<TablaCantidadNivel> listarTablaCantidad(Integer nivelId) {
        return tablaCantidadRepo.findByNivelId(nivelId);
    }

    @Transactional
    public TablaCantidadNivel guardarTablaCantidad(TablaCantidadNivel fila) {
        if (fila.getNivel() == null || fila.getNivel().getId() == null) {
            throw new IllegalArgumentException("La fila de tabla de cantidad debe tener un nivel asociado.");
        }
        nivelRepo.findById(fila.getNivel().getId())
                .orElseThrow(() -> new RuntimeException(
                        "Nivel no encontrado con ID: " + fila.getNivel().getId()));
        List<RubricaValidacion.RangoExistente> otros = tablaCantidadRepo.findByNivelId(fila.getNivel().getId()).stream()
                .filter(t -> t.getTabla().equals(fila.getTabla()) && !t.getId().equals(fila.getId()))
                .map(t -> new RubricaValidacion.RangoExistente(t.getId(), t.getRangoMinAtletas(), t.getRangoMaxAtletas()))
                .collect(Collectors.toList());
        RubricaValidacion.tablaCantidad(fila.getTabla(), fila.getUnidad(), fila.getRangoMinAtletas(),
                fila.getRangoMaxAtletas(), fila.getMayoria(), fila.getGranParte(), fila.getMaximo(), otros);
        TablaCantidadNivel g = tablaCantidadRepo.save(fila);
        auditar(fila.getId() == null ? "crea fila de tabla de cantidad" : "modifica fila de tabla de cantidad",
                "nivel " + g.getNivel().getId() + " " + g.getTabla() + " " + g.getRangoMinAtletas() + "-" + g.getRangoMaxAtletas());
        return g;
    }

    public void eliminarTablaCantidad(Integer id) {
        if (!tablaCantidadRepo.existsById(id)) {
            throw new RuntimeException("Fila de tabla de cantidad no encontrada con ID: " + id);
        }
        tablaCantidadRepo.deleteById(id);
        auditar("elimina fila de tabla de cantidad", id);
    }

    // ── Topes de sección ─────────────────────────────────────────────────────
    public List<TopeSeccionNivel> listarTopesSeccion(Integer nivelId, Integer subCriterioId) {
        return topeSeccionRepo.findByNivelIdAndSubCriterioIdOrderByOrdenDesc(nivelId, subCriterioId);
    }

    @Transactional
    public TopeSeccionNivel guardarTopeSeccion(TopeSeccionNivel tope) {
        if (tope.getNivel() == null || tope.getNivel().getId() == null
                || tope.getSubCriterio() == null || tope.getSubCriterio().getId() == null) {
            throw new IllegalArgumentException("El tope de sección debe tener nivel y sub-criterio asociados.");
        }
        nivelRepo.findById(tope.getNivel().getId())
                .orElseThrow(() -> new RuntimeException(
                        "Nivel no encontrado con ID: " + tope.getNivel().getId()));
        subCriterioRepo.findById(tope.getSubCriterio().getId())
                .orElseThrow(() -> new RuntimeException(
                        "Sub-criterio no encontrado con ID: " + tope.getSubCriterio().getId()));
        List<Integer> otrosOrdenes = topeSeccionRepo
                .findByNivelIdAndSubCriterioIdOrderByOrdenDesc(tope.getNivel().getId(), tope.getSubCriterio().getId())
                .stream().filter(t -> !t.getId().equals(tope.getId())).map(TopeSeccionNivel::getOrden)
                .collect(Collectors.toList());
        RubricaValidacion.topeSeccion(tope.getOrden(), tope.getValorPuntaje(), tope.getCondicionHabilidadesDiferentes(),
                tope.getCondicionGrupoMinimo(), tope.getDescripcionRubrica(), otrosOrdenes);
        if (tope.getCondicionGrupoMinimo() != null && tope.getCondicionGrupoMinimo().isBlank()) {
            tope.setCondicionGrupoMinimo(null);
        }
        TopeSeccionNivel g = topeSeccionRepo.save(tope);
        auditar(tope.getId() == null ? "crea escalon" : "modifica escalon",
                "nivel " + tope.getNivel().getId() + " sub-criterio " + tope.getSubCriterio().getId() + " orden " + g.getOrden()
                        + " valor " + g.getValorPuntaje());
        return g;
    }

    public List<TopeSeccionNivel> listarTopesSeccionPorNivel(Integer nivelId) {
        return topeSeccionRepo.findByNivelIdOrderBySubCriterioIdAscOrdenDesc(nivelId);
    }

    public void eliminarTopeSeccion(Integer id) {
        if (!topeSeccionRepo.existsById(id)) {
            throw new RuntimeException("Tope de sección no encontrado con ID: " + id);
        }
        topeSeccionRepo.deleteById(id);
        auditar("elimina escalon", id);
    }

    // ── Drivers ──────────────────────────────────────────────────────────────
    public List<DriverSubCriterio> listarDrivers(Integer subCriterioId) {
        return driverRepo.findBySubCriterioId(subCriterioId);
    }

    public DriverSubCriterio guardarDriver(DriverSubCriterio driver) {
        return driverRepo.save(driver);
    }

    @Transactional
    public TopeDriverNivel guardarTopeDriver(TopeDriverNivel tope) {
        return topeDriverRepo.save(tope);
    }

    // Sin @Transactional: es solo lectura, usado por el formulario de
    // captura para mostrar los valores máximos de cada driver (Grado de
    // Dificultad / Participación Máxima) como guía mientras se digita.
    public TopeDriverNivel obtenerTopeDriver(Integer driverId, Integer nivelId) {
        return topeDriverRepo.findByDriverIdAndNivelId(driverId, nivelId).orElse(null);
    }

    @Transactional
    public EscalonDriverNivel guardarEscalonDriver(EscalonDriverNivel escalon) {
        return escalonDriverRepo.save(escalon);
    }

    public List<EscalonDriverNivel> listarEscalonesDriver(Integer driverId, Integer nivelId) {
        return escalonDriverRepo.findByDriverIdAndNivelIdOrderByOrden(driverId, nivelId);
    }

    public void eliminarEscalonDriver(Integer id) {
        if (!escalonDriverRepo.existsById(id)) {
            throw new RuntimeException("Escalón de driver no encontrado con ID: " + id);
        }
        escalonDriverRepo.deleteById(id);
    }
}