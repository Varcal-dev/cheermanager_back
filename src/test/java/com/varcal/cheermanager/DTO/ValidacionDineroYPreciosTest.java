package com.varcal.cheermanager.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.ConvenioDto;
import com.varcal.cheermanager.DTO.Financiero.PlanPagoDTO;
import com.varcal.cheermanager.DTO.Financiero.TipoPlanPagoDTO;
import com.varcal.cheermanager.DTO.Inventario.ActualizarPrecioDTO;
import com.varcal.cheermanager.DTO.Inventario.CategoriaProductoDTO;
import com.varcal.cheermanager.DTO.Inventario.ProductoDTO;
import com.varcal.cheermanager.DTO.Inventario.ProductoVarianteDTO;
import com.varcal.cheermanager.Models.Financiero.Descuento;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * SEC-02 fase 1: reglas de Bean Validation de dinero, precios y porcentajes.
 * Usa el Validator de Jakarta directamente (sin Spring) para probar cada regla.
 */
class ValidacionDineroYPreciosTest {

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

    /** Campos con error, ordenados, para comparar de forma estable. */
    private static <T> Set<String> campos(T objeto) {
        return validador.validate(objeto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    private static <T> String mensaje(T objeto, String campo) {
        return validador.validate(objeto).stream()
                .filter(v -> v.getPropertyPath().toString().equals(campo))
                .map(ConstraintViolation::getMessage)
                .findFirst().orElse(null);
    }

    // ---------- PlanPagoDTO ----------

    private static PlanPagoDTO planValido() {
        PlanPagoDTO d = new PlanPagoDTO();
        d.setNombre("Plan Gold");
        d.setTipoPlanId(1);
        d.setValorMensual(new BigDecimal("150000.00"));
        d.setFechaVigenciaInicio(LocalDate.of(2026, 1, 1));
        return d;
    }

    @Test
    void plan_valido_no_tiene_errores() {
        assertTrue(campos(planValido()).isEmpty());
    }

    @Test
    void plan_rechaza_valor_negativo_y_nulo() {
        PlanPagoDTO d = planValido();
        d.setValorMensual(new BigDecimal("-1"));
        assertEquals(Set.of("valorMensual"), campos(d));
        d.setValorMensual(null);
        assertEquals(Set.of("valorMensual"), campos(d));
    }

    @Test
    void plan_acepta_valor_cero() {
        PlanPagoDTO d = planValido();
        d.setValorMensual(BigDecimal.ZERO);
        assertTrue(campos(d).isEmpty());
    }

    @Test
    void plan_rechaza_valor_que_no_cabe_en_decimal_10_2() {
        PlanPagoDTO d = planValido();
        d.setValorMensual(new BigDecimal("123456789.00")); // 9 enteros
        assertEquals(Set.of("valorMensual"), campos(d));
        d.setValorMensual(new BigDecimal("10.123")); // 3 decimales
        assertEquals(Set.of("valorMensual"), campos(d));
    }

    @Test
    void plan_descuento_entre_0_y_100() {
        PlanPagoDTO d = planValido();
        d.setDescuentoPorcentaje(null); // opcional
        assertTrue(campos(d).isEmpty());
        d.setDescuentoPorcentaje(new BigDecimal("0"));
        assertTrue(campos(d).isEmpty());
        d.setDescuentoPorcentaje(new BigDecimal("100"));
        assertTrue(campos(d).isEmpty());
        d.setDescuentoPorcentaje(new BigDecimal("100.01"));
        assertEquals(Set.of("descuentoPorcentaje"), campos(d));
        d.setDescuentoPorcentaje(new BigDecimal("-5"));
        assertEquals(Set.of("descuentoPorcentaje"), campos(d));
    }

    @Test
    void plan_exige_nombre_tipo_y_fecha_inicio() {
        PlanPagoDTO d = new PlanPagoDTO();
        d.setValorMensual(BigDecimal.TEN);
        assertEquals(Set.of("nombre", "tipoPlanId", "fechaVigenciaInicio"), campos(d));
    }

    @Test
    void plan_nombre_en_blanco_o_mas_de_100_caracteres() {
        PlanPagoDTO d = planValido();
        d.setNombre("   ");
        assertEquals(Set.of("nombre"), campos(d));
        d.setNombre("x".repeat(101));
        assertEquals(Set.of("nombre"), campos(d));
        d.setNombre("x".repeat(100));
        assertTrue(campos(d).isEmpty());
    }

    @Test
    void plan_sesiones_semanales_no_negativas() {
        PlanPagoDTO d = planValido();
        d.setSesionesSemanales(-1);
        assertEquals(Set.of("sesionesSemanales"), campos(d));
        d.setSesionesSemanales(3);
        assertTrue(campos(d).isEmpty());
    }

    // ---------- TipoPlanPagoDTO ----------

    private static TipoPlanPagoDTO tipoValido() {
        TipoPlanPagoDTO d = new TipoPlanPagoDTO();
        d.setNombre("Mensual");
        d.setCategoria("mensualidad");
        d.setDuracionMeses(1);
        return d;
    }

    @Test
    void tipo_plan_valido_y_duracion_minima() {
        assertTrue(campos(tipoValido()).isEmpty());
        TipoPlanPagoDTO d = tipoValido();
        d.setDuracionMeses(0);
        assertEquals(Set.of("duracionMeses"), campos(d));
        d.setDuracionMeses(null);
        assertEquals(Set.of("duracionMeses"), campos(d));
    }

    @Test
    void tipo_plan_exige_nombre_y_categoria() {
        TipoPlanPagoDTO d = new TipoPlanPagoDTO();
        d.setDuracionMeses(1);
        assertEquals(Set.of("nombre", "categoria"), campos(d));
    }

    // ---------- ConvenioDto ----------

    @Test
    void convenio_exige_empresa_y_descuento() {
        ConvenioDto d = new ConvenioDto();
        assertEquals(Set.of("nombreEmpresa", "descuentoId"), campos(d));
        d.setNombreEmpresa("Acme");
        d.setDescuentoId(7);
        assertTrue(campos(d).isEmpty()); // las fechas siguen siendo opcionales (la columna admite NULL)
        d.setNombreEmpresa("x".repeat(101));
        assertEquals(Set.of("nombreEmpresa"), campos(d));
    }

    // ---------- Descuento (entidad) ----------

    private static Descuento descuentoValido() {
        Descuento d = new Descuento();
        d.setNombre("Hermanos");
        d.setPorcentaje(new BigDecimal("15"));
        d.setFechaInicio(LocalDate.of(2026, 1, 1));
        return d;
    }

    @Test
    void descuento_valido_y_limites_de_porcentaje() {
        Descuento d = descuentoValido();
        assertTrue(campos(d).isEmpty());
        d.setPorcentaje(BigDecimal.ZERO);
        assertTrue(campos(d).isEmpty());
        d.setPorcentaje(new BigDecimal("100"));
        assertTrue(campos(d).isEmpty());
        d.setPorcentaje(new BigDecimal("500"));
        assertEquals(Set.of("porcentaje"), campos(d));
        d.setPorcentaje(new BigDecimal("-20"));
        assertEquals(Set.of("porcentaje"), campos(d));
        d.setPorcentaje(null);
        assertEquals(Set.of("porcentaje"), campos(d));
    }

    @Test
    void descuento_exige_nombre_y_fecha_inicio_y_limita_textos() {
        Descuento d = new Descuento();
        d.setPorcentaje(BigDecimal.TEN);
        assertEquals(Set.of("nombre", "fechaInicio"), campos(d));
        Descuento largo = descuentoValido();
        largo.setNombre("x".repeat(256));
        largo.setDescripcion("y".repeat(256));
        assertEquals(Set.of("nombre", "descripcion"), campos(largo));
    }

    // ---------- Categoría de producto ----------

    @Test
    void categoria_exige_nombre_y_codigo_de_maximo_3() {
        CategoriaProductoDTO d = new CategoriaProductoDTO();
        assertEquals(Set.of("nombre", "codigo"), campos(d));
        d.setNombre("Ropa");
        d.setCodigo("ROP");
        assertTrue(campos(d).isEmpty());
        d.setCodigo("ROPA");
        assertEquals(Set.of("codigo"), campos(d));
    }

    // ---------- Producto ----------

    private static ProductoDTO productoValido() {
        ProductoDTO d = new ProductoDTO();
        d.setNombre("Gorra");
        d.setCategoriaProductoId(2);
        return d;
    }

    @Test
    void producto_valido_con_precios_de_referencia_opcionales() {
        assertTrue(campos(productoValido()).isEmpty());
        ProductoDTO d = productoValido();
        d.setPrecioMinReferencia(new BigDecimal("0"));
        d.setPrecioMaxReferencia(new BigDecimal("99999999.99"));
        assertTrue(campos(d).isEmpty());
    }

    @Test
    void producto_rechaza_precios_negativos_y_textos_largos() {
        ProductoDTO d = productoValido();
        d.setPrecioMinReferencia(new BigDecimal("-1"));
        d.setPrecioMaxReferencia(new BigDecimal("-1"));
        d.setNotasProveedor("n".repeat(501));
        d.setOrigen("o".repeat(256));
        d.setDescripcion("d".repeat(256));
        d.setCodigo("c".repeat(21));
        assertEquals(Set.of("precioMinReferencia", "precioMaxReferencia", "notasProveedor", "origen",
                "descripcion", "codigo"), campos(d));
    }

    @Test
    void producto_exige_nombre_y_categoria() {
        assertEquals(Set.of("nombre", "categoriaProductoId"), campos(new ProductoDTO()));
    }

    // ---------- Variante de producto ----------

    private static ProductoVarianteDTO varianteValida() {
        ProductoVarianteDTO d = new ProductoVarianteDTO();
        d.setProductoId(1);
        d.setPrecioVenta(new BigDecimal("25000"));
        return d;
    }

    @Test
    void variante_valida_y_precio_cero_permitido() {
        assertTrue(campos(varianteValida()).isEmpty());
        ProductoVarianteDTO d = varianteValida();
        d.setPrecioVenta(BigDecimal.ZERO); // cortesías / regalos
        assertTrue(campos(d).isEmpty());
    }

    @Test
    void variante_rechaza_precio_negativo_nulo_y_stock_negativo() {
        ProductoVarianteDTO d = varianteValida();
        d.setPrecioVenta(new BigDecimal("-10"));
        d.setStockInicial(-1);
        assertEquals(Set.of("precioVenta", "stockInicial"), campos(d));
        d.setPrecioVenta(null);
        d.setStockInicial(null);
        assertEquals(Set.of("precioVenta"), campos(d));
    }

    @Test
    void variante_exige_producto_y_limita_talla_y_color() {
        ProductoVarianteDTO d = new ProductoVarianteDTO();
        d.setPrecioVenta(BigDecimal.ONE);
        d.setTalla("t".repeat(21));
        d.setColor("c".repeat(31));
        assertEquals(Set.of("productoId", "talla", "color"), campos(d));
    }

    // ---------- PUT /precio ----------

    @Test
    void actualizar_precio_rechaza_negativo_y_nulo_pero_acepta_cero() {
        assertTrue(campos(new ActualizarPrecioDTO(new BigDecimal("25000"))).isEmpty());
        assertTrue(campos(new ActualizarPrecioDTO(BigDecimal.ZERO)).isEmpty());
        assertEquals(Set.of("precioVenta"), campos(new ActualizarPrecioDTO(new BigDecimal("-1"))));
        assertEquals(Set.of("precioVenta"), campos(new ActualizarPrecioDTO(null)));
    }

    @Test
    void mensajes_estan_en_espanol() {
        PlanPagoDTO d = planValido();
        d.setValorMensual(new BigDecimal("-1"));
        assertEquals("El valor mensual no puede ser negativo", mensaje(d, "valorMensual"));
    }
}
