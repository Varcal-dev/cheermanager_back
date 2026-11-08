package com.varcal.cheermanager.Service.Persona;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.HabilidadCatalogo;
import com.varcal.cheermanager.Models.Personas.FichaDeportiva;
import com.varcal.cheermanager.Models.Personas.FichaHabilidadDeclarada;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.HabilidadCatalogoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.FichaDeportivaRepository;
import com.varcal.cheermanager.repository.Personas.FichaHabilidadDeclaradaRepository;

/**
 * Ficha deportiva: experiencia previa (con el nivel y las habilidades que el deportista dice tener) y tallas de
 * uniforme. Lo declarado orienta la ubicacion inicial; lo que vale es lo que se valida despues con la evaluacion (C1).
 */
@Service
public class FichaDeportivaService {

    static final List<String> NIVELES = List.of("N1", "N2", "N3", "N4");
    static final int MAX_ANIOS = 40;
    static final int MAX_TALLA = 10;

    private final FichaDeportivaRepository fichas;
    private final FichaHabilidadDeclaradaRepository declaradas;
    private final HabilidadCatalogoRepository catalogo;
    private final DeportistaRepository deportistas;

    public FichaDeportivaService(FichaDeportivaRepository fichas, FichaHabilidadDeclaradaRepository declaradas,
            HabilidadCatalogoRepository catalogo, DeportistaRepository deportistas) {
        this.fichas = fichas;
        this.declaradas = declaradas;
        this.catalogo = catalogo;
        this.deportistas = deportistas;
    }

    public record Tallas(String uniforme, String faldaShort, String camiseta, String buzo, String calzado) {
    }

    public record Solicitud(Boolean tieneExperiencia, Integer aniosExperiencia, String nivelDeclarado,
            List<Integer> habilidadIds, Tallas tallas) {
    }

    public record Vista(Boolean tieneExperiencia, Integer aniosExperiencia, String nivelDeclarado,
            List<HabilidadCatalogo> habilidades, String nivelSugerido, Tallas tallas) {
    }

    /** Habilidades activas del glosario, para el formulario de registro. */
    public List<HabilidadCatalogo> catalogo() {
        return catalogo.findByActivoTrueOrderById();
    }

    public Vista obtener(Integer deportistaId) {
        existe(deportistaId);
        FichaDeportiva f = fichas.findByDeportistaId(deportistaId).orElse(null);
        List<Integer> ids = declaradas.findByDeportistaId(deportistaId).stream()
                .map(FichaHabilidadDeclarada::getHabilidadId).toList();
        Map<Integer, HabilidadCatalogo> porId = catalogo.findByActivoTrueOrderById().stream()
                .collect(Collectors.toMap(HabilidadCatalogo::getId, Function.identity()));
        List<HabilidadCatalogo> habs = ids.stream().map(porId::get).filter(h -> h != null).toList();
        if (f == null) {
            return new Vista(null, null, null, habs, nivelSugerido(habs), new Tallas(null, null, null, null, null));
        }
        return new Vista(f.getTieneExperiencia(), f.getAniosExperiencia(), f.getNivelDeclarado(), habs,
                nivelSugerido(habs), new Tallas(f.getTallaUniforme(), f.getTallaFaldaShort(), f.getTallaCamiseta(),
                        f.getTallaBuzo(), f.getTallaCalzado()));
    }

    @Transactional
    public Vista guardar(Integer deportistaId, Solicitud s) {
        existe(deportistaId);
        if (s == null) {
            throw new IllegalArgumentException("Faltan los datos de la ficha.");
        }
        Boolean tiene = s.tieneExperiencia();
        Set<Integer> ids = new LinkedHashSet<>(s.habilidadIds() == null ? List.of() : s.habilidadIds());
        String nivel = s.nivelDeclarado() == null || s.nivelDeclarado().isBlank() ? null
                : s.nivelDeclarado().trim().toUpperCase();
        Integer anios = s.aniosExperiencia();

        if (!Boolean.TRUE.equals(tiene)) {
            // Sin experiencia (o sin responder): no hay nivel, años ni habilidades que guardar.
            if (Boolean.FALSE.equals(tiene)) {
                nivel = null;
                anios = null;
                ids.clear();
            } else if (nivel != null || anios != null || !ids.isEmpty()) {
                throw new IllegalArgumentException("Indique si el deportista tiene experiencia.");
            }
        } else {
            if (nivel != null && !NIVELES.contains(nivel)) {
                throw new IllegalArgumentException("El nivel debe ser N1, N2, N3 o N4.");
            }
            if (anios != null && (anios < 0 || anios > MAX_ANIOS)) {
                throw new IllegalArgumentException("Los años de experiencia deben estar entre 0 y " + MAX_ANIOS + ".");
            }
        }
        Set<Integer> validas = catalogo.findByActivoTrueOrderById().stream().map(HabilidadCatalogo::getId)
                .collect(Collectors.toSet());
        for (Integer id : ids) {
            if (id == null || !validas.contains(id)) {
                throw new IllegalArgumentException("Hay una habilidad que no existe en el glosario.");
            }
        }
        Tallas t = s.tallas() == null ? new Tallas(null, null, null, null, null) : s.tallas();

        FichaDeportiva f = fichas.findByDeportistaId(deportistaId).orElseGet(() -> {
            FichaDeportiva nueva = new FichaDeportiva();
            nueva.setDeportistaId(deportistaId);
            return nueva;
        });
        f.setTieneExperiencia(tiene);
        f.setAniosExperiencia(anios);
        f.setNivelDeclarado(nivel);
        f.setTallaUniforme(talla(t.uniforme()));
        f.setTallaFaldaShort(talla(t.faldaShort()));
        f.setTallaCamiseta(talla(t.camiseta()));
        f.setTallaBuzo(talla(t.buzo()));
        f.setTallaCalzado(talla(t.calzado()));
        fichas.save(f);

        List<FichaHabilidadDeclarada> previas = declaradas.findByDeportistaId(deportistaId);
        Set<Integer> previasIds = previas.stream().map(FichaHabilidadDeclarada::getHabilidadId)
                .collect(Collectors.toSet());
        List<FichaHabilidadDeclarada> sobran = previas.stream().filter(p -> !ids.contains(p.getHabilidadId()))
                .toList();
        if (!sobran.isEmpty()) {
            declaradas.deleteAll(sobran);
        }
        List<FichaHabilidadDeclarada> nuevas = new ArrayList<>();
        for (Integer id : ids) {
            if (!previasIds.contains(id)) {
                FichaHabilidadDeclarada d = new FichaHabilidadDeclarada();
                d.setDeportistaId(deportistaId);
                d.setHabilidadId(id);
                nuevas.add(d);
            }
        }
        if (!nuevas.isEmpty()) {
            declaradas.saveAll(nuevas);
        }
        return obtener(deportistaId);
    }

    private void existe(Integer deportistaId) {
        if (deportistaId == null || deportistas.findById(deportistaId).isEmpty()) {
            throw new IllegalArgumentException("Deportista no encontrado.");
        }
    }

    private static String talla(String v) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim();
        if (t.length() > MAX_TALLA) {
            throw new IllegalArgumentException("Cada talla admite hasta " + MAX_TALLA + " caracteres.");
        }
        return t.toUpperCase();
    }

    /** Nivel mas alto entre las habilidades declaradas (orientativo para quien ubica al deportista). */
    static String nivelSugerido(List<HabilidadCatalogo> habs) {
        return habs.stream().map(HabilidadCatalogo::getNivel).filter(NIVELES::contains)
                .max((a, b) -> Integer.compare(NIVELES.indexOf(a), NIVELES.indexOf(b))).orElse(null);
    }
}
