package com.varcal.cheermanager.Models.Inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Personas.Persona;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ventas")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago; // ej. "efectivo", "transferencia" — sin catálogo formal aún, igual que en Financiero

    // --- Comprador: exactamente uno de los dos grupos debe estar diligenciado ---

    // Si el comprador es un deportista/entrenador/persona ya registrada en el
    // club (ej. compra su propia licra, o un papá compra para su hija).
    @ManyToOne
    @JoinColumn(name = "persona_id")
    private Persona persona;

    // Si el comprador es externo (no inscrito en el club) — ej. alguien que
    // compra merchandising o un regalo en un evento. El service valida que
    // se llene esto SOLO cuando persona_id es null.
    @Column(name = "nombre_comprador_externo", length = 150)
    private String nombreCompradorExterno;

    @Column(name = "contacto_comprador_externo", length = 150)
    private String contactoCompradorExterno; // teléfono o email, texto libre

    // Opcional: si la venta se carga a la cuenta del deportista (ej. se suma
    // a su próxima factura de mensualidad) en vez de cobrarse de contado.
    // Queda null en la inmensa mayoría de ventas de tienda (pago inmediato).
    @ManyToOne
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles;
}