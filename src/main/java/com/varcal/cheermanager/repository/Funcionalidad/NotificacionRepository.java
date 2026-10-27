package com.varcal.cheermanager.repository.Funcionalidad;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Funcionalidad.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findTop50ByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    long countByUsuarioIdAndVistaFalse(Integer usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notificacion n SET n.vista = true WHERE n.usuario.id = :usuarioId AND n.vista = false")
    int marcarTodasVistas(@Param("usuarioId") Integer usuarioId);
}
