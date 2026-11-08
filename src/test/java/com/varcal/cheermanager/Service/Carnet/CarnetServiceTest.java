package com.varcal.cheermanager.Service.Carnet;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C13: el codigo del carnet va firmado, no se puede alterar ni usar en otro club. */
class CarnetServiceTest {

    CarnetService servicio(String secreto, String estado) {
        Persona p = new Persona();
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        EstadoPersona ep = new EstadoPersona();
        ep.setEstado(estado);
        Deportista d = new Deportista();
        d.setId(7);
        d.setPersona(p);
        d.setEstado(ep);
        GrupoEntrenamiento g = new GrupoEntrenamiento();
        g.setNombre("Senior N3");
        DeportistaPerteneceGrupo m = new DeportistaPerteneceGrupo();
        m.setGrupo(g);
        Club club = new Club();
        club.setNombre("Ratels");
        Repo deps = new Repo().on("findById", a -> Optional.ofNullable(((Integer) a[0]) == 7 ? d : null));
        Repo mem = new Repo().on("findByDeportistaIdAndFechaFinIsNull", a -> List.of(m));
        Repo clubes = new Repo().on("findById", a -> Optional.of(club));
        return new CarnetService(secreto, Fakes.proxy(DeportistaRepository.class, deps),
                Fakes.proxy(DeportistaPerteneceGrupoRepository.class, mem), Fakes.proxy(ClubRepository.class, clubes));
    }

    @Test
    void el_carnet_se_arma_y_su_codigo_se_verifica() {
        TenantContext.set(2);
        try {
            CarnetService s = servicio("secreto", "Activo");
            CarnetService.Carnet c = s.carnet(7);
            assertEquals("Ana Pérez", c.nombre());
            assertEquals("Ratels", c.club());
            assertEquals(List.of("Senior N3"), c.grupos());
            assertTrue(c.vigente());
            assertTrue(c.codigo().startsWith("CM1.2.7."));
            assertEquals(7, s.verificar(c.codigo()).deportistaId());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void un_retirado_se_verifica_pero_no_esta_vigente() {
        TenantContext.set(2);
        try {
            CarnetService s = servicio("secreto", "Retirado");
            assertFalse(s.verificar(s.carnet(7).codigo()).vigente());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void codigo_alterado_o_con_otro_secreto_se_rechaza() {
        TenantContext.set(2);
        try {
            CarnetService s = servicio("secreto", "Activo");
            String codigo = s.carnet(7).codigo();
            assertThrows(IllegalArgumentException.class, () -> s.verificar(codigo.replace(".7.", ".8.")));
            assertThrows(IllegalArgumentException.class, () -> servicio("otro", "Activo").verificar(codigo));
            assertThrows(IllegalArgumentException.class, () -> s.verificar("basura"));
            assertThrows(IllegalArgumentException.class, () -> s.verificar(null));
            assertThrows(IllegalArgumentException.class, () -> s.verificar("CM1.a.b.c"));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void carnet_de_otro_club_no_se_acepta() {
        String codigo;
        TenantContext.set(2);
        try {
            codigo = servicio("secreto", "Activo").carnet(7).codigo();
        } finally {
            TenantContext.clear();
        }
        TenantContext.set(3);
        try {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> servicio("secreto", "Activo").verificar(codigo));
            assertTrue(ex.getMessage().contains("otro club"));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void deportista_inexistente_falla() {
        TenantContext.set(2);
        try {
            assertThrows(IllegalArgumentException.class, () -> servicio("secreto", "Activo").carnet(99));
        } finally {
            TenantContext.clear();
        }
    }
}
