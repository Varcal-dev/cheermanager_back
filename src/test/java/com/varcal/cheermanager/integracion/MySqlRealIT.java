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

import com.varcal.cheermanager.DTO.Comunes.PaginaDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Persona.PersonaService;
import com.varcal.cheermanager.DTO.Persona.DeportistaBusquedaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaOpcionDTO;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
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
import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.Service.Importacion.ExportesService;
import com.varcal.cheermanager.Service.Persona.CopiaDatosService;
import com.varcal.cheermanager.tenant.TenantLookupRepository;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
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
    @Autowired FacturaService facturaService;
    @Autowired PersonaService personaService;
    @Autowired DeportistaRepository deportistaRepository;
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
    @Autowired ExportesService exportesService;
    @Autowired CopiaDatosService copiaDatosService;
    @Autowired TenantLookupRepository tenantLookup;

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

    // ---------------------------------------------------------------------------------------------------------
    // PERF-01: la pantalla de Pagos pagina en el servidor. Estas consultas HQL solo se validan arrancando el contexto
    // (aqui) y ejecutandolas contra MySQL real; la base compartida puede traer datos de otras pruebas, por eso se aisla
    // con una persona unica y los totales se comparan como DIFERENCIA antes/despues.
    // ---------------------------------------------------------------------------------------------------------

    private Integer crearFactura(Persona persona, String numero, String total, boolean anulada) {
        Factura f = new Factura();
        f.setNumeroFactura(numero);
        f.setFechaEmision(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        f.setPersona(persona);
        f.setTotal(new BigDecimal(total));
        f.setAnulada(anulada);
        return facturaRepository.save(f).getId();
    }

    private void pagar(Integer facturaId, String monto, Integer tipoPagoId, Integer metodoPagoId) {
        PagoDTO dto = new PagoDTO();
        dto.setFacturaId(facturaId);
        dto.setTipoPagoId(tipoPagoId);
        dto.setMetodoPagoId(metodoPagoId);
        dto.setFecha(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        dto.setEstado("Pagado");
        dto.setMonto(new BigDecimal(monto));
        pagoService.registrarPago(dto);
    }

    @Test
    void pantallaDePagosPaginaFiltraYTotalizaComoElDetalle() {
        final String sufijo = Long.toString(System.nanoTime(), 36);
        final Integer tipoPagoId = TenantContext.runAs(2, () -> tipoPagoRepository.findAll().get(0).getId());
        final Integer metodoPagoId = TenantContext.runAs(2, () -> metodoPagoRepository.findAll().get(0).getId());

        Map<String, Object> antes = TenantContext.runAs(2, () -> pagoService.resumen());

        // F1 pagada completa (sin saldo), F2 con abono (saldo 150), F3 sin pagos (saldo 300), F4 anulada (no cuenta).
        TenantContext.runAs(2, () -> {
            Persona p = new Persona();
            p.setNombre("ItPagos");
            p.setApellidos(sufijo);
            p = personaRepository.save(p);
            Integer f1 = crearFactura(p, "ITP-" + sufijo + "-1", "100.00", false);
            Integer f2 = crearFactura(p, "ITP-" + sufijo + "-2", "200.00", false);
            crearFactura(p, "ITP-" + sufijo + "-3", "300.00", false);
            crearFactura(p, "ITP-" + sufijo + "-4", "400.00", true);
            pagar(f1, "100.00", tipoPagoId, metodoPagoId);
            pagar(f2, "50.00", tipoPagoId, metodoPagoId);
        });

        // --- Totales de las tarjetas: la diferencia debe ser exactamente lo creado.
        Map<String, Object> despues = TenantContext.runAs(2, () -> pagoService.resumen());
        BigDecimal dRecaudado = ((BigDecimal) despues.get("totalRecaudado")).subtract((BigDecimal) antes.get("totalRecaudado"));
        BigDecimal dPendiente = ((BigDecimal) despues.get("totalPendiente")).subtract((BigDecimal) antes.get("totalPendiente"));
        assertEquals(0, new BigDecimal("150.00").compareTo(dRecaudado), "recaudado: 100 + 50");
        assertEquals(0, new BigDecimal("450.00").compareTo(dPendiente), "pendiente: 150 (F2) + 300 (F3); F1 pagada y F4 anulada no cuentan");

        // --- Facturas con saldo: solo F2 y F3 de esta persona.
        PaginaDTO<FacturaResponseDTO> conSaldo = TenantContext.runAs(2, () -> facturaService.listarConSaldo());
        List<String> numeros = conSaldo.contenido().stream().map(FacturaResponseDTO::getNumeroFactura)
                .filter(n -> n.startsWith("ITP-" + sufijo)).sorted().toList();
        assertEquals(List.of("ITP-" + sufijo + "-2", "ITP-" + sufijo + "-3"), numeros);

        // --- Pagos paginados, aislados con la busqueda por nombre unico.
        PaginaDTO<PagoResponseDTO> todos = TenantContext.runAs(2, () -> pagoService.listarPagina(sufijo, null, null, null, 0, 20));
        assertEquals(2, todos.totalElementos());
        assertEquals(2, todos.contenido().size());
        assertEquals(0, new BigDecimal("50.00").compareTo(todos.contenido().get(0).getMonto()), "el mas reciente primero");

        PaginaDTO<PagoResponseDTO> pagina0 = TenantContext.runAs(2, () -> pagoService.listarPagina(sufijo, null, null, null, 0, 1));
        PaginaDTO<PagoResponseDTO> pagina1 = TenantContext.runAs(2, () -> pagoService.listarPagina(sufijo, null, null, null, 1, 1));
        assertEquals(2, pagina0.totalElementos());
        assertEquals(2, pagina0.totalPaginas());
        assertEquals(1, pagina0.contenido().size());
        assertEquals(1, pagina1.contenido().size());
        assertNotEquals(pagina0.contenido().get(0).getId(), pagina1.contenido().get(0).getId());

        // --- Filtros: por numero de factura, por estado y por rango de fechas.
        assertEquals(1, TenantContext.runAs(2, () -> pagoService.listarPagina("ITP-" + sufijo + "-2", "PAGADO", null, null, 0, 20)).totalElementos());
        assertEquals(0, TenantContext.runAs(2, () -> pagoService.listarPagina(sufijo, "ANULADO", null, null, 0, 20)).totalElementos());
        LocalDate manana = com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(1);
        assertEquals(0, TenantContext.runAs(2, () -> pagoService.listarPagina(sufijo, null, manana, null, 0, 20)).totalElementos());
    }

    // ---------------------------------------------------------------------------------------------------------
    // PERF-01 (5b): busqueda ligera y lista minima de deportistas. Las consultas HQL nuevas se validan aqui contra
    // MySQL real (y al arrancar el contexto).
    // ---------------------------------------------------------------------------------------------------------

    @Test
    void busquedaYOpcionesDeDeportistasFiltranPorNombreYPorClub() {
        final String sufijo = Long.toString(System.nanoTime(), 36);
        final Integer deportistaId = TenantContext.runAs(2, () -> {
            Persona p = new Persona();
            p.setNombre("ItBusca");
            p.setApellidos("Zz" + sufijo);
            p = personaRepository.save(p);
            Deportista d = new Deportista();
            d.setPersona(p);
            return deportistaRepository.save(d).getId();
        });

        // Por nombre completo parcial, sin distinguir mayusculas.
        List<DeportistaBusquedaDTO> r = TenantContext.runAs(2, () -> personaService.buscarDeportistas("itbusca zz" + sufijo.toUpperCase(), 6));
        assertEquals(1, r.size());
        assertEquals(deportistaId, r.get(0).deportistaId());
        assertEquals("ItBusca", r.get(0).nombre());

        // Menos de 2 letras: vacio.
        assertTrue(TenantContext.runAs(2, () -> personaService.buscarDeportistas("i", 6)).isEmpty());

        // El limite se respeta (todos los "ItBusca" que haya, nunca mas de 1 si se pide 1).
        assertTrue(TenantContext.runAs(2, () -> personaService.buscarDeportistas("ItBusca", 1)).size() <= 1);

        // Otro club NO ve a este deportista (el filtro de club lo pone Hibernate).
        List<DeportistaBusquedaDTO> otroClub = TenantContext.runAs(1, () -> personaService.buscarDeportistas("zz" + sufijo, 6));
        assertTrue(otroClub.isEmpty(), "el club 1 no debe ver deportistas del club 2");

        // Opciones: incluye al deportista creado, con persona y nombre.
        List<DeportistaOpcionDTO> opciones = TenantContext.runAs(2, () -> personaService.listarDeportistasOpciones());
        assertTrue(opciones.stream().anyMatch(o -> o.deportistaId().equals(deportistaId) && "ItBusca".equals(o.nombre())));
        assertTrue(TenantContext.runAs(1, () -> personaService.listarDeportistasOpciones()).stream()
                .noneMatch(o -> o.deportistaId().equals(deportistaId)));
    }

    // ===== TEST-01 (D): exportes y copia de datos aislados por club, y cupo del plan contra BD real =====

    /** Lo sembrado en un club para comprobar que sus datos no aparecen en otro. */
    private record Sembrado(Integer personaId, Integer deportistaId, String apellidos, String numeroFactura,
            String notaCaja) {
    }

    /**
     * Siembra en el club indicado: persona + deportista, un acudiente vinculado como principal, una factura con un
     * pago y un cierre de caja. Todo lleva el marcador en el apellido / numero / nota para buscarlo en los CSV.
     */
    private Sembrado sembrarDatosDeExporte(int clubId, String marcador, Integer tipoPagoId, Integer metodoPagoId,
            boolean conCierreDeCaja) {
        final String apellidos = "ItExp" + marcador;
        final String numeroFactura = "ITE-" + marcador;
        final String notaCaja = "ItCaja" + marcador;
        final Integer[] ids = new Integer[2];
        TenantContext.runAs(clubId, () -> {
            Persona p = new Persona();
            p.setNombre("Titular");
            p.setApellidos(apellidos);
            p = personaRepository.save(p);
            Deportista d = new Deportista();
            d.setPersona(p);
            d = deportistaRepository.save(d);
            ids[0] = p.getId();
            ids[1] = d.getId();

            Integer facturaId = crearFactura(p, numeroFactura, "100.00", false);
            pagar(facturaId, "40.00", tipoPagoId, metodoPagoId);
        });
        // Acudiente y vinculo por SQL: las claves foraneas compuestas (club_id, id) los atan al mismo club.
        Integer acudienteId = insertar(
                "INSERT INTO acudientes (club_id, nombre, apellidos, telefono) VALUES (?, 'AcuIt', ?, '3000000000')",
                clubId, "Acu" + apellidos);
        insertar("INSERT INTO deportista_acudientes (club_id, deportista_id, acudiente_id, parentesco, principal) "
                + "VALUES (?, ?, ?, 'Madre', b'1')", clubId, ids[1], acudienteId);
        if (conCierreDeCaja) {
            // REABIERTA (no CERRADA): un dia CERRADO bloquea pagos de ese dia y afectaria a otras pruebas de la clase.
            // Hay un solo cierre por (club, fecha), por eso solo una prueba lo siembra.
            insertar("INSERT INTO cierres_caja (club_id, fecha, estado, total_pagado, efectivo_esperado, nota) "
                    + "VALUES (?, ?, 'REABIERTA', 0, 0, ?)", clubId,
                    java.sql.Date.valueOf(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy()), notaCaja);
        }
        return new Sembrado(ids[0], ids[1], apellidos, numeroFactura, notaCaja);
    }

    private java.util.Map<String, String> exportesDelClub(int clubId) {
        final LocalDate hoy = com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy();
        final LocalDate desde = hoy.minusDays(1);
        final LocalDate hasta = hoy.plusDays(1);
        return TenantContext.runAs(clubId, () -> {
            java.util.Map<String, String> csv = new java.util.LinkedHashMap<>();
            csv.put("deportistas", new String(exportesService.deportistas(), StandardCharsets.UTF_8));
            csv.put("acudientes", new String(exportesService.acudientes(), StandardCharsets.UTF_8));
            csv.put("facturas", new String(exportesService.facturas(desde, hasta), StandardCharsets.UTF_8));
            csv.put("pagos", new String(exportesService.pagos(desde, hasta), StandardCharsets.UTF_8));
            csv.put("cierres_caja", new String(exportesService.cierresCaja(desde, hasta), StandardCharsets.UTF_8));
            return csv;
        });
    }

    @Test
    void losExportesDeUnClubNoIncluyenDatosDeOtroClub() {
        final String s = Long.toString(System.nanoTime(), 36);
        // Los catalogos de tipos y metodos de pago son globales: sirven para los dos clubes.
        final Integer tipoPagoId = TenantContext.runAs(2, () -> tipoPagoRepository.findAll().get(0).getId());
        final Integer metodoPagoId = TenantContext.runAs(2, () -> metodoPagoRepository.findAll().get(0).getId());

        Sembrado club1 = sembrarDatosDeExporte(1, "1" + s, tipoPagoId, metodoPagoId, true);
        Sembrado club2 = sembrarDatosDeExporte(2, "2" + s, tipoPagoId, metodoPagoId, true);

        java.util.Map<String, String> csv1 = exportesDelClub(1);
        java.util.Map<String, String> csv2 = exportesDelClub(2);

        // Marcador de cada exporte: apellido (personas), numero de factura (facturas, pagos) y nota (caja).
        java.util.Map<String, java.util.function.Function<Sembrado, String>> marcador = new java.util.LinkedHashMap<>();
        marcador.put("deportistas", Sembrado::apellidos);
        marcador.put("acudientes", Sembrado::apellidos);
        marcador.put("facturas", Sembrado::numeroFactura);
        marcador.put("pagos", Sembrado::numeroFactura);
        marcador.put("cierres_caja", Sembrado::notaCaja);

        for (var e : marcador.entrySet()) {
            String nombre = e.getKey();
            String propio1 = e.getValue().apply(club1), ajeno1 = e.getValue().apply(club2);
            assertTrue(csv1.get(nombre).contains(propio1), "el exporte '" + nombre + "' del club 1 debe traer sus datos");
            assertFalse(csv1.get(nombre).contains(ajeno1), "el exporte '" + nombre + "' del club 1 NO debe traer datos del club 2");
            assertTrue(csv2.get(nombre).contains(ajeno1), "el exporte '" + nombre + "' del club 2 debe traer sus datos");
            assertFalse(csv2.get(nombre).contains(propio1), "el exporte '" + nombre + "' del club 2 NO debe traer datos del club 1");
        }
    }

    @Test
    void laCopiaDeDatosDelTitularNoCruzaClubesNiMezclaTitulares() {
        final String s = Long.toString(System.nanoTime(), 36);
        final Integer tipoPagoId = TenantContext.runAs(2, () -> tipoPagoRepository.findAll().get(0).getId());
        final Integer metodoPagoId = TenantContext.runAs(2, () -> metodoPagoRepository.findAll().get(0).getId());
        Sembrado club1 = sembrarDatosDeExporte(1, "c1" + s, tipoPagoId, metodoPagoId, false);
        Sembrado club2 = sembrarDatosDeExporte(2, "c2" + s, tipoPagoId, metodoPagoId, false);

        // Un club no puede pedir la copia de una persona de otro club (el id existe, pero no es suyo).
        IllegalArgumentException deClub2EnClub1 = assertThrows(IllegalArgumentException.class,
                () -> TenantContext.runAs(1, () -> copiaDatosService.copia(club2.personaId())));
        assertEquals("Persona no encontrada", deClub2EnClub1.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> TenantContext.runAs(2, () -> copiaDatosService.copia(club1.personaId())));

        // La copia propia trae lo suyo y nada del otro club.
        CopiaDatosService.Copia copia2 = TenantContext.runAs(2, () -> copiaDatosService.copia(club2.personaId()));
        String todo2 = copia2.secciones().stream().flatMap(sec -> sec.filas().stream()).flatMap(List::stream)
                .collect(Collectors.joining("|"));
        assertTrue(todo2.contains(club2.apellidos()), "datos personales del titular");
        assertTrue(todo2.contains("Acu" + club2.apellidos()), "su acudiente");
        assertTrue(todo2.contains(club2.numeroFactura()), "su factura");
        assertFalse(todo2.contains(club1.apellidos()), "nada del titular del otro club");
        assertFalse(todo2.contains(club1.numeroFactura()));
        assertTrue(copia2.secciones().stream().anyMatch(sec -> sec.titulo().startsWith("Historial m")),
                "incluye la seccion de salud, aunque vacia");
    }

    @Test
    void elCupoDelPlanSeExigeEnAltaYReactivacionContraLaBaseReal() {
        final String s = Long.toString(System.nanoTime(), 36);
        final Integer limiteOriginal = jdbc.queryForObject("SELECT limite_deportistas FROM clubes WHERE id = 2", Integer.class);
        final Integer estadoActivo = jdbc.queryForObject("SELECT id FROM estados_persona WHERE estado = 'Activo'", Integer.class);
        final Integer generoId = jdbc.queryForObject("SELECT id FROM generos ORDER BY id LIMIT 1", Integer.class);
        try {
            final long activos = TenantContext.runAs(2, () -> tenantLookup.contarDeportistasActivos(2));

            // 1) Club exactamente lleno: el alta se rechaza y no queda NADA guardado.
            jdbc.update("UPDATE clubes SET limite_deportistas = ? WHERE id = 2", (int) activos);
            int personasAntes = jdbc.queryForObject("SELECT COUNT(*) FROM personas WHERE club_id = 2", Integer.class);
            int usuariosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE club_id = 2", Integer.class);
            RuntimeException lleno = assertThrows(RuntimeException.class,
                    () -> TenantContext.runAs(2, () -> personaService.registrarDeportista(dtoCupo("A" + s, estadoActivo, generoId))));
            assertTrue(lleno.getMessage().contains("Tu plan permite hasta " + activos), lleno.getMessage());
            assertEquals(personasAntes, jdbc.queryForObject("SELECT COUNT(*) FROM personas WHERE club_id = 2", Integer.class));
            assertEquals(usuariosAntes, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE club_id = 2", Integer.class));

            // 2) Con un cupo libre entra uno; con el club otra vez lleno, el siguiente se rechaza.
            jdbc.update("UPDATE clubes SET limite_deportistas = ? WHERE id = 2", (int) activos + 1);
            Deportista a = TenantContext.runAs(2, () -> personaService.registrarDeportista(dtoCupo("B" + s, estadoActivo, generoId)));
            assertNotNull(a.getId());
            assertEquals(activos + 1, (long) TenantContext.runAs(2, () -> tenantLookup.contarDeportistasActivos(2)));
            assertThrows(RuntimeException.class,
                    () -> TenantContext.runAs(2, () -> personaService.registrarDeportista(dtoCupo("C" + s, estadoActivo, generoId))));

            // 3) Retirar libera el cupo: entra otro deportista.
            TenantContext.runAs(2, () -> personaService.eliminarDeportista(a.getId()));
            assertEquals(activos, (long) TenantContext.runAs(2, () -> tenantLookup.contarDeportistasActivos(2)));
            Deportista b = TenantContext.runAs(2, () -> personaService.registrarDeportista(dtoCupo("D" + s, estadoActivo, generoId)));
            assertNotNull(b.getId());

            // 4) Con el club lleno, reactivar al retirado tambien se rechaza y sigue Retirado.
            RuntimeException sinCupo = assertThrows(RuntimeException.class,
                    () -> TenantContext.runAs(2, () -> personaService.reactivarDeportista(a.getId(), "vuelve")));
            assertTrue(sinCupo.getMessage().contains("Tu plan permite hasta"), sinCupo.getMessage());
            assertEquals("Retirado", jdbc.queryForObject(
                    "SELECT ep.estado FROM deportistas d JOIN estados_persona ep ON ep.id = d.estado_id WHERE d.id = ?",
                    String.class, a.getId()));

            // 5) Si el limite sube, la reactivacion pasa.
            jdbc.update("UPDATE clubes SET limite_deportistas = ? WHERE id = 2", (int) activos + 2);
            TenantContext.runAs(2, () -> personaService.reactivarDeportista(a.getId(), "vuelve"));
            assertEquals("Activo", jdbc.queryForObject(
                    "SELECT ep.estado FROM deportistas d JOIN estados_persona ep ON ep.id = d.estado_id WHERE d.id = ?",
                    String.class, a.getId()));
        } finally {
            jdbc.update("UPDATE clubes SET limite_deportistas = ? WHERE id = 2", limiteOriginal);
        }
    }

    private DeportistaDTO dtoCupo(String sufijo, Integer estadoId, Integer generoId) {
        DeportistaDTO d = new DeportistaDTO();
        d.setNombre("ItCupo");
        d.setApellidos("Z" + sufijo);
        d.setNumeroDocumento("IT" + sufijo);
        d.setTipoDocumento("CC");
        d.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        d.setGeneroId(generoId);
        d.setEstadoId(estadoId);
        return d;
    }
}
