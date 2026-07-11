package com.varcal.cheermanager.repository.Funcionalidad;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Funcionalidad.Auditoria;

// Auditoria.java (Model) existía sin ningún Repository — no había forma de
// leer los registros de auditoría desde código. Se agrega el mínimo para
// que el dashboard de Administración pueda mostrar actividad reciente.
public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {
    List<Auditoria> findByFechaAfterOrderByFechaDesc(LocalDateTime desde, Pageable pageable);
}