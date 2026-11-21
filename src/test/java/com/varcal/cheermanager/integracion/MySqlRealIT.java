package com.varcal.cheermanager.integracion;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.varcal.cheermanager.DTO.Financiero.PagoDTO;
import com.varcal.cheermanager.DTO.Inventario.MovimientoInventarioDTO;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Inventario.CategoriaProducto;
import com.varcal.cheermanager.Models.Inventario.Producto;
import com.varcal.cheermanager.Models.Inventario.ProductoVariante;
import com.varcal.cheermanager.Service.Inventario.MovimientoInventarioService;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Inventario.CategoriaProductoRepository;
import com.varcal.cheermanager.repository.Inventario.ProductoRepository;
import com.varcal.cheermanager.repository.Inventario.ProductoVarianteRepository;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.DTO.Financiero.ResumenGeneracionCobrosDTO;
import com.varcal.cheermanager.Service.Financiero.PagoService;
import com.varcal.cheermanager.Service.Org_dep.InscripcionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import java.sql.Statement;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.MetodoPagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPagoRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * Pruebas contra MySQL 8 REAL (contenedor Docker), con el esquema creado por Flyway (V1+V2+V3) y
 * Hibernate en modo `validate`. Sin Docker se omiten (no fallan).
 *
 *  1) El contexto solo arranca si Flyway aplica todas las migraciones Y el esquema coincide con las
 *     entidades JPA (si falta una columna o un tipo no coincide, Hibernate aborta el arranque).
 *  2) Pagos simultaneos sobre la misma factura nunca superan el total (bloqueo pesimista).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MySqlRealIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("cheermanager").withUsername("cm").withPassword("cm");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("jwt.secret", () -> "dGVzdC1zZWNyZXRvLXBhcmEtcHJ1ZWJhcy1kZS1jaGVlcm1hbmFnZXItbWluaW1vLTMyLWJ5dGVzLW9rLW9r");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PagoService pagoService;
    @Autowired InscripcionService inscripcionService;
    @Autowired FacturaRepository facturaRepository;
    @Autowired PersonaRepository personaRepository;
    @Autowired TipoPagoRepository tipoPagoRepository;
    @Autowired MovimientoInventarioService movimientoInventarioService;
    @Autowired CategoriaProductoRepository categoriaProductoRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired ProductoVarianteRepository productoVarianteRepository;
    @Autowired UserRepository userRepository;
    @Autowired MetodoPagoRepository metodoPagoRepository;

    @Test
    void flywayAplicaTodoYHibernateValidaElEsquema() {
        // Si llegamos aqui el contexto arranco: migraciones OK y `validate` OK.
        Integer ultima = jdbc.queryForObject(
                "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertNotNull(ultima);
        assertTrue(ultima >= 3, "deben estar aplicadas al menos V1, V2 y V3");
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM clubes", Integer.class), "Plataforma + Ratels");
    }

    @Test
    void pagosSimultaneosNoSuperanElTotalDeLaFactura() throws Exception {
        final int hilos = 8;
        final BigDecimal total = new BigDecimal("100000.00");
        final BigDecimal monto = new BigDecimal("30000.00"); // caben exactamente 3 pagos (90.000)

        final Integer facturaId = TenantContext.runAs(2, () -> {
            Persona p = new Persona();
            p.setNombre("Prueba");
            p.setApellidos("Concurrencia");
            p = personaRepository.save(p);
            Factura f = new Factura();
            f.setNumeroFactura("IT-" + System.nanoTime());
            f.setFechaEmision(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
            f.setPersona(p);
            f.setTotal(total);
            return facturaRepository.save(f).getId();
        });
        final Integer tipoPagoId = TenantContext.runAs(2, () -> tipoPagoRepository.findAll().get(0).getId());
        final Integer metodoPagoId = TenantContext.runAs(2, () -> metodoPagoRepository.findAll().get(0).getId());

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            Callable<Boolean> tarea = () -> {
                salida.await(); // todos arrancan a la vez
                try {
                    TenantContext.runAs(2, () -> {
                        PagoDTO dto = new PagoDTO();
                        dto.setFacturaId(facturaId);
                        dto.setTipoPagoId(tipoPagoId);
                        dto.setMetodoPagoId(metodoPagoId);
                        dto.setFecha(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
                        dto.setEstado("Pagado");
                        dto.setMonto(monto);
                        pagoService.registrarPago(dto);
                    });
                    return true;
                } catch (RuntimeException e) {
                    return false; // rechazado por exceder el saldo
                }
            };
            resultados.add(pool.submit(tarea));
        }
        salida.countDown();
        int aceptados = 0;
        for (Future<Boolean> f : resultados) {
            if (f.get()) aceptados++;
        }
        pool.shutdown();

        BigDecimal pagado = jdbc.queryForObject(
                "SELECT COALESCE(SUM(monto),0) FROM pagos WHERE factura_id = ? AND estado = 'Pagado'",
                BigDecimal.class, facturaId);
        assertTrue(pagado.compareTo(total) <= 0, "se pago mas que el total de la factura: " + pagado);
        assertEquals(3, aceptados, "con 8 intentos de 30.000 sobre 100.000 deben caber exactamente 3");
    }

    // ---------------------------------------------------------------------------------------------------------
    // INV-01: el stock no se pierde ni se sobrevende cuando varias operaciones coinciden sobre la misma variante.
    // ---------------------------------------------------------------------------------------------------------

    private Long crearUsuarioClub2() {
        return TenantContext.runAs(2, () -> {
            Usuario u = new Usuario();
            String sufijo = Long.toString(System.nanoTime(), 36);
            u.setUsername("it-inv-" + sufijo);
            u.setEmail("it-inv-" + sufijo + "@example.com");
            u.setPasswordHash("no-se-usa");
            return Long.valueOf(userRepository.save(u).getId());
        });
    }

    private Integer crearVarianteConStock(int stock) {
        return TenantContext.runAs(2, () -> {
            String sufijo = Long.toString(System.nanoTime(), 36);
            CategoriaProducto c = new CategoriaProducto();
            c.setNombre("IT-cat-" + sufijo);
            c.setCodigo(sufijo.substring(sufijo.length() - 3).toUpperCase());
            c = categoriaProductoRepository.save(c);
            Producto p = new Producto();
            p.setSku("IT-" + sufijo);
            p.setNombre("Producto IT " + sufijo);
            p.setCategoriaProducto(c);
            p.setRequiereControlStock(true);
            p = productoRepository.save(p);
            ProductoVariante v = new ProductoVariante();
            v.setProducto(p);
            v.setSkuVariante("ITV-" + sufijo);
            v.setPrecioVenta(new BigDecimal("1000.00"));
            v.setStockActual(stock);
            return productoVarianteRepository.save(v).getId();
        });
    }

    /** Ejecuta cada tipo/cantidad en su propio hilo, todos a la vez. Devuelve cuantos fueron aceptados. */
    private int ejecutarMovimientosSimultaneos(Integer varianteId, Long usuarioId, List<String> tipos) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tipos.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (String tipo : tipos) {
            resultados.add(pool.submit((Callable<Boolean>) () -> {
                salida.await();
                try {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("it", null, List.of());
                    auth.setDetails(Map.of("userId", usuarioId));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    TenantContext.runAs(2, () -> {
                        MovimientoInventarioDTO dto = new MovimientoInventarioDTO();
                        dto.setProductoVarianteId(varianteId);
                        dto.setTipo(tipo);
                        dto.setCantidad(1);
                        dto.setMotivo("IT concurrencia");
                        movimientoInventarioService.registrar(dto);
                    });
                    return true;
                } catch (RuntimeException e) {
                    return false; // p. ej. "Stock insuficiente"
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }
        salida.countDown();
        int aceptados = 0;
        for (Future<Boolean> f : resultados) {
            if (f.get()) aceptados++;
        }
        pool.shutdown();
        return aceptados;
    }

    @Test
    void ventasSimultaneasNoSobrevendenElStock() throws Exception {
        Long usuarioId = crearUsuarioClub2();
        Integer varianteId = crearVarianteConStock(3);

        int aceptados = ejecutarMovimientosSimultaneos(varianteId, usuarioId, java.util.Collections.nCopies(8, "salida"));

        Integer stock = jdbc.queryForObject("SELECT stock_actual FROM producto_variantes WHERE id = ?", Integer.class, varianteId);
        Integer salidas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM movimientos_inventario WHERE producto_variante_id = ? AND tipo = 'salida'",
                Integer.class, varianteId);
        assertEquals(3, aceptados, "con stock 3 y 8 salidas de 1 unidad solo 3 deben aceptarse");
        assertEquals(0, stock, "el stock final debe ser 0 (nunca negativo)");
        assertEquals(3, salidas, "cada unidad vendida debe quedar registrada una vez");
    }

    @Test
    void entradasYSalidasSimultaneasConservanElStock() throws Exception {
        Long usuarioId = crearUsuarioClub2();
        Integer varianteId = crearVarianteConStock(100);

        List<String> tipos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tipos.add("entrada");
            tipos.add("salida");
        }
        int aceptados = ejecutarMovimientosSimultaneos(varianteId, usuarioId, tipos);

        Integer stock = jdbc.queryForObject("SELECT stock_actual FROM producto_variantes WHERE id = ?", Integer.class, varianteId);
        Integer movimientos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM movimientos_inventario WHERE producto_variante_id = ?", Integer.class, varianteId);
        assertEquals(20, aceptados, "con stock de sobra todas las operaciones deben aceptarse");
        assertEquals(100, stock, "10 entradas y 10 salidas de 1 unidad deben dejar el stock en 100");
        assertEquals(20, movimientos);
    }

    // ===== FIN-01: una mensualidad por inscripcion y periodo =====

    private Integer insertar(String sql, Object... args) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps;
        }, kh);
        return kh.getKey().intValue();
    }

    /** Persona + deportista + plan + inscripcion ACTIVA con plan de mensualidad, en el club 2 (Ratels). */
    private Integer crearInscripcionConMensualidadClub2() {
        Integer personaId = insertar("INSERT INTO personas (nombre, apellidos, club_id) VALUES (?, ?, 2)",
                "Prueba", "Mensualidad " + System.nanoTime());
        Integer estadoId = jdbc.queryForObject("SELECT id FROM estados_persona LIMIT 1", Integer.class);
        Integer deportistaId = insertar("INSERT INTO deportistas (persona_id, estado_id, club_id) VALUES (?, ?, 2)",
                personaId, estadoId);
        List<Integer> tipos = jdbc.queryForList("SELECT id FROM tipos_planes_pago WHERE club_id = 2 LIMIT 1", Integer.class);
        Integer tipoId = tipos.isEmpty()
                ? insertar("INSERT INTO tipos_planes_pago (categoria, duracion_meses, nombre, club_id) VALUES ('MENSUAL', 1, 'IT FIN-01', 2)")
                : tipos.get(0);
        Integer planId = insertar("INSERT INTO planes_pago (nombre, valor_mensual, fecha_vigencia_inicio, tipo_plan_id, club_id) "
                + "VALUES (?, 80000.00, '2026-01-01', ?, 2)", "Plan IT FIN-01 " + System.nanoTime(), tipoId);
        return insertar("INSERT INTO inscripciones (deportista_id, plan_pago_id, plan_mensualidad_id, fecha_inscripcion, estado, club_id) "
                + "VALUES (?, ?, ?, '2026-01-15', 'Activa', 2)", deportistaId, planId, planId);
    }

    @Test
    void dosGeneracionesSimultaneasDelMismoPeriodoDejanUnaSolaMensualidad() throws Exception {
        final Integer inscripcionId = crearInscripcionConMensualidadClub2();
        final String periodo = "2026-09";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<ResumenGeneracionCobrosDTO>> resultados = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            resultados.add(pool.submit(() -> {
                salida.await(); // el job y el boton manual arrancan a la vez
                return TenantContext.runAs(2, () -> inscripcionService.generarCobrosMensualidad(periodo));
            }));
        }
        salida.countDown();
        int generadas = 0;
        int yaFacturadas = 0;
        for (Future<ResumenGeneracionCobrosDTO> f : resultados) {
            ResumenGeneracionCobrosDTO r = f.get();
            assertTrue(r.getErrores().isEmpty(), "una colision no debe reportarse como error: " + r.getErrores());
            generadas += r.getFacturasGeneradas();
            yaFacturadas += r.getYaFacturadas();
        }
        pool.shutdown();

        Integer facturas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM facturas WHERE inscripcion_id = ? AND periodo = ?", Integer.class, inscripcionId, periodo);
        assertEquals(1, facturas, "debe existir exactamente UNA mensualidad del periodo");
        assertEquals(1, generadas, "solo un proceso la crea");
        assertEquals(1, yaFacturadas, "el otro la cuenta como ya facturada");
    }

    @Test
    void elIndiceUnicoRechazaElPeriodoRepetidoPeroNoLasFacturasSinPeriodo() {
        Integer inscripcionId = crearInscripcionConMensualidadClub2();
        Integer personaId = jdbc.queryForObject("SELECT p.id FROM personas p JOIN deportistas d ON d.persona_id = p.id "
                + "JOIN inscripciones i ON i.deportista_id = d.id WHERE i.id = ?", Integer.class, inscripcionId);
        String sql = "INSERT INTO facturas (numero_factura, fecha_emision, persona_id, total, inscripcion_id, periodo, club_id) "
                + "VALUES (?, '2026-10-01', ?, 80000.00, ?, ?, 2)";

        // Sin periodo (inscripcion, tienda, manuales): MySQL admite varios NULL en un UNIQUE.
        insertar(sql, "IT-NULL-A-" + System.nanoTime(), personaId, inscripcionId, null);
        insertar(sql, "IT-NULL-B-" + System.nanoTime(), personaId, inscripcionId, null);

        insertar(sql, "IT-P-A-" + System.nanoTime(), personaId, inscripcionId, "2026-10");
        assertThrows(DataIntegrityViolationException.class,
                () -> insertar(sql, "IT-P-B-" + System.nanoTime(), personaId, inscripcionId, "2026-10"));
    }
}
