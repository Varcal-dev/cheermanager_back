package com.varcal.cheermanager.Service.Financiero.proveedor;

import com.varcal.cheermanager.Models.Financiero.FacturacionConfig;
import com.varcal.cheermanager.Models.Financiero.Factura;

/**
 * Puerta unica hacia quien firma y envia a la DIAN. Hoy la implementa Factus; si mas adelante construimos
 * motor propio, es otra implementacion y el resto del sistema no cambia.
 */
public interface ProveedorFacturacion {

    /** Resultado de emitir: numero y CUFE asignados, y el enlace al PDF (puede venir vacio si quedo en proceso). */
    record Resultado(boolean aceptada, String numero, String cufe, String urlPdf, String mensaje) {
    }

    /** Codigo guardado en facturacion_config.proveedor. */
    String codigo();

    /** true si el servidor tiene las credenciales (variables de entorno) para hablar con el proveedor. */
    boolean credencialesConfiguradas();

    /** Emite la factura. La referencia es idempotente: repetirla no duplica el documento. */
    Resultado emitir(FacturacionConfig config, Factura factura, String referencia);
}
