package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Service.Clubes.ClubService.CrearClubRequest;
import com.varcal.cheermanager.repository.Auth.PermisoRepository;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Clubes.PlanSaasRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;
import com.varcal.cheermanager.repository.Org_dep.CategoriaNivelRepository;
import com.varcal.cheermanager.repository.Org_dep.DivisionRepository;
import com.varcal.cheermanager.repository.Org_dep.EstadoCategoriaNivelRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.tenant.TenantLookupRepository;

/** 2B: un club nuevo nace exigiendo consentimiento de salud (el administrador puede desactivarlo despues). */
class ClubServiceCrearTest {

    @Test
    void elClubNuevoNaceExigiendoConsentimientoDeSalud() {
        ClubRepository clubRepository = mock(ClubRepository.class);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> {
            Club c = inv.getArgument(0);
            c.setId(10);
            return c;
        });
        TenantLookupRepository lookup = mock(TenantLookupRepository.class);
        PlanSaasRepository planes = mock(PlanSaasRepository.class);
        when(planes.findByCodigo(any())).thenReturn(java.util.Optional.empty());

        ClubService service = new ClubService(clubRepository, mock(RolRepository.class),
                mock(PermisoRepository.class), mock(UserRepository.class), lookup, mock(PasswordEncoder.class),
                mock(PlatformTransactionManager.class), mock(NivelRepository.class), mock(DivisionRepository.class),
                planes, mock(TipoPlanPagoRepository.class), mock(CategoriaNivelRepository.class),
                mock(EstadoCategoriaNivelRepository.class));

        Club creado = service.crearClub(new CrearClubRequest("Club de Prueba", null, "admin_prueba",
                "admin@prueba.co", "una-clave-larga-123", false));

        ArgumentCaptor<Club> guardado = ArgumentCaptor.forClass(Club.class);
        verify(clubRepository).save(guardado.capture());
        assertTrue(guardado.getValue().getExigirConsentimientoSalud(), "el club nuevo debe exigir consentimiento de salud");
        assertTrue(creado.getExigirConsentimientoSalud());
        assertEquals("club-de-prueba", creado.getSlug());
    }
}
