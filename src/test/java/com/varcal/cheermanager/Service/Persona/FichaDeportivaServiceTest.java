package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.HabilidadCatalogo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.FichaDeportiva;
import com.varcal.cheermanager.Models.Personas.FichaHabilidadDeclarada;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.HabilidadCatalogoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.FichaDeportivaRepository;
import com.varcal.cheermanager.repository.Personas.FichaHabilidadDeclaradaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** Ficha deportiva: experiencia declarada (nivel + habilidades) y tallas. */
class FichaDeportivaServiceTest {

    List<FichaDeportiva> fichas;
    List<FichaHabilidadDeclarada> decl;

    static HabilidadCatalogo hab(int id, String nivel) {
        HabilidadCatalogo h = new HabilidadCatalogo();
        h.setId(id);
        h.setNivel(nivel);
        h.setCategoria("GIMNASIA_ESTATICA");
        h.setNombre("H" + id);
        h.setActivo(true);
        return h;
    }

    FichaDeportivaService servicio() {
        fichas = new ArrayList<>();
        decl = new ArrayList<>();
        List<HabilidadCatalogo> cat = List.of(hab(1, "N1"), hab(2, "N1"), hab(3, "N2"), hab(4, "N3"));
        Deportista d = new Deportista();
        d.setId(7);
        Repo deps = new Repo().on("findById", a -> Optional.ofNullable(((Integer) a[0]) == 7 ? d : null));
        Repo cats = new Repo().on("findByActivoTrueOrderById", a -> cat);
        Repo fs = new Repo()
                .on("findByDeportistaId", a -> fichas.stream().findFirst())
                .on("save", a -> { FichaDeportiva f = (FichaDeportiva) a[0]; if (!fichas.contains(f)) fichas.add(f); return f; });
        Repo ds = new Repo()
                .on("findByDeportistaId", a -> new ArrayList<>(decl))
                .on("deleteAll", a -> { decl.removeAll((List<?>) a[0]); return null; })
                .on("saveAll", a -> { decl.addAll((List<FichaHabilidadDeclarada>) a[0]); return a[0]; });
        return new FichaDeportivaService(Fakes.proxy(FichaDeportivaRepository.class, fs),
                Fakes.proxy(FichaHabilidadDeclaradaRepository.class, ds), Fakes.proxy(HabilidadCatalogoRepository.class, cats),
                Fakes.proxy(DeportistaRepository.class, deps));
    }

    static FichaDeportivaService.Tallas tallas() {
        return new FichaDeportivaService.Tallas(" ys ", null, "m", "", "34");
    }

    @Test
    void con_experiencia_guarda_nivel_anios_habilidades_y_tallas() {
        FichaDeportivaService s = servicio();
        FichaDeportivaService.Vista v = s.guardar(7, new FichaDeportivaService.Solicitud(true, 3, "n2", List.of(1, 3), tallas()));
        assertTrue(v.tieneExperiencia());
        assertEquals(3, v.aniosExperiencia());
        assertEquals("N2", v.nivelDeclarado());
        assertEquals(2, v.habilidades().size());
        assertEquals("N2", v.nivelSugerido());
        assertEquals("YS", v.tallas().uniforme());
        assertEquals("M", v.tallas().camiseta());
        assertNull(v.tallas().buzo());
        assertEquals("34", v.tallas().calzado());
    }

    @Test
    void sin_experiencia_borra_nivel_anios_y_habilidades_pero_conserva_tallas() {
        FichaDeportivaService s = servicio();
        s.guardar(7, new FichaDeportivaService.Solicitud(true, 3, "N2", List.of(1, 3), tallas()));
        FichaDeportivaService.Vista v = s.guardar(7, new FichaDeportivaService.Solicitud(false, 5, "N4", List.of(1), tallas()));
        assertFalse(v.tieneExperiencia());
        assertNull(v.aniosExperiencia());
        assertNull(v.nivelDeclarado());
        assertTrue(v.habilidades().isEmpty());
        assertTrue(decl.isEmpty());
        assertEquals("M", v.tallas().camiseta());
    }

    @Test
    void reemplazar_habilidades_quita_las_que_sobran_y_agrega_las_nuevas_sin_duplicar() {
        FichaDeportivaService s = servicio();
        s.guardar(7, new FichaDeportivaService.Solicitud(true, null, null, List.of(1, 2), null));
        FichaDeportivaService.Vista v = s.guardar(7, new FichaDeportivaService.Solicitud(true, null, null, List.of(2, 4, 4), null));
        assertEquals(2, decl.size());
        assertEquals(List.of(2, 4), decl.stream().map(FichaHabilidadDeclarada::getHabilidadId).sorted().toList());
        assertEquals("N3", v.nivelSugerido());
        assertEquals(1, fichas.size());
    }

    @Test
    void sin_responder_no_admite_nivel_ni_habilidades_pero_si_solo_tallas() {
        FichaDeportivaService s = servicio();
        assertThrows(IllegalArgumentException.class,
                () -> s.guardar(7, new FichaDeportivaService.Solicitud(null, null, "N1", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio().guardar(7, new FichaDeportivaService.Solicitud(null, null, null, List.of(1), null)));
        FichaDeportivaService.Vista v = servicio().guardar(7, new FichaDeportivaService.Solicitud(null, null, null, null, tallas()));
        assertNull(v.tieneExperiencia());
        assertEquals("YS", v.tallas().uniforme());
    }

    @Test
    void validaciones_de_nivel_anios_habilidad_talla_y_deportista() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio().guardar(7, new FichaDeportivaService.Solicitud(true, null, "N5", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio().guardar(7, new FichaDeportivaService.Solicitud(true, 41, null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio().guardar(7, new FichaDeportivaService.Solicitud(true, -1, null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio().guardar(7, new FichaDeportivaService.Solicitud(true, null, null, List.of(99), null)));
        assertThrows(IllegalArgumentException.class, () -> servicio().guardar(7, new FichaDeportivaService.Solicitud(true, null, null, null,
                new FichaDeportivaService.Tallas("12345678901", null, null, null, null))));
        assertThrows(IllegalArgumentException.class,
                () -> servicio().guardar(99, new FichaDeportivaService.Solicitud(true, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> servicio().guardar(7, null));
        assertThrows(IllegalArgumentException.class, () -> servicio().obtener(99));
    }

    @Test
    void deportista_sin_ficha_devuelve_vista_vacia() {
        FichaDeportivaService.Vista v = servicio().obtener(7);
        assertNull(v.tieneExperiencia());
        assertTrue(v.habilidades().isEmpty());
        assertNull(v.nivelSugerido());
        assertEquals(4, servicio().catalogo().size());
    }
}
