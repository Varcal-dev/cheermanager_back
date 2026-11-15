package com.varcal.cheermanager.Service.Auth;

import com.varcal.cheermanager.tenant.TenantContext;

import com.varcal.cheermanager.Service.Clubes.RolesPlantilla;

import com.varcal.cheermanager.Models.Clubes.Club;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.varcal.cheermanager.DTO.Ath.RolConConteoDTO;
import com.varcal.cheermanager.Models.Auth.Permiso;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Funcionalidad.Auditoria;
import com.varcal.cheermanager.Service.Funcionalidad.AuditoriaService;
import com.varcal.cheermanager.repository.Auth.PermisoRepository;
import com.varcal.cheermanager.repository.Auth.RolRepository;

@Service
public class RolService {
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private JerarquiaAccesoService jerarquia;

    /** Permisos sin los cuales el Administrador no podria volver a abrir la pantalla de roles para arreglarlo. */
    private static final Set<String> PERMISOS_DE_RESCATE = Set.of("ver_roles", "gestionar_roles", "asignar_permiso");

    public RolService(RolRepository rolRepository, PermisoRepository permisoRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
    }

    public List<Rol> listar() {
        return rolRepository.findAll();
    }

    public List<RolConConteoDTO> listarRolesConConteo() {
        List<Object[]> resultados = rolRepository.listarRolesConConteo();

        return resultados.stream()
                .map(obj -> new RolConConteoDTO(
                        ((Number) obj[0]).intValue(),
                        String.valueOf(obj[1]),
                        ((Number) obj[2]).intValue()))
                .collect(Collectors.toList());
    }

    public Rol getRolById(Integer id) {
        return rolRepository.findById(id).orElse(null);
    }

    public Rol createRol(Rol rol) {
        // El body llega como entidad: se ignoran id y permisos (los permisos se asignan SOLO por
        // assignPermissionsToRol, que valida; asi no se cuela un permiso de plataforma por el body).
        rol.setId(null);
        rol.setPermisos(new HashSet<>());
        return rolRepository.save(rol);
    }

    public Rol modificarRol(Integer id, Rol rolDetails) {
        return rolRepository.findById(id)
                .map(rol -> {
                    // Las migraciones y el techo de permisos reconocen al Administrador por nombre: renombrarlo
                    // dejaria a los permisos nuevos sin llegar a nadie.
                    if (JerarquiaAccesoService.ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombre())
                            && !JerarquiaAccesoService.ROL_ADMINISTRADOR.equalsIgnoreCase(rolDetails.getNombre())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "El rol Administrador no se puede renombrar.");
                    }
                    jerarquia.exigirPuedeOtorgarRoles(List.of(rol));
                    rol.setNombre(rolDetails.getNombre());
                    // Antes solo se actualizaba el nombre — "Editar Rol" en
                    // el front no tenía forma de guardar cambios en la
                    // descripción aunque el formulario la mostrara.
                    rol.setDescripcion(rolDetails.getDescripcion());
                    return rolRepository.save(rol);
                })
                .orElse(null);
    }

    public void deleteRol(Integer id) {
        Rol rol = rolRepository.findById(id).orElse(null);
        if (rol == null) {
            return;
        }
        if (JerarquiaAccesoService.ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol Administrador no se puede eliminar.");
        }
        jerarquia.exigirPuedeOtorgarRoles(List.of(rol));
        boolean enUso = rolRepository.listarRolesConConteo().stream()
                .anyMatch(f -> ((Number) f[0]).intValue() == id && ((Number) f[2]).intValue() > 0);
        if (enUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol tiene usuarios asignados: reasignalos a otro rol antes de eliminarlo.");
        }
        rolRepository.deleteById(id);
    }

    public Rol assignPermissionsToRol(Integer rolId, List<Integer> permisoIds) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        List<Permiso> permisos = permisoRepository.findAllById(permisoIds);

        // Escalada de privilegios: un admin de club NO puede darse permisos de plataforma
        // (gestionar_clubes, catalogos globales, rubrica global).
        if (TenantContext.get() != Club.PLATAFORMA_ID
                && permisos.stream().anyMatch(p -> RolesPlantilla.SOLO_PLATAFORMA.contains(p.getNombre()))) {
            throw new RuntimeException("Hay permisos reservados a la plataforma que no se pueden asignar a un rol de club");
        }

        // Techo de permisos: no se puede tocar un rol con mas poder que el propio, ni agregarle permisos que uno no
        // tiene (un rol personalizado con "asignar_permiso" no puede darse el resto del sistema).
        Set<String> antesNombres = nombresDePermisos(rol.getPermisos());
        Set<String> despuesNombres = permisos.stream().map(Permiso::getNombre).collect(Collectors.toSet());
        jerarquia.exigirPuedeOtorgarRoles(List.of(rol));
        Set<String> nuevos = new HashSet<>(despuesNombres);
        nuevos.removeAll(antesNombres);
        jerarquia.exigirPuedeOtorgarPermisos(nuevos);
        if (JerarquiaAccesoService.ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombre())
                && !despuesNombres.containsAll(PERMISOS_DE_RESCATE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El rol Administrador debe conservar los permisos de roles (" + String.join(", ", PERMISOS_DE_RESCATE)
                            + ") para poder corregir errores.");
        }

        // Los permisos son una relacion: el snapshot automatico de la auditoria no los ve. Se registra aparte
        // exactamente que permisos se dieron y cuales se quitaron (es el cambio mas sensible del sistema).
        Set<String> antes = nombresDePermisos(rol.getPermisos());

        rol.setPermisos(new HashSet<>(permisos));

        Rol guardado = rolRepository.save(rol);
        registrarCambioDePermisos(guardado, antes, nombresDePermisos(guardado.getPermisos()));
        return guardado;
    }

    private static Set<String> nombresDePermisos(Set<Permiso> permisos) {
        try {
            return permisos == null ? new java.util.TreeSet<>()
                    : permisos.stream().map(Permiso::getNombre).collect(Collectors.toCollection(java.util.TreeSet::new));
        } catch (RuntimeException e) {
            return new java.util.TreeSet<>(); // coleccion perezosa sin sesion: no se puede leer
        }
    }

    private void registrarCambioDePermisos(Rol rol, Set<String> antes, Set<String> despues) {
        try {
            Set<String> dados = new java.util.TreeSet<>(despues);
            dados.removeAll(antes);
            Set<String> quitados = new java.util.TreeSet<>(antes);
            quitados.removeAll(despues);
            if (dados.isEmpty() && quitados.isEmpty()) {
                return;
            }
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> anterior = new java.util.LinkedHashMap<>();
            anterior.put("rol", rol.getNombre());
            anterior.put("permisosQuitados", quitados);
            java.util.Map<String, Object> nuevo = new java.util.LinkedHashMap<>();
            nuevo.put("rol", rol.getNombre());
            nuevo.put("permisosAgregados", dados);
            nuevo.put("totalPermisos", despues.size());
            auditoriaService.registrarEvento("roles", Auditoria.AccionEnum.PERMISOS, rol.getId(),
                    quitados.isEmpty() ? null : om.writeValueAsString(anterior),
                    dados.isEmpty() ? null : om.writeValueAsString(nuevo));
        } catch (Exception ignored) {
            // best-effort: auditar jamas debe impedir guardar los permisos
        }
    }
}