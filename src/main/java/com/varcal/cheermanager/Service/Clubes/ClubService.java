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
import com.varcal.cheermanager.repository.Auth.PermisoRepository;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.Models.Org_dep.Division;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Org_dep.DivisionRepository;
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

    public ClubService(ClubRepository clubRepository, RolRepository rolRepository,
            PermisoRepository permisoRepository, UserRepository userRepository,
            TenantLookupRepository lookup, PasswordEncoder passwordEncoder,
            PlatformTransactionManager txManager, NivelRepository nivelRepository,
            DivisionRepository divisionRepository) {
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
                .collect(Collectors.toCollection(HashSet::new));
        Set<Permiso> deEntrenador = RolesPlantilla.ENTRENADOR_PERMISOS.stream()
                .map(permisos::get).filter(p -> p != null)
                .collect(Collectors.toCollection(HashSet::new));

        Rol admin = nuevoRol(RolesPlantilla.ADMIN, "Administrador del club", deAdmin);
        nuevoRol(RolesPlantilla.ENTRENADOR, "Entrenador", deEntrenador);
        nuevoRol(RolesPlantilla.DEPORTISTA, "Deportista", new HashSet<>());

        Usuario u = new Usuario();
        u.setUsername(req.adminUsername().trim());
        u.setEmail(req.adminEmail().trim());
        u.setPasswordHash(passwordEncoder.encode(req.adminPassword()));
        u.setRol(admin);
        u.setRequiereCambioPassword(true);
        userRepository.save(u);

        // Plantilla inicial EDITABLE (por defecto si): evita que el club arranque con pantallas vacias.
        // Son nombres genericos de ejemplo; cada club los renombra o borra segun su federacion/liga.
        if (!Boolean.FALSE.equals(req.cargarPlantilla())) {
            for (int i = 1; i <= 6; i++) {
                Nivel n = new Nivel();
                n.setNombre("Nivel " + i);
                nivelRepository.save(n);
            }
            for (String nombre : List.of("Tiny", "Mini", "Youth", "Junior", "Senior")) {
                Division d = new Division();
                d.setNombre(nombre);
                divisionRepository.save(d);
            }
        }
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
