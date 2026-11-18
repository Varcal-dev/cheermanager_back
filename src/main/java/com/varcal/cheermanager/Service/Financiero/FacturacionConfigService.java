package com.varcal.cheermanager.Service.Financiero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Financiero.DocumentoElectronico;
import com.varcal.cheermanager.Models.Financiero.FacturacionConfig;
import com.varcal.cheermanager.Service.Financiero.proveedor.ProveedorFacturacion;
import com.varcal.cheermanager.repository.Financiero.DocumentoElectronicoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturacionConfigRepository;

/**
 * Configuracion fiscal del club para facturar electronicamente (DIAN-1). Solo guarda y valida datos: el envio al
 * proveedor llega en DIAN-2. Mientras "activa" sea false el sistema factura como siempre.
 */
@Service
public class FacturacionConfigService {

    private static final int[] PESOS_DV = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};
    private static final Set<String> PROVEEDORES = Set.of("FACTUS");
    private static final Set<String> AMBIENTES = Set.of("PRUEBAS", "PRODUCCION");
    private static final Set<String> EMITIR_AL = Set.of("PAGO", "COBRO");
    private static final Set<String> TIPOS_PERSONA = Set.of("NATURAL", "JURIDICA");
    private static final Set<String> TRATAMIENTOS = Set.of("EXCLUIDO", "GRAVADO");

    /** Lo que el front envia y recibe. */
    public record ConfigDTO(Boolean activa, String proveedor, String ambiente, String emitirAl, String tipoPersona,
            String nit, String dv, String razonSocial, String nombreComercial, Boolean responsableIva,
            String tratamientoIva, BigDecimal tarifaIva, String municipioCodigo, String direccion, String telefono,
            String email, Integer rangoNumeracionId, String prefijo) {
    }

    /** Config + si el servidor tiene credenciales + lo que falta para poder activarla. */
    public record Estado(ConfigDTO config, boolean credencialesConfiguradas, List<String> faltantes) {
    }

    private final FacturacionConfigRepository repo;
    private final DocumentoElectronicoRepository documentos;
    private final List<ProveedorFacturacion> proveedores;

    public FacturacionConfigService(FacturacionConfigRepository repo, DocumentoElectronicoRepository documentos,
            List<ProveedorFacturacion> proveedores) {
        this.repo = repo;
        this.documentos = documentos;
        this.proveedores = proveedores;
    }

    @Transactional(readOnly = true)
    public Estado obtener() {
        FacturacionConfig c = repo.findFirstByOrderByIdAsc().orElseGet(FacturacionConfig::new);
        ConfigDTO dto = aDto(c);
        return new Estado(dto, credenciales(c.getProveedor()), faltantes(dto));
    }

    @Transactional
    public Estado guardar(ConfigDTO d) {
        if (d == null) {
            throw new RuntimeException("Faltan los datos de la configuracion");
        }
        List<String> errores = errores(d);
        if (!errores.isEmpty()) {
            throw new RuntimeException(String.join("; ", errores));
        }
        boolean quiereActivar = Boolean.TRUE.equals(d.activa());
        if (quiereActivar) {
            List<String> falta = faltantes(d);
            if (!falta.isEmpty()) {
                throw new RuntimeException("No se puede activar. Falta: " + String.join(", ", falta));
            }
            if (!credenciales(d.proveedor())) {
                throw new RuntimeException("No se puede activar: el servidor no tiene las credenciales del proveedor");
            }
        }
        FacturacionConfig c = repo.findFirstByOrderByIdAsc().orElseGet(FacturacionConfig::new);
        c.setActiva(quiereActivar);
        c.setProveedor(valor(d.proveedor(), "FACTUS"));
        c.setAmbiente(valor(d.ambiente(), "PRUEBAS"));
        c.setEmitirAl(valor(d.emitirAl(), "PAGO"));
        c.setTipoPersona(limpio(d.tipoPersona()));
        c.setNit(soloDigitos(d.nit()));
        c.setDv(limpio(d.dv()));
        c.setRazonSocial(limpio(d.razonSocial()));
        c.setNombreComercial(limpio(d.nombreComercial()));
        c.setResponsableIva(Boolean.TRUE.equals(d.responsableIva()));
        c.setTratamientoIva(valor(d.tratamientoIva(), "EXCLUIDO"));
        c.setTarifaIva("GRAVADO".equals(c.getTratamientoIva()) && d.tarifaIva() != null ? d.tarifaIva() : BigDecimal.ZERO);
        c.setMunicipioCodigo(limpio(d.municipioCodigo()));
        c.setDireccion(limpio(d.direccion()));
        c.setTelefono(limpio(d.telefono()));
        c.setEmail(limpio(d.email()));
        c.setRangoNumeracionId(d.rangoNumeracionId());
        c.setPrefijo(limpio(d.prefijo()));
        c.setActualizadoEn(LocalDateTime.now());
        repo.save(c);
        return obtener();
    }

    @Transactional(readOnly = true)
    public List<DocumentoElectronico> ultimosDocumentos() {
        return documentos.findTop100ByOrderByIdDesc();
    }

    /** true si el club tiene la facturacion electronica activa (la usaran los siguientes pasos). */
    @Transactional(readOnly = true)
    public boolean activa() {
        return repo.findFirstByOrderByIdAsc().map(c -> Boolean.TRUE.equals(c.getActiva())).orElse(false);
    }

    // ------------------------------------------------------------------ puro (probado sin base de datos)

    /** Digito de verificacion del NIT (algoritmo DIAN, modulo 11). */
    public static int calcularDv(String nit) {
        String n = soloDigitos(nit);
        if (n == null || n.isEmpty() || n.length() > PESOS_DV.length) {
            throw new IllegalArgumentException("NIT invalido");
        }
        int suma = 0;
        for (int i = 0; i < n.length(); i++) {
            int digito = n.charAt(n.length() - 1 - i) - '0';
            suma += digito * PESOS_DV[i];
        }
        int r = suma % 11;
        return r > 1 ? 11 - r : r;
    }

    /** Errores de FORMA (se aplican siempre, aunque no se active). */
    public static List<String> errores(ConfigDTO d) {
        List<String> e = new ArrayList<>();
        if (d.proveedor() != null && !PROVEEDORES.contains(d.proveedor())) {
            e.add("Proveedor no valido");
        }
        if (d.ambiente() != null && !AMBIENTES.contains(d.ambiente())) {
            e.add("Ambiente no valido (PRUEBAS o PRODUCCION)");
        }
        if (d.emitirAl() != null && !EMITIR_AL.contains(d.emitirAl())) {
            e.add("Momento de emision no valido (PAGO o COBRO)");
        }
        if (d.tipoPersona() != null && !d.tipoPersona().isBlank() && !TIPOS_PERSONA.contains(d.tipoPersona())) {
            e.add("Tipo de persona no valido (NATURAL o JURIDICA)");
        }
        if (d.tratamientoIva() != null && !TRATAMIENTOS.contains(d.tratamientoIva())) {
            e.add("Tratamiento de IVA no valido (EXCLUIDO o GRAVADO)");
        }
        if (d.tarifaIva() != null && (d.tarifaIva().signum() < 0 || d.tarifaIva().compareTo(new BigDecimal("100")) > 0)) {
            e.add("La tarifa de IVA debe estar entre 0 y 100");
        }
        String nit = soloDigitos(d.nit());
        if (nit != null && !nit.isEmpty()) {
            if (nit.length() > PESOS_DV.length) {
                e.add("El NIT es demasiado largo");
            } else if (limpio(d.dv()) != null && !String.valueOf(calcularDv(nit)).equals(limpio(d.dv()))) {
                e.add("El digito de verificacion no corresponde al NIT (deberia ser " + calcularDv(nit) + ")");
            }
        }
        if (limpio(d.email()) != null && !limpio(d.email()).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            e.add("El correo no es valido");
        }
        return e;
    }

    /** Datos que faltan para poder ACTIVAR. */
    public static List<String> faltantes(ConfigDTO d) {
        List<String> f = new ArrayList<>();
        if (limpio(d.tipoPersona()) == null) f.add("tipo de persona");
        if (soloDigitos(d.nit()) == null || soloDigitos(d.nit()).isEmpty()) f.add("NIT");
        if (limpio(d.dv()) == null) f.add("digito de verificacion");
        if (limpio(d.razonSocial()) == null) f.add("razon social o nombre");
        if (limpio(d.direccion()) == null) f.add("direccion");
        if (limpio(d.municipioCodigo()) == null) f.add("municipio");
        if (limpio(d.email()) == null) f.add("correo");
        if (d.rangoNumeracionId() == null) f.add("rango de numeracion (resolucion DIAN)");
        if ("GRAVADO".equals(d.tratamientoIva()) && (d.tarifaIva() == null || d.tarifaIva().signum() <= 0)) {
            f.add("tarifa de IVA");
        }
        return f;
    }

    private boolean credenciales(String codigo) {
        String cod = valor(codigo, "FACTUS");
        return proveedores.stream().anyMatch(p -> p.codigo().equals(cod) && p.credencialesConfiguradas());
    }

    private static ConfigDTO aDto(FacturacionConfig c) {
        return new ConfigDTO(c.getActiva(), c.getProveedor(), c.getAmbiente(), c.getEmitirAl(), c.getTipoPersona(),
                c.getNit(), c.getDv(), c.getRazonSocial(), c.getNombreComercial(), c.getResponsableIva(),
                c.getTratamientoIva(), c.getTarifaIva(), c.getMunicipioCodigo(), c.getDireccion(), c.getTelefono(),
                c.getEmail(), c.getRangoNumeracionId(), c.getPrefijo());
    }

    private static String valor(String v, String defecto) {
        return limpio(v) == null ? defecto : v.trim();
    }

    private static String limpio(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String soloDigitos(String v) {
        return v == null ? null : v.replaceAll("[^0-9]", "");
    }
}
