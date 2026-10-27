package com.varcal.cheermanager.Models.Funcionalidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Configuracion de recordatorios de mora de UN club (una sola fila por club). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "recordatorios_config")
public class RecordatorioConfig {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** true = el sistema envia solo cada dia (8:00 Bogota) con los canales marcados. */
    @Column(name = "automatico", nullable = false)
    private Boolean automatico = false;

    /** Aviso dentro del sistema (portal): es el canal estandar. */
    @Column(name = "canal_sistema", nullable = false)
    private Boolean canalSistema = true;

    @Column(name = "canal_correo", nullable = false)
    private Boolean canalCorreo = false;

    @Column(name = "canal_whatsapp", nullable = false)
    private Boolean canalWhatsapp = false;

    /** No se vuelve a avisar de la misma factura por el mismo canal antes de estos dias. */
    @Column(name = "dias_entre_avisos", nullable = false)
    private Integer diasEntreAvisos = 7;
}
