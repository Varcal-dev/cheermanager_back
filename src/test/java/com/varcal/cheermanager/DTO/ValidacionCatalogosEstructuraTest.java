package com.varcal.cheermanager.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Ath.CrearRolConPermisosDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.AsignarHorarioGrupoDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.HorarioDTO;
import com.varcal.cheermanager.DTO.Org_dep.GrupoEntrenamientoDTO;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionDto;
import com.varcal.cheermanager.DTO.Persona.AgregarDeportistaRequest;
import com.varcal.cheermanager.DTO.Persona.AsignacionEntrenadorDTO;
import com.varcal.cheermanager.Models.Auth.Permiso;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Org_dep.Division;
import com.varcal.cheermanager.Models.Org_dep.EstadoCategoriaNivel;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.Models.Org_dep.TipoGrupo;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** SEC-02 fase 4: catalogos (entidades), estructura del club e inscripciones. */
class ValidacionCatalogosEstructuraTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static Set<String> campos(Object o) {
        Set<String> r = new TreeSet<>();
        validador.validate(o).forEach(v -> r.add(v.getPropertyPath().toString()));
        return r;
    }

    // ---------- Catalogos (entidades) ----------

    @Test
    void rol_exige_nombre_y_limita_largos() {
        Rol r = new Rol();
        assertEquals(Set.of("nombre"), campos(r));
        r.setNombre("   ");
        assertEquals(Set.of("nombre"), campos(r));
        r.setNombre("Entrenador");
        assertTrue(campos(r).isEmpty()); // descripcion es opcional
        r.setNombre("n".repeat(256));
        r.setDescripcion("d".repeat(256));
        assertEquals(Set.of("nombre", "descripcion"), campos(r));
    }

    @Test
    void catalogos_simples_exigen_nombre_de_hasta_255() {
        assertEquals(Set.of("nombre"), campos(new Permiso()));
        assertEquals(Set.of("nombre"), campos(new Nivel()));
        assertEquals(Set.of("nombre"), campos(new Division()));
        assertEquals(Set.of("nombre"), campos(new TipoGrupo()));
        assertEquals(Set.of("nombre"), campos(new EstadoCategoriaNivel()));

        Nivel n = new Nivel();
        n.setNombre("Nivel 1");
        assertTrue(campos(n).isEmpty());
        n.setNombre("x".repeat(256));
        assertEquals(Set.of("nombre"), campos(n));

        Division d = new Division();
        d.setNombre("Tiny");
        assertTrue(campos(d).isEmpty());

        Permiso p = new Permiso();
        p.setNombre("ver_roles");
        assertTrue(campos(p).isEmpty());

        TipoGrupo t = new TipoGrupo();
        t.setNombre("Competitivo");
        assertTrue(campos(t).isEmpty());

        EstadoCategoriaNivel e = new EstadoCategoriaNivel();
        e.setNombre("Activa");
        assertTrue(campos(e).isEmpty());
    }

    @Test
    void asignar_permisos_exige_rol_y_lista_pero_acepta_lista_vacia() {
        CrearRolConPermisosDTO d = new CrearRolConPermisosDTO();
        assertEquals(Set.of("rolId", "permisoIds"), campos(d));
        d.setRolId(3);
        d.setPermisoIds(List.of()); // quitar todos los permisos es valido
        assertTrue(campos(d).isEmpty());
    }

    // ---------- Grupos, horarios y asignaciones ----------

    @Test
    void grupo_exige_nombre_tipo_y_categoria() {
        GrupoEntrenamientoDTO g = new GrupoEntrenamientoDTO(null, null, null);
        assertEquals(Set.of("nombre", "tipoGrupoId", "categoriaNivelId"), campos(g));
        assertTrue(campos(new GrupoEntrenamientoDTO("Ratels Prep", 1, 2)).isEmpty());
        assertEquals(Set.of("nombre"), campos(new GrupoEntrenamientoDTO("g".repeat(256), 1, 2)));
        assertEquals(Set.of("nombre"), campos(new GrupoEntrenamientoDTO("  ", 1, 2)));
    }

    @Test
    void horario_exige_dia_pero_las_horas_siguen_opcionales() {
        HorarioDTO h = new HorarioDTO();
        assertEquals(Set.of("diaId"), campos(h));
        h.setDiaId(1);
        assertTrue(campos(h).isEmpty());
    }

    @Test
    void asignar_horario_exige_ambos_ids() {
        AsignarHorarioGrupoDTO a = new AsignarHorarioGrupoDTO();
        assertEquals(Set.of("horarioId", "grupoEntrenamientoId"), campos(a));
    }

    @Test
    void agregar_deportista_exige_deportista() {
        AgregarDeportistaRequest r = new AgregarDeportistaRequest();
        assertEquals(Set.of("deportistaId"), campos(r));
        r.setDeportistaId(9);
        assertTrue(campos(r).isEmpty()); // observaciones opcional
    }

    @Test
    void asignar_entrenador_no_exige_grupo_porque_el_controlador_lo_pone_desde_la_ruta() {
        AsignacionEntrenadorDTO a = new AsignacionEntrenadorDTO();
        assertEquals(Set.of("entrenadorId", "fechaInicio"), campos(a));
        a.setEntrenadorId(4);
        a.setFechaInicio("2026-11-23");
        assertTrue(campos(a).isEmpty()); // sin grupoId: lo asigna el controlador
    }

    // ---------- Inscripcion ----------

    @Test
    void inscripcion_alta_exige_deportista_plan_fecha_y_estado() {
        InscripcionDto d = new InscripcionDto();
        assertEquals(Set.of("deportistaId", "planPagoId", "fechaInscripcion", "estado"), campos(d));
        d.setDeportistaId(1);
        d.setPlanPagoId(2);
        d.setFechaInscripcion(LocalDate.of(2026, 11, 23));
        d.setEstado("Activa");
        assertTrue(campos(d).isEmpty()); // fechaVencimiento, temporada y plan de mensualidad son opcionales
        d.setEstado(" ");
        assertEquals(Set.of("estado"), campos(d));
    }
}
