package com.varcal.cheermanager.Models.Funcionalidad;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity                          // ← ¿está esto?
@Table(name = "log_acceso")
public class LogAcceso {
    @Id
    private Integer id;
}
