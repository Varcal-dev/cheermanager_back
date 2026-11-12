package com.varcal.cheermanager.Service.Evaluaciones;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Evaluaciones.CriterioEvaluacion;
import com.varcal.cheermanager.Models.Evaluaciones.EvaluacionPlantilla;
import com.varcal.cheermanager.Models.Evaluaciones.EvaluacionPlantillaCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.TipoCategoria;
import com.varcal.cheermanager.repository.Evaluaciones.CriterioEvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionPlantillaCriterioRepository;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionPlantillaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.TipoCategoriaRepository;
import com.varcal.cheermanager.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Plantillas de evaluacion (tryout): un conjunto reutilizable de criterios tomados de la rubrica
 * (secciones/sub-criterios de Prep o Elite) y/o del catalogo de habilidades, mas criterios propios del club.
 *
 * No hay un sistema paralelo: al guardar una plantilla, cada item elegido se convierte (o se reutiliza) en un
 * criterio del club (criterios_evaluacion) y los puntajes se guardan en la tabla evaluaciones de siempre,
 * asi que Historial, Progresion y Resumen los ven sin cambios. Novice no aplica (no tiene rubrica de esta forma).
 */
@Service
public class EvaluacionPlantillaService {

    public static final Set<String> MODALIDADES = Set.of("Prep", "Elite");
    public static final int MAX_CRITERIOS = 40;

    public record ItemEntrada(String tipo, Integer ref) {
    }

    public record PlantillaEntrada(String nombre, String modalidad, String descripcion, List<ItemEntrada> items) {
    }

    public record CriterioDTO(Integer criterioId, String nombre, String categoria, int orden) {
    }

    public record PlantillaDTO(Integer id, String nombre, String tipo, String modalidad, String descripcion,
            List<CriterioDTO> criterios) {
    }

    public record RubricaItem(Integer id, String nombre, String seccion) {
    }

    public record HabilidadItem(Integer id, String codigo, String nivel, String categoria, String nombre) {
    }

    public record Catalogo(String modalidad, List<RubricaItem> rubrica, List<HabilidadItem> habilidades) {
    }

    public record Fila(Integer deportistaId, String nombre, Map<Integer, Integer> puntajes, Double promedio,
            int evaluados, int total) {
    }

    public record Resultados(Integer plantillaId, String nombre, LocalDate fecha, List<CriterioDTO> criterios,
            List<Fila> filas) {
    }

    @PersistenceContext
    private EntityManager em;
    @Autowired
    private EvaluacionPlantillaRepository plantillas;
    @Autowired
    private EvaluacionPlantillaCriterioRepository items;
    @Autowired
    private CriterioEvaluacionRepository criterios;
    @Autowired
    private TipoCategoriaRepository categorias;

    // ---------- Catalogo ----------

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Catalogo catalogo(String modalidad) {
        String mod = normalizarModalidad(modalidad);
        List<Object[]> r;
        if (mod != null) {
            // Sub-criterios que la rubrica de esa modalidad realmente usa (via sus niveles).
            r = em.createNativeQuery("SELECT DISTINCT s.id, s.nombre, s.seccion FROM sub_criterios_rubrica s "
                    + "JOIN tope_seccion_nivel t ON t.sub_criterio_id = s.id "
                    + "JOIN niveles_competencia n ON n.id = t.nivel_id "
                    + "WHERE n.modalidad = :m AND n.activo = 1 ORDER BY s.seccion, s.nombre")
                    .setParameter("m", mod).getResultList();
        } else {
            r = List.of();
        }
        if (mod != null && r.isEmpty()) {
            // Si el catalogo no relaciona secciones con niveles, se ofrecen todos los sub-criterios.
            r = em.createNativeQuery("SELECT id, nombre, seccion FROM sub_criterios_rubrica ORDER BY seccion, nombre")
                    .getResultList();
        }
        List<RubricaItem> rub = new ArrayList<>();
        for (Object[] o : r)
            rub.add(new RubricaItem(((Number) o[0]).intValue(), (String) o[1], (String) o[2]));

        List<Object[]> h = em.createNativeQuery(
                "SELECT id, codigo, nivel, categoria, nombre FROM habilidades_catalogo WHERE activo = 1 ORDER BY id")
                .getResultList();
        List<HabilidadItem> hab = new ArrayList<>();
        for (Object[] o : h)
            hab.add(new HabilidadItem(((Number) o[0]).intValue(), (String) o[1], (String) o[2], (String) o[3],
                    (String) o[4]));
        return new Catalogo(mod, rub, hab);
    }

    // ---------- CRUD ----------

    @Transactional(readOnly = true)
    public List<PlantillaDTO> listar() {
        List<PlantillaDTO> out = new ArrayList<>();
        for (EvaluacionPlantilla p : plantillas.findByActivaTrueOrderByNombreAsc())
            out.add(aDto(p));
        return out;
    }

    @Transactional(readOnly = true)
    public PlantillaDTO obtener(Integer id) {
        return aDto(buscar(id));
    }

    @Transactional
    public PlantillaDTO crear(PlantillaEntrada e) {
        String nombre = validarNombre(e.nombre());
        if (plantillas.existsByNombreIgnoreCase(nombre))
            throw new IllegalStateException("Ya existe una plantilla con ese nombre");
        EvaluacionPlantilla p = new EvaluacionPlantilla();
        p.setNombre(nombre);
        aplicar(p, e);
        p = plantillas.save(p);
        guardarItems(p, e);
        return aDto(p);
    }

    @Transactional
    public PlantillaDTO actualizar(Integer id, PlantillaEntrada e) {
        EvaluacionPlantilla p = buscar(id);
        String nombre = validarNombre(e.nombre());
        if (plantillas.existsByNombreIgnoreCaseAndIdNot(nombre, id))
            throw new IllegalStateException("Ya existe una plantilla con ese nombre");
        p.setNombre(nombre);
        aplicar(p, e);
        plantillas.save(p);
        items.deleteByPlantillaId(id);
        items.flush();
        guardarItems(p, e);
        return aDto(p);
    }

    /** Borra solo la plantilla: los criterios y las evaluaciones ya registradas se conservan. */
    @Transactional
    public void eliminar(Integer id) {
        EvaluacionPlantilla p = buscar(id);
        items.deleteByPlantillaId(id);
        plantillas.delete(p);
    }

    // ---------- Resultados ----------

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<LocalDate> fechas(Integer id) {
        buscar(id);
        Integer club = TenantContext.require();
        List<Object> r = em.createNativeQuery("SELECT DISTINCT e.fecha FROM evaluaciones e "
                + "JOIN evaluacion_plantilla_criterios pc ON pc.criterio_id = e.criterio_id AND pc.plantilla_id = :p "
                + "WHERE e.club_id = :club ORDER BY e.fecha DESC")
                .setParameter("p", id).setParameter("club", club).getResultList();
        List<LocalDate> out = new ArrayList<>();
        for (Object o : r)
            out.add(o instanceof Date d ? d.toLocalDate() : LocalDate.parse(o.toString()));
        return out;
    }

    /** Ranking de una fecha: promedio de los criterios evaluados de cada deportista y cuantos faltan. */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Resultados resultados(Integer id, LocalDate fecha) {
        if (fecha == null)
            throw new IllegalArgumentException("Indica la fecha");
        EvaluacionPlantilla p = buscar(id);
        Integer club = TenantContext.require();
        PlantillaDTO dto = aDto(p);
        int total = dto.criterios().size();
        List<Object[]> r = em.createNativeQuery("SELECT e.deportista_id, e.criterio_id, e.puntaje_obtenido, "
                + "CONCAT(per.nombre, ' ', per.apellidos) FROM evaluaciones e "
                + "JOIN evaluacion_plantilla_criterios pc ON pc.criterio_id = e.criterio_id AND pc.plantilla_id = :p "
                + "JOIN deportistas d ON d.id = e.deportista_id JOIN personas per ON per.id = d.persona_id "
                + "WHERE e.club_id = :club AND e.fecha = :f ORDER BY per.nombre, per.apellidos")
                .setParameter("p", id).setParameter("club", club).setParameter("f", Date.valueOf(fecha))
                .getResultList();
        Map<Integer, String> nombres = new LinkedHashMap<>();
        Map<Integer, Map<Integer, Integer>> pts = new LinkedHashMap<>();
        for (Object[] o : r) {
            Integer dep = ((Number) o[0]).intValue();
            nombres.putIfAbsent(dep, (String) o[3]);
            pts.computeIfAbsent(dep, k -> new LinkedHashMap<>()).put(((Number) o[1]).intValue(),
                    ((Number) o[2]).intValue());
        }
        List<Fila> filas = new ArrayList<>();
        for (var en : pts.entrySet()) {
            Map<Integer, Integer> m = en.getValue();
            double prom = m.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            filas.add(new Fila(en.getKey(), nombres.get(en.getKey()), m,
                    Math.round(prom * 100.0) / 100.0, m.size(), total));
        }
        filas.sort((a, b) -> Double.compare(b.promedio(), a.promedio()));
        return new Resultados(p.getId(), p.getNombre(), fecha, dto.criterios(), filas);
    }

    // ---------- Internos ----------

    private EvaluacionPlantilla buscar(Integer id) {
        return plantillas.findById(id).orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
    }

    private String validarNombre(String n) {
        if (n == null || n.isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        n = n.trim();
        if (n.length() > 120)
            throw new IllegalArgumentException("El nombre no puede pasar de 120 caracteres");
        return n;
    }

    private String normalizarModalidad(String m) {
        if (m == null || m.isBlank())
            return null;
        for (String ok : MODALIDADES)
            if (ok.equalsIgnoreCase(m.trim()))
                return ok;
        throw new IllegalArgumentException("La modalidad debe ser Prep o Elite (Novice no aplica)");
    }

    private void aplicar(EvaluacionPlantilla p, PlantillaEntrada e) {
        p.setModalidad(normalizarModalidad(e.modalidad()));
        String d = e.descripcion() == null ? null : e.descripcion().trim();
        if (d != null && d.length() > 300)
            throw new IllegalArgumentException("La descripcion no puede pasar de 300 caracteres");
        p.setDescripcion(d == null || d.isEmpty() ? null : d);
        p.setTipo("TRYOUT");
    }

    private void guardarItems(EvaluacionPlantilla p, PlantillaEntrada e) {
        if (e.items() == null || e.items().isEmpty())
            throw new IllegalArgumentException("Elige al menos un criterio");
        if (e.items().size() > MAX_CRITERIOS)
            throw new IllegalArgumentException("Maximo " + MAX_CRITERIOS + " criterios por plantilla");
        Set<Integer> usados = new LinkedHashSet<>();
        int orden = 0;
        for (ItemEntrada it : e.items()) {
            if (it == null || it.tipo() == null || it.ref() == null)
                throw new IllegalArgumentException("Criterio invalido");
            CriterioEvaluacion c = resolver(it, p.getModalidad());
            if (!usados.add(c.getId()))
                continue; // repetido: se ignora
            EvaluacionPlantillaCriterio pc = new EvaluacionPlantillaCriterio();
            pc.setPlantillaId(p.getId());
            pc.setCriterio(c);
            pc.setOrden(orden++);
            items.save(pc);
        }
    }

    private CriterioEvaluacion resolver(ItemEntrada it, String modalidad) {
        switch (it.tipo().toUpperCase()) {
            case "CLUB": {
                return criterios.findById(it.ref())
                        .orElseThrow(() -> new IllegalArgumentException("Criterio del club no encontrado"));
            }
            case "RUBRICA": {
                if (modalidad == null)
                    throw new IllegalArgumentException("Elige la modalidad (Prep o Elite) para usar la rubrica");
                Object nombre = unico("SELECT nombre FROM sub_criterios_rubrica WHERE id = :i", it.ref());
                if (nombre == null)
                    throw new IllegalArgumentException("Sub-criterio de rubrica no encontrado");
                return encontrarOCrear("Rúbrica " + modalidad, truncar((String) nombre));
            }
            case "HABILIDAD": {
                Object cod = unico("SELECT CONCAT(codigo, ' · ', nombre) FROM habilidades_catalogo WHERE id = :i",
                        it.ref());
                if (cod == null)
                    throw new IllegalArgumentException("Habilidad no encontrada");
                return encontrarOCrear("Habilidades", truncar((String) cod));
            }
            default:
                throw new IllegalArgumentException("Tipo de criterio invalido: " + it.tipo());
        }
    }

    @SuppressWarnings("unchecked")
    private Object unico(String sql, Integer id) {
        List<Object> r = em.createNativeQuery(sql).setParameter("i", id).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    private String truncar(String s) {
        return s.length() > 255 ? s.substring(0, 255) : s;
    }

    private CriterioEvaluacion encontrarOCrear(String categoria, String nombre) {
        TipoCategoria cat = categorias.findAll().stream()
                .filter(c -> c.getCategoria().equalsIgnoreCase(categoria)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta la categoria '" + categoria + "' (migracion V42)"));
        return criterios.findFirstByCategoriaIdAndNombreIgnoreCase(cat.getId(), nombre).orElseGet(() -> {
            CriterioEvaluacion c = new CriterioEvaluacion();
            c.setNombre(nombre);
            c.setCategoria(cat);
            return criterios.save(c);
        });
    }

    private PlantillaDTO aDto(EvaluacionPlantilla p) {
        List<CriterioDTO> cs = new ArrayList<>();
        for (EvaluacionPlantillaCriterio pc : items.findByPlantillaIdOrderByOrdenAsc(p.getId()))
            cs.add(new CriterioDTO(pc.getCriterio().getId(), pc.getCriterio().getNombre(),
                    pc.getCriterio().getCategoria().getCategoria(), pc.getOrden()));
        return new PlantillaDTO(p.getId(), p.getNombre(), p.getTipo(), p.getModalidad(), p.getDescripcion(), cs);
    }
}
