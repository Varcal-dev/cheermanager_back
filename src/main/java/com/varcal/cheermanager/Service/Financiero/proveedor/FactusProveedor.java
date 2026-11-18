package com.varcal.cheermanager.Service.Financiero.proveedor;

import org.springframework.stereotype.Component;

import com.varcal.cheermanager.Models.Financiero.FacturacionConfig;
import com.varcal.cheermanager.Models.Financiero.Factura;

/**
 * Factus (proveedor tecnologico DIAN). En DIAN-1 solo existe el esqueleto: se sabe si el servidor tiene las
 * credenciales (variables de entorno FACTUS_CLIENT_ID, FACTUS_CLIENT_SECRET, FACTUS_USERNAME, FACTUS_PASSWORD) pero
 * todavia no se llama a la API; el envio llega en DIAN-2.
 */
@Component
public class FactusProveedor implements ProveedorFacturacion {

    public static final String CODIGO = "FACTUS";

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public boolean credencialesConfiguradas() {
        return hay("FACTUS_CLIENT_ID") && hay("FACTUS_CLIENT_SECRET") && hay("FACTUS_USERNAME")
                && hay("FACTUS_PASSWORD");
    }

    @Override
    public Resultado emitir(FacturacionConfig config, Factura factura, String referencia) {
        throw new IllegalStateException("El envio a Factus todavia no esta implementado (llega en DIAN-2)");
    }

    private static boolean hay(String variable) {
        String v = System.getenv(variable);
        return v != null && !v.isBlank();
    }
}
