package com.varcal.cheermanager.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.repository.Personas.GeneroRepository;

/**
 * Valida los ids que el cliente envia para columnas Integer SIN relacion JPA
 * (Deportista.nivelActualId, convenioId, Persona.generoId, historiales...).
 *
 * Hibernate filtra por club solo cuando navega una relacion o hace findById.
 * Un Integer suelto se guardaria sin comprobar nada, y asi un club podria
 * apuntar a un nivel/convenio/plan de OTRO club. Aqui se usa findById, que si
 * esta filtrado por @TenantId: un id de otro club responde igual que uno
 * inexistente (mismo mensaje, para no revelar que existe en otro club).
 *
 * Null se acepta: significa "sin valor" y cada servicio decide si es obligatorio.
 */
@Component
public class ReferenciasClub {

    @Autowired
    private NivelRepository nivelRepository;
    @Autowired
    private ConvenioRepository convenioRepository;
    @Autowired
    private PlanPagoRepository planPagoRepository;
    @Autowired
    private GeneroRepository generoRepository;

    public void exigirNivel(Integer id) {
        if (id != null && nivelRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Nivel no encontrado con ID: " + id);
        }
    }

    public void exigirConvenio(Integer id) {
        if (id != null && convenioRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Convenio no encontrado con ID: " + id);
        }
    }

    public void exigirPlanPago(Integer id) {
        if (id != null && planPagoRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Plan de pago no encontrado con ID: " + id);
        }
    }

    /** Genero es catalogo global (no pertenece a un club), solo se comprueba que exista. */
    public void exigirGenero(Integer id) {
        if (id != null && generoRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Genero no encontrado con ID: " + id);
        }
    }
}