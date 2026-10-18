package com.varcal.cheermanager.tenant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.varcal.cheermanager.Models.Financiero.Convenio;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.Models.Personas.Genero;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.repository.Personas.GeneroRepository;

@ExtendWith(MockitoExtension.class)
class ReferenciasClubTest {

    @Mock NivelRepository nivelRepository;
    @Mock ConvenioRepository convenioRepository;
    @Mock PlanPagoRepository planPagoRepository;
    @Mock GeneroRepository generoRepository;
    @InjectMocks ReferenciasClub referencias;

    @Test
    void nullSeAceptaYNoConsultaLaBase() {
        referencias.exigirNivel(null);
        referencias.exigirConvenio(null);
        referencias.exigirPlanPago(null);
        referencias.exigirGenero(null);
        verifyNoInteractions(nivelRepository, convenioRepository, planPagoRepository, generoRepository);
    }

    @Test
    void idQueElFiltroDeClubNoVeSeRechaza() {
        when(nivelRepository.findById(7)).thenReturn(Optional.empty());
        when(convenioRepository.findById(7)).thenReturn(Optional.empty());
        when(planPagoRepository.findById(7)).thenReturn(Optional.empty());
        when(generoRepository.findById(7)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> referencias.exigirNivel(7));
        assertThrows(RuntimeException.class, () -> referencias.exigirConvenio(7));
        assertThrows(RuntimeException.class, () -> referencias.exigirPlanPago(7));
        assertThrows(RuntimeException.class, () -> referencias.exigirGenero(7));
    }

    @Test
    void idDelPropioClubPasa() {
        when(nivelRepository.findById(1)).thenReturn(Optional.of(new Nivel()));
        when(convenioRepository.findById(1)).thenReturn(Optional.of(new Convenio()));
        when(planPagoRepository.findById(1)).thenReturn(Optional.of(new PlanPago()));
        when(generoRepository.findById(1)).thenReturn(Optional.of(new Genero()));
        assertDoesNotThrow(() -> {
            referencias.exigirNivel(1);
            referencias.exigirConvenio(1);
            referencias.exigirPlanPago(1);
            referencias.exigirGenero(1);
        });
    }
}
