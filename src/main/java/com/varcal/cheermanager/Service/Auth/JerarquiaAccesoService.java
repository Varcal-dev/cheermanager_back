package com.varcal.cheermanager.Service.Auth;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;

/**
 * Techo de permisos: nadie puede dar ni tocar mas de lo que tiene.
 *
 * Tener el permiso "modificar_usuario" o "asignar_permiso" no basta para volverse administrador: quien edita a un
 * usuario, le asigna un rol, le cambia la clave o le agrega permisos a un rol, solo puede hacerlo si TODO lo que ese
 * rol permite ya lo permite su propio rol. El rol Administrador es la raiz del club (las migraciones le dan todo por
 * nombre) y no pasa por el techo: el limite es para los roles personalizados que reciban esos permisos.
 * "acceder_portal" queda fuera de la cuenta: es del portal de familias, no un poder administrativo, y el Administrador
 * no lo tiene, pero debe poder asignar los roles Deportista y Acudiente.
 *
 * Ademas, nadie puede desactivar, eliminar o cambiar de rol su propia cuenta, ni dejar al club sin Administrador.
 * El equipo de la plataforma (club 1) no pasa por el techo: solo administra al propio equipo de la plataforma.
 */
@Service
public class JerarquiaAccesoService {

    public static final String ROL_ADMINISTRADOR = "Administrador";

    /** Permisos que no cuentan para el techo (no son poder administrativo). */
    static final Set<String> SIN_TECHO = Set.of("acceder_portal");

    private final UsuarioService usuarioService;
    private final UserRepository userRepository;

    public JerarquiaAccesoService(UsuarioService usuarioService, UserRepository userRepository) {
        this.usuarioService = usuarioService;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ puro (probado sin base de datos)

    /** Permisos de {@code objetivo} que {@code actor} no tiene (vacio = el actor puede otorgarlos todos). */
    public static Set<String> faltantes(Set<String> actor, Set<String> objetivo) {
        Set<String> r = new TreeSet<>(objetivo);
        r.removeAll(actor);
        r.removeAll(SIN_TECHO);
        return r;
    }

    public static Set<String> permisosDe(Rol rol) {
        if (rol == null || rol.getPermisos() == null) {
            return Set.of();
        }
        return rol.getPermisos().stream().map(p -> p.getNombre()).collect(Collectors.toSet());
    }

    /** true si alguno de los roles del usuario (el unico o los multiples) se llama Administrador. */
    public static boolean esAdministrador(Usuario u) {
        if (u.getRol() != null && ROL_ADMINISTRADOR.equalsIgnoreCase(u.getRol().getNombre())) {
            return true;
        }
        return u.getRoles() != null
                && u.getRoles().stream().anyMatch(r -> ROL_ADMINISTRADOR.equalsIgnoreCase(r.getNombre()));
    }

    private static ResponseStatusException prohibido(String mensaje) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, mensaje);
    }

    // ------------------------------------------------------------------ con sesion

    private boolean esPlataforma() {
        return TenantContext.get() == Club.PLATAFORMA_ID;
    }

    private Set<String> permisosDelActor() {
        return usuarioService.obtenerPermisos(AuthenticatedUserUtil.getCurrentUserId().intValue());
    }

    /** La plataforma y el Administrador del club no pasan por el techo. */
    private boolean exentoDelTecho() {
        if (esPlataforma()) {
            return true;
        }
        return userRepository.findById(AuthenticatedUserUtil.getCurrentUserId())
                .map(JerarquiaAccesoService::esAdministrador).orElse(false);
    }

    /** El actor solo puede asignar roles cuyos permisos ya tiene. */
    public void exigirPuedeOtorgarRoles(Collection<Rol> roles) {
        if (exentoDelTecho()) {
            return;
        }
        Set<String> actor = permisosDelActor();
        Set<String> pedidos = new HashSet<>();
        roles.forEach(r -> pedidos.addAll(permisosDe(r)));
        Set<String> falta = faltantes(actor, pedidos);
        if (!falta.isEmpty()) {
            throw prohibido("No puedes asignar un rol con permisos que tu propio rol no tiene (" + resumir(falta) + ").");
        }
    }

    /** El actor solo puede asignar a un rol permisos que ya tiene. */
    public void exigirPuedeOtorgarPermisos(Set<String> permisosNuevos) {
        if (exentoDelTecho()) {
            return;
        }
        Set<String> falta = faltantes(permisosDelActor(), permisosNuevos);
        if (!falta.isEmpty()) {
            throw prohibido("No puedes dar permisos que tu propio rol no tiene (" + resumir(falta) + ").");
        }
    }

    /** Editar, cambiar la clave o reasignar roles de alguien exige tener todo lo que ese usuario puede hacer. */
    public void exigirPuedeGestionar(Usuario objetivo) {
        if (exentoDelTecho()) {
            return;
        }
        Integer actorId = AuthenticatedUserUtil.getCurrentUserId().intValue();
        if (actorId.equals(objetivo.getId())) {
            return; // sobre si mismo: las reglas de "no tocarse" van aparte
        }
        Set<String> falta = faltantes(permisosDelActor(), usuarioService.obtenerPermisos(objetivo.getId()));
        if (!falta.isEmpty()) {
            throw prohibido("No puedes modificar a un usuario con mas permisos que tu (" + resumir(falta) + ").");
        }
    }

    /** Nadie desactiva, elimina ni cambia de rol su propia cuenta: lo hace otra persona con permiso. */
    public void exigirNoEsUnoMismo(Integer objetivoId, String accion) {
        if (AuthenticatedUserUtil.getCurrentUserId().intValue() == objetivoId) {
            throw prohibido("No puedes " + accion + " tu propia cuenta. Pidele a otro administrador que lo haga.");
        }
    }

    /** El club no puede quedarse sin ningun Administrador activo. */
    public void exigirNoDejarSinAdministrador(Usuario objetivo, String accion) {
        if (!esAdministrador(objetivo)) {
            return;
        }
        long otros = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(objetivo.getId()))
                .filter(JerarquiaAccesoService::esAdministrador)
                .count();
        if (otros == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No puedes " + accion + " al unico Administrador activo del club.");
        }
    }

    private static String resumir(Set<String> permisos) {
        String lista = permisos.stream().limit(4).collect(Collectors.joining(", "));
        return permisos.size() > 4 ? lista + " y " + (permisos.size() - 4) + " mas" : lista;
    }
}
