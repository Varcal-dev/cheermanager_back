package com.varcal.cheermanager.Service.Legal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Legal.AceptacionLegal;
import com.varcal.cheermanager.repository.Legal.AceptacionLegalRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** Aceptacion de terminos y politica (L1): exige confirmar todo, registra quien/cuando/IP/version y es idempotente. */
class LegalServiceTest {

    final List<AceptacionLegal> guardadas = new ArrayList<>();

    LegalService servicio() {
        return servicio(new ArrayList<>());
    }

    LegalService servicio(List<AceptacionLegal> previas) {
        guardadas.clear();
        guardadas.addAll(previas);
        Repo repo = new Repo()
                .on("existsByTipoAndVersion", a -> guardadas.stream()
                        .anyMatch(x -> x.getTipo().equals(a[0]) && x.getVersion().equals(a[1])))
                .on("save", a -> { guardadas.add((AceptacionLegal) a[0]); return a[0]; })
                .on("findAllByOrderByFechaDescIdDesc", a -> new ArrayList<>(guardadas));
        return new LegalService(Fakes.proxy(AceptacionLegalRepository.class, repo));
    }

    @Test
    void club_sin_aceptaciones_no_esta_al_dia() {
        LegalService s = servicio();
        assertFalse(s.clubAlDia());
        assertFalse(s.estado().isAlDia());
        assertEquals(2, s.estado().getDocumentos().size());
    }

    @Test
    void aceptar_ambos_documentos_deja_al_club_al_dia_y_registra_datos() {
        LegalService s = servicio();
        LegalService.Estado e = s.aceptar(7, "190.1.2.3", Set.of(LegalService.TERMINOS, LegalService.POLITICA));
        assertTrue(e.isAlDia());
        assertTrue(s.clubAlDia());
        assertEquals(2, guardadas.size());
        for (AceptacionLegal a : guardadas) {
            assertEquals(7, a.getUsuarioId());
            assertEquals("190.1.2.3", a.getIp());
            assertEquals("2026-11", a.getVersion());
            assertNotNull(a.getFecha());
        }
    }

    @Test
    void falta_confirmar_un_documento_no_guarda_nada() {
        LegalService s = servicio();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> s.aceptar(7, "1.1.1.1", Set.of(LegalService.TERMINOS)));
        assertTrue(ex.getMessage().contains("Política"));
        assertTrue(guardadas.isEmpty());
        assertFalse(s.clubAlDia());
    }

    @Test
    void sin_confirmaciones_o_nulo_se_rechaza() {
        LegalService s = servicio();
        assertThrows(IllegalArgumentException.class, () -> s.aceptar(7, null, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> s.aceptar(7, null, null));
        assertTrue(guardadas.isEmpty());
    }

    @Test
    void aceptar_dos_veces_no_duplica() {
        LegalService s = servicio();
        s.aceptar(7, "1.1.1.1", Set.of(LegalService.TERMINOS, LegalService.POLITICA));
        s.aceptar(8, "2.2.2.2", Set.of());
        assertEquals(2, guardadas.size());
        assertEquals(7, guardadas.get(0).getUsuarioId());
    }

    @Test
    void version_nueva_deja_pendiente_aunque_haya_aceptado_la_anterior() {
        AceptacionLegal vieja = new AceptacionLegal();
        vieja.setTipo(LegalService.TERMINOS);
        vieja.setVersion("2025-01");
        LegalService s = servicio(List.of(vieja));
        assertFalse(s.clubAlDia());
        assertFalse(s.estado().getDocumentos().get(0).isAceptado());
    }

    @Test
    void ip_se_recorta_y_vacia_es_null() {
        assertNull(LegalService.recortarIp(null));
        assertNull(LegalService.recortarIp("  "));
        assertEquals(64, LegalService.recortarIp("x".repeat(100)).length());
    }

    @Test
    void la_plataforma_siempre_esta_al_dia_y_un_club_real_no() {
        LegalService s = servicio();
        com.varcal.cheermanager.tenant.TenantContext.set(1);
        try {
            assertTrue(s.clubAlDia());
            assertTrue(s.estado().isAlDia());
        } finally {
            com.varcal.cheermanager.tenant.TenantContext.clear();
        }
        com.varcal.cheermanager.tenant.TenantContext.set(2);
        try {
            assertFalse(s.clubAlDia());
        } finally {
            com.varcal.cheermanager.tenant.TenantContext.clear();
        }
    }
}
