package com.varcal.cheermanager.Service.Clubes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo;
import com.varcal.cheermanager.repository.Consentimientos.ConsentimientoRepository;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Org_dep.DivisionRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.repository.Org_dep.TemporadaRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;

/**
 * Lista de pasos para dejar un club listo para operar, calculada con datos reales del club
 * del usuario (todos los repositorios ya filtran por club).
 */
@Service
public class OnboardingService {

    public record Paso(String clave, String titulo, boolean completo, String detalle) {
    }

    public record Estado(int completados, int total, List<Paso> pasos) {
    }

    private final NivelRepository nivelRepository;
    private final DivisionRepository divisionRepository;
    private final PlanPagoRepository planPagoRepository;
    private final TemporadaRepository temporadaRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final GrupoEntrenamientoRepository grupoRepository;
    private final DeportistaRepository deportistaRepository;
    private final DeportistaAcudienteRepository vinculoRepository;
    private final ConsentimientoRepository consentimientoRepository;

    public OnboardingService(NivelRepository nivelRepository, DivisionRepository divisionRepository,
            PlanPagoRepository planPagoRepository, TemporadaRepository temporadaRepository,
            EntrenadorRepository entrenadorRepository, GrupoEntrenamientoRepository grupoRepository,
            DeportistaRepository deportistaRepository, DeportistaAcudienteRepository vinculoRepository,
            ConsentimientoRepository consentimientoRepository) {
        this.nivelRepository = nivelRepository;
        this.divisionRepository = divisionRepository;
        this.planPagoRepository = planPagoRepository;
        this.temporadaRepository = temporadaRepository;
        this.entrenadorRepository = entrenadorRepository;
        this.grupoRepository = grupoRepository;
        this.deportistaRepository = deportistaRepository;
        this.vinculoRepository = vinculoRepository;
        this.consentimientoRepository = consentimientoRepository;
    }

    public Estado estado() {
        List<Paso> pasos = new ArrayList<>();
        pasos.add(conteo("niveles", "Definir los niveles del club", nivelRepository.count()));
        pasos.add(conteo("divisiones", "Definir las divisiones", divisionRepository.count()));
        pasos.add(conteo("planes", "Crear al menos un plan de pago", planPagoRepository.count()));
        pasos.add(new Paso("temporada", "Activar la temporada actual", temporadaRepository.findFirstByActivaTrue().isPresent(),
                temporadaRepository.findFirstByActivaTrue().map(t -> "Activa: " + t.getNombre()).orElse("Sin temporada activa")));
        pasos.add(conteo("entrenadores", "Registrar entrenadores", entrenadorRepository.count()));
        pasos.add(conteo("grupos", "Crear grupos de entrenamiento", grupoRepository.count()));
        pasos.add(conteo("deportistas", "Registrar deportistas", deportistaRepository.count()));

        long sinAcudiente = vinculoRepository.contarMenoresSinAcudiente(LocalDate.now().minusYears(18));
        pasos.add(new Paso("acudientes", "Todo deportista menor de edad tiene acudiente", sinAcudiente == 0,
                sinAcudiente == 0 ? "Sin pendientes" : sinAcudiente + " menores sin acudiente"));

        int sinSalud = consentimientoRepository.deportistasSinConsentimiento(Tipo.DATOS_SALUD).size();
        pasos.add(new Paso("consentimientos", "Consentimiento de datos de salud de cada deportista", sinSalud == 0,
                sinSalud == 0 ? "Sin pendientes" : sinSalud + " deportistas sin consentimiento vigente"));

        int ok = (int) pasos.stream().filter(Paso::completo).count();
        return new Estado(ok, pasos.size(), pasos);
    }

    private static Paso conteo(String clave, String titulo, long n) {
        return new Paso(clave, titulo, n > 0, n + " registrados");
    }
}