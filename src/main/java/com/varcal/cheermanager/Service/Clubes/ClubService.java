package com.varcal.cheermanager.Service.Clubes;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.varcal.cheermanager.Models.Auth.Permiso;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.PlanSaas;
import com.varcal.cheermanager.repository.Auth.PermisoRepository;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.Models.Org_dep.CategoriaNivel;
import com.varcal.cheermanager.Models.Org_dep.Division;
import com.varcal.cheermanager.Models.Org_dep.EstadoCategoriaNivel;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;
import com.varcal.cheermanager.repository.Clubes.PlanSaasRepository;
import com.varcal.cheermanager.repository.Org_dep.CategoriaNivelRepository;
import com.varcal.cheermanager.repository.Org_dep.DivisionRepository;
import com.varcal.cheermanager.repository.Org_dep.EstadoCategoriaNivelRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;

/**
 * Alta y administracion de clubes (solo superadmin de plataforma).
 *
 * crearClub NO es @Transactional a proposito: el club se guarda primero (tabla global) y luego
 * los roles y el admin se siembran en una transaccion NUEVA con el tenant del club nuevo, porque
 * la sesion de Hibernate queda atada al tenant con el que se abrio.
 */
@Service
public class ClubService {

    public record CrearClubRequest(String nombre, String slug, String adminUsername, String adminEmail,
            String adminPassword, Boolean cargarPlantilla) {
    }

    private final ClubRepository clubRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UserRepository userRepository;
    private final TenantLookupRepository lookup;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate txNueva;
    private final NivelRepository nivelRepository;
    private final DivisionRepository divisionRepository;
    private final PlanSaasRepository planSaasRepository;
    private final TipoPlanPagoRepository tipoPlanPagoRepository;
    private final CategoriaNivelRepository categoriaNivelRepository;
    private final EstadoCategoriaNivelRepository estadoCategoriaNivelRepository;

    public ClubService(ClubRepository clubRepository, RolRepository rolRepository,
            PermisoRepository permisoRepository, UserRepository userRepository,
            TenantLookupRepository lookup, PasswordEncoder passwordEncoder,
            PlatformTransactionManager txManager, NivelRepository nivelRepository,
            DivisionRepository divisionRepository, PlanSaasRepository planSaasRepository,
            TipoPlanPagoRepository tipoPlanPagoRepository, CategoriaNivelRepository categoriaNivelRepository,
            EstadoCategoriaNivelRepository estadoCategoriaNivelRepository) {
        this.tipoPlanPagoRepository = tipoPlanPagoRepository;
        this.categoriaNivelRepository = categoriaNivelRepository;
        this.estadoCategoriaNivelRepository = estadoCategoriaNivelRepository;
        this.planSaasRepository = planSaasRepository;
        this.nivelRepository = nivelRepository;
        this.divisionRepository = divisionRepository;
        this.clubRepository = clubRepository;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.userRepository = userRepository;
        this.lookup = lookup;
        this.passwordEncoder = passwordEncoder;
        this.txNueva = new TransactionTemplate(txManager);
        this.txNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Club crearClub(CrearClubRequest req) {
        if (req.nombre() == null || req.nombre().isBlank()) {
            throw new RuntimeException("El nombre del club es obligatorio");
        }
        if (req.adminUsername() == null || req.adminUsername().isBlank()
                || req.adminEmail() == null || req.adminEmail().isBlank()) {
            throw new RuntimeException("Usuario y correo del administrador del club son obligatorios");
        }
        if (req.adminPassword() == null || req.adminPassword().length() < 10) {
            throw new RuntimeException("La clave inicial del administrador debe tener al menos 10 caracteres");
        }
        String slug = (req.slug() == null || req.slug().isBlank()) ? slugify(req.nombre()) : slugify(req.slug());
        if (slug.isEmpty()) {
            throw new RuntimeException("No se pudo generar un identificador (slug) valido para el club");
        }
        if (clubRepository.existsBySlug(slug)) {
            throw new RuntimeException("Ya existe un club con el identificador '" + slug + "'");
        }
        if (lookup.existeUsuarioEnAlgunClub(req.adminUsername().trim(), req.adminEmail().trim())) {
            throw new RuntimeException("El usuario o el correo del administrador ya estan en uso");
        }

        Club club = new Club();
        club.setNombre(req.nombre().trim());
        club.setSlug(slug);
        // Suscripcion: todo club nuevo empieza en prueba (Club.DIAS_PRUEBA dias) con el plan "prueba": todas las funciones
        // y el cupo de Start (V41). Si ese plan aun no existe, cae a Start. El superadmin puede cambiar plan, vigencia y
        // limites desde la pantalla de Clubes.
        club.setPlanId(planSaasRepository.findByCodigo("prueba").or(() -> planSaasRepository.findByCodigo("start"))
                .map(PlanSaas::getId).orElse(null));
        club = clubRepository.save(club);

        final Integer clubId = club.getId();
        try {
            TenantContext.runAs(clubId, () -> txNueva.executeWithoutResult(status -> sembrar(req)));
        } catch (RuntimeException e) {
            // Compensacion: si falla la siembra (rollback de roles/usuario) no dejamos un club vacio.
            clubRepository.deleteById(clubId);
            throw e;
        }
        return club;
    }

    /** Roles plantilla + administrador del club. Corre con el tenant del club NUEVO. */
    private void sembrar(CrearClubRequest req) {
        Map<String, Permiso> permisos = permisoRepository.findAll().stream()
                .collect(Collectors.toMap(Permiso::getNombre, Function.identity(), (a, b) -> a));

        Set<Permiso> deAdmin = permisos.values().stream()
                .filter(p -> !RolesPlantilla.SOLO_PLATAFORMA.contains(p.getNombre()))
                .filter(p -> !RolesPlantilla.PERMISO_PORTAL.equals(p.getNombre()))
                .collect(Collectors.toCollection(HashSet::new));
        Set<Permiso> deEntrenador = RolesPlantilla.ENTRENADOR_PERMISOS.stream()
                .map(permisos::get).filter(p -> p != null)
                .collect(Collectors.toCollection(HashSet::new));

        Rol admin = nuevoRol(RolesPlantilla.ADMIN, "Administrador del club", deAdmin);
        nuevoRol(RolesPlantilla.ENTRENADOR, "Entrenador", deEntrenador);
        // Roles operativos de plantilla (mismos nombres y permisos que en Ratels). Un permiso que no exista
        // en el catalogo se omite en vez de romper el alta (RolesPlantillaTest vigila que no pase).
        for (RolesPlantilla.RolBase base : RolesPlantilla.ROLES_ADICIONALES) {
            Set<Permiso> deRol = base.permisos().stream()
                    .map(permisos::get).filter(p -> p != null)
                    .collect(Collectors.toCollection(HashSet::new));
            nuevoRol(base.nombre(), base.descripcion(), deRol);
        }
        Set<Permiso> deportistaPortal = new HashSet<>();
        Permiso portal = permisos.get(RolesPlantilla.PERMISO_PORTAL);
        if (portal != null) {
            deportistaPortal.add(portal);
        }
        nuevoRol(RolesPlantilla.DEPORTISTA, "Deportista", new HashSet<>(deportistaPortal));
        nuevoRol(RolesPlantilla.ACUDIENTE, "Padre, madre o tutor (portal de solo lectura)", deportistaPortal);

        Usuario u = new Usuario();
        u.setUsername(req.adminUsername().trim());
        u.setEmail(req.adminEmail().trim());
        u.setPasswordHash(passwordEncoder.encode(req.adminPassword()));
        u.setRol(admin);
        u.setRequiereCambioPassword(true);
        userRepository.save(u);

        // Tipos de plan de pago base (por club): sin ellos no se puede crear ningun plan de inscripcion/mensualidad.
        tipoPlan("Inscripción anual", TipoPlanPago.Categoria.inscripcion, 12, "Cobro único al inscribir al deportista en la temporada");
        tipoPlan("Mensualidad", TipoPlanPago.Categoria.mensualidad, 1, "Cuota que se factura cada mes");
        tipoPlan("Mensualidad trimestral", TipoPlanPago.Categoria.mensualidad, 3, "Cuota que cubre tres meses");
        tipoPlan("Mensualidad semestral", TipoPlanPago.Categoria.mensualidad, 6, "Cuota que cubre seis meses");
        tipoPlan("Mensualidad anual", TipoPlanPago.Categoria.mensualidad, 12, "Cuota que cubre doce meses");

        // Plantilla inicial EDITABLE (por defecto si): evita que el club arranque con pantallas vacias.
        // Son nombres genericos de ejemplo; cada club los renombra o borra segun su federacion/liga.
        if (!Boolean.FALSE.equals(req.cargarPlantilla())) {
            List<Nivel> niveles = new java.util.ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                Nivel n = new Nivel();
                n.setNombre("Nivel " + i);
                niveles.add(nivelRepository.save(n));
            }
            List<Division> divisiones = new java.util.ArrayList<>();
            for (String nombre : List.of("Tiny", "Mini", "Youth", "Junior", "Senior")) {
                Division d = new Division();
                d.setNombre(nombre);
                divisiones.add(divisionRepository.save(d));
            }
            // Categorias por nivel (nivel x division, sin rango de edad ni cupo: sin limite). Son indispensables para
            // crear grupos y no hay pantalla para crearlas; sin ellas un club nuevo no podia armar ni un grupo.
            EstadoCategoriaNivel activa = estadoCategoriaNivelRepository.findAll().stream()
                    .filter(e -> "Activa".equalsIgnoreCase(e.getNombre())).findFirst().orElse(null);
            if (activa != null) {
                for (Nivel n : niveles) {
                    for (Division d : divisiones) {
                        CategoriaNivel c = new CategoriaNivel();
                        c.setNombre(n.getNombre() + " - " + d.getNombre());
                        c.setNivel(n);
                        c.setDivision(d);
                        c.setEstadoCategoriaNivel(activa);
                        categoriaNivelRepository.save(c);
                    }
                }
            }
        }
    }

    private void tipoPlan(String nombre, TipoPlanPago.Categoria categoria, int meses, String descripcion) {
        TipoPlanPago t = new TipoPlanPago();
        t.setNombre(nombre);
        t.setCategoria(categoria);
        t.setDuracionMeses(meses);
        t.setDescripcion(descripcion);
        tipoPlanPagoRepository.save(t);
    }

    private Rol nuevoRol(String nombre, String descripcion, Set<Permiso> permisos) {
        Rol r = new Rol();
        r.setNombre(nombre);
        r.setDescripcion(descripcion);
        r.setPermisos(permisos);
        return rolRepository.save(r);
    }

    public List<Club> listar() {
        return clubRepository.findAll();
    }

    public Club obtener(Integer id) {
        return clubRepository.findById(id).orElseThrow(() -> new RuntimeException("Club no encontrado"));
    }

    public Club cambiarActivo(Integer id, boolean activo) {
        if (id == Club.PLATAFORMA_ID && !activo) {
            throw new RuntimeException("No se puede desactivar el club de la plataforma");
        }
        Club c = obtener(id);
        c.setActivo(activo);
        return clubRepository.save(c);
    }

    static String slugify(String texto) {
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}