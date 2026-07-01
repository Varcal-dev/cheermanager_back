package com.varcal.cheermanager.Service.Evaluaciones.Rutina;

import java.util.List;

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
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.DriverSubCriterioRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.EscalonDriverNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.NivelCompetenciaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.SubCriterioRubricaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TablaCantidadNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeDriverNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeSeccionNivelRepository;

@Service
public class RubricaConfigService {

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
        return nivelRepo.save(nivel);
    }

    public void eliminarNivel(Integer id) {
        if (!nivelRepo.existsById(id)) {
            throw new RuntimeException("Nivel no encontrado con ID: " + id);
        }
        nivelRepo.deleteById(id);
    }

    // ── Sub-criterios ────────────────────────────────────────────────────────
    public List<SubCriterioRubrica> listarSubCriterios() {
        return subCriterioRepo.findAll();
    }

    public SubCriterioRubrica guardarSubCriterio(SubCriterioRubrica sc) {
        return subCriterioRepo.save(sc);
    }

    // ── Tabla de cantidad ────────────────────────────────────────────────────
    public List<TablaCantidadNivel> listarTablaCantidad(Integer nivelId) {
        return tablaCantidadRepo.findByNivelId(nivelId);
    }

    @Transactional
    public TablaCantidadNivel guardarTablaCantidad(TablaCantidadNivel fila) {
        if (fila.getNivel() == null || fila.getNivel().getId() == null) {
            throw new RuntimeException("La fila de tabla de cantidad debe tener un nivel asociado.");
        }
        nivelRepo.findById(fila.getNivel().getId())
                .orElseThrow(() -> new RuntimeException(
                        "Nivel no encontrado con ID: " + fila.getNivel().getId()));
        return tablaCantidadRepo.save(fila);
    }

    public void eliminarTablaCantidad(Integer id) {
        if (!tablaCantidadRepo.existsById(id)) {
            throw new RuntimeException("Fila de tabla de cantidad no encontrada con ID: " + id);
        }
        tablaCantidadRepo.deleteById(id);
    }

    // ── Topes de sección ─────────────────────────────────────────────────────
    public List<TopeSeccionNivel> listarTopesSeccion(Integer nivelId, Integer subCriterioId) {
        return topeSeccionRepo.findByNivelIdAndSubCriterioIdOrderByOrdenDesc(nivelId, subCriterioId);
    }

    @Transactional
    public TopeSeccionNivel guardarTopeSeccion(TopeSeccionNivel tope) {
        if (tope.getNivel() == null || tope.getNivel().getId() == null) {
            throw new RuntimeException("El tope de sección debe tener nivel y sub-criterio asociados.");
        }
        nivelRepo.findById(tope.getNivel().getId())
                .orElseThrow(() -> new RuntimeException(
                        "Nivel no encontrado con ID: " + tope.getNivel().getId()));
        subCriterioRepo.findById(tope.getSubCriterio().getId())
                .orElseThrow(() -> new RuntimeException(
                        "Sub-criterio no encontrado con ID: " + tope.getSubCriterio().getId()));
        return topeSeccionRepo.save(tope);
    }

    public void eliminarTopeSeccion(Integer id) {
        if (!topeSeccionRepo.existsById(id)) {
            throw new RuntimeException("Tope de sección no encontrado con ID: " + id);
        }
        topeSeccionRepo.deleteById(id);
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