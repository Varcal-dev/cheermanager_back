package com.varcal.cheermanager.DTO;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Controller.clubes.PagosEnLineaController.PagarRequest;
import com.varcal.cheermanager.DTO.Financiero.CobroEventoLoteDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaManualDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoDTO;
import com.varcal.cheermanager.DTO.Inventario.MovimientoInventarioDTO;
import com.varcal.cheermanager.DTO.Inventario.VentaDTO;
import com.varcal.cheermanager.config.GlobalExceptionHandler;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * SEC-02 fase 5B (dinero y caja): los DTO de cobros, pagos, ventas e inventario rechazan ids/montos vacios con 400
 * en vez de NPE (500). Las reglas de negocio siguen en los servicios.
 */
class ValidacionLoteBDineroCajaTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static Set<String> campos(Object o) {
        Set<String> r = new TreeSet<>();
        validador.validate(o).forEach(v -> r.add(v.getPropertyPath().toString()));
        return r;
    }

    private static String x(int n) {
        return "x".repeat(n);
    }

    @Test
    void pago_exige_factura_tipo_metodo_fecha_estado_y_monto_positivo() {
        assertEquals(Set.of("facturaId", "tipoPagoId", "metodoPagoId", "fecha", "estado", "monto"), campos(new PagoDTO()));
        PagoDTO d = new PagoDTO();
        d.setFacturaId(1);
        d.setTipoPagoId(2);
        d.setMetodoPagoId(3);
        d.setFecha(LocalDate.of(2026, 11, 27));
        d.setEstado("Pagado");
        d.setMonto(new BigDecimal("50000"));
        assertTrue(campos(d).isEmpty());
        d.setMonto(BigDecimal.ZERO);
        assertEquals(Set.of("monto"), campos(d));
    }

    @Test
    void factura_manual_exige_persona_descripcion_y_total_positivo() {
        assertEquals(Set.of("personaId", "descripcion", "total"), campos(new FacturaManualDTO()));
        FacturaManualDTO d = new FacturaManualDTO();
        d.setPersonaId(1);
        d.setConcepto("UNIFORME");
        d.setDescripcion("Camiseta");
        d.setTotal(new BigDecimal("80000"));
        assertTrue(campos(d).isEmpty());
        d.setDescripcion(x(256));
        d.setConcepto(x(31));
        assertEquals(Set.of("descripcion", "concepto"), campos(d));
    }

    @Test
    void venta_exige_items_y_cada_item_variante_y_cantidad() {
        VentaDTO v = new VentaDTO();
        assertEquals(Set.of("items"), campos(v));
        v.setItems(List.of(new VentaDTO.ItemVentaDTO()));
        assertEquals(Set.of("items[0].productoVarianteId", "items[0].cantidad"), campos(v));
        VentaDTO.ItemVentaDTO i = new VentaDTO.ItemVentaDTO();
        i.setProductoVarianteId(4);
        i.setCantidad(0);
        v.setItems(List.of(i));
        assertEquals(Set.of("items[0].cantidad"), campos(v));
        i.setCantidad(2);
        v.setNombreCompradorExterno(x(151));
        v.setContactoCompradorExterno(x(151));
        v.setMetodoPago(x(31));
        assertEquals(Set.of("nombreCompradorExterno", "contactoCompradorExterno", "metodoPago"), campos(v));
    }

    @Test
    void movimiento_de_inventario_exige_variante_tipo_y_cantidad_positiva() {
        assertEquals(Set.of("productoVarianteId", "tipo", "cantidad"), campos(new MovimientoInventarioDTO()));
        MovimientoInventarioDTO d = new MovimientoInventarioDTO();
        d.setProductoVarianteId(1);
        d.setTipo("entrada");
        d.setCantidad(5);
        d.setMotivo(x(301));
        assertEquals(Set.of("motivo"), campos(d));
    }

    @Test
    void cobro_de_evento_exige_monto_positivo() {
        CobroEventoLoteDTO d = new CobroEventoLoteDTO();
        assertEquals(Set.of("monto"), campos(d));
        d.setMonto(new BigDecimal("-1"));
        assertEquals(Set.of("monto"), campos(d));
        d.setMonto(new BigDecimal("30000"));
        assertTrue(campos(d).isEmpty());
    }

    @Test
    void pago_en_linea_exige_meses_entre_1_y_12() {
        assertEquals(Set.of("meses"), campos(new PagarRequest(null)));
        assertEquals(Set.of("meses"), campos(new PagarRequest(0)));
        assertEquals(Set.of("meses"), campos(new PagarRequest(13)));
        assertTrue(campos(new PagarRequest(12)).isEmpty());
    }

    // ---------- HTTP: llega 400 con el campo, no 500 ----------

    @RestController
    static class ControladorPrueba {
        @PostMapping("/t/pagos")
        public String pago(@Valid @RequestBody PagoDTO dto) {
            return "ok";
        }

        @PostMapping("/t/ventas")
        public String venta(@Valid @RequestBody VentaDTO dto) {
            return "ok";
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorPrueba())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void el_cuerpo_del_front_para_pagos_y_ventas_pasa() throws Exception {
        mvc.perform(post("/t/pagos").contentType(MediaType.APPLICATION_JSON).content(
                "{\"facturaId\":1,\"tipoPagoId\":2,\"metodoPagoId\":3,\"fecha\":\"2026-11-27\",\"estado\":\"Pagado\",\"monto\":50000}"))
                .andExpect(status().isOk());
        mvc.perform(post("/t/ventas").contentType(MediaType.APPLICATION_JSON).content(
                "{\"personaId\":null,\"nombreCompradorExterno\":\"Ana\",\"contactoCompradorExterno\":\"300\",\"metodoPago\":\"efectivo\","
                        + "\"pagada\":null,\"items\":[{\"productoVarianteId\":4,\"cantidad\":2}]}"))
                .andExpect(status().isOk());
    }

    @Test
    void pago_sin_factura_y_venta_sin_items_son_400() throws Exception {
        mvc.perform(post("/t/pagos").contentType(MediaType.APPLICATION_JSON).content("{\"monto\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("facturaId: ")));
        mvc.perform(post("/t/ventas").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("items: ")));
    }
}
