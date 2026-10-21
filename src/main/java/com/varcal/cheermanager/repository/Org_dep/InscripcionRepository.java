package com.varcal.cheermanager.repository.Org_dep;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Org_dep.Inscripcion;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {
    List<Inscripcion> findByDeportistaId(Long deportistaId);

    // Version con Integer (el id de Deportista es Integer): usada al retirar un deportista.
    List<Inscripcion> findByDeportistaIdAndEstado(Integer deportistaId, Inscripcion.EstadoInscripcion estado);


    // Usado por el proceso mensual de facturación: todas las inscripciones
    // activas que tengan un plan de mensualidad asignado (las que no lo
    // tienen simplemente no generan cobro recurrente).
    List<Inscripcion> findByEstadoAndPlanMensualidadIsNotNull(Inscripcion.EstadoInscripcion estado);

    @Query(value = """
            SELECT
                i.id                                 AS inscripcion_id,
                d.id                                  AS deportista_id,
                CONCAT(p.nombre, ' ', p.apellidos)    AS deportista,
                n.nombre                              AS nivel,
                i.fecha_inscripcion,
                i.fecha_vencimiento,
                i.estado,
                pp.nombre                              AS plan_pago,
                ge.nombre                              AS grupo_asignado,
                NULL                                    AS documentos_completos
            FROM inscripciones i
            JOIN deportistas d          ON d.id = i.deportista_id
            JOIN personas p             ON p.id = d.persona_id
            LEFT JOIN niveles n         ON n.id = d.nivel_actual_id AND n.club_id = d.club_id
            JOIN planes_pago pp         ON pp.id = i.plan_pago_id
            LEFT JOIN grupo_deportista gd    ON gd.deportista_id = d.id AND gd.fecha_fin IS NULL
            LEFT JOIN grupos_entrenamiento ge ON ge.id = gd.grupo_entrenamiento_id
            WHERE i.club_id = :#{T(com.varcal.cheermanager.tenant.TenantContext).require()}
            """, nativeQuery = true)
    List<Object[]> obtenerVistaInscripcionesDetalle();

    @Query(value = """
            SELECT
                d.id                                 AS deportista_id,
                CONCAT(p.nombre, ' ', p.apellidos)    AS nombre_completo,
                n.nombre                              AS nivel
            FROM deportistas d
            JOIN personas p     ON p.id = d.persona_id
            LEFT JOIN niveles n ON n.id = d.nivel_actual_id AND n.club_id = d.club_id
            WHERE d.club_id = :#{T(com.varcal.cheermanager.tenant.TenantContext).require()}
              AND NOT EXISTS (
                SELECT 1 FROM inscripciones i
                WHERE i.deportista_id = d.id AND i.estado = 'Activa'
            )
            """, nativeQuery = true)
    List<Object[]> obtenerVistaDeportistasNoInscritos();

    @Query(value = """
            SELECT
                d.id                    AS deportista_id,
                p.nombre,
                p.apellidos,
                p.telefono,
                p.fecha_nacimiento,
                gen.genero          AS genero,
                n.nombre                 AS nivel,
                i.fecha_inscripcion,
                i.fecha_vencimiento,
                i.estado                 AS estado_inscripcion,
                pp.nombre                AS plan_pago,
                tpp.nombre               AS tipo_plan,
                pp.valor_mensual,
                ep.estado                AS estado_deportista,
                ge.nombre                AS grupo_entrenamiento
            FROM inscripciones i
            JOIN deportistas d              ON d.id = i.deportista_id
            JOIN personas p                 ON p.id = d.persona_id
            LEFT JOIN generos gen           ON gen.id = p.genero_id
            LEFT JOIN niveles n             ON n.id = d.nivel_actual_id AND n.club_id = d.club_id
            JOIN planes_pago pp             ON pp.id = i.plan_pago_id
            LEFT JOIN tipos_planes_pago tpp ON tpp.id = pp.tipo_plan_id
            LEFT JOIN estados_persona ep    ON ep.id = d.estado_id
            LEFT JOIN grupo_deportista gd   ON gd.deportista_id = d.id AND gd.fecha_fin IS NULL
            LEFT JOIN grupos_entrenamiento ge ON ge.id = gd.grupo_entrenamiento_id
            WHERE i.estado = 'Activa' AND i.club_id = :#{T(com.varcal.cheermanager.tenant.TenantContext).require()}
            """, nativeQuery = true)
    List<Object[]> obtenerVistaDeportistasInscritos();

    long countByPlanPagoIdAndEstado(Integer planPagoId, Inscripcion.EstadoInscripcion estado);

}