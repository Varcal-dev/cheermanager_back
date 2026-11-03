package com.varcal.cheermanager.Service.Importacion;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.Models.Financiero.Convenio;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Genero;
import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.Service.Persona.AcudienteService;
import com.varcal.cheermanager.Service.Persona.PersonaService;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.EstadoPersonaRepository;
import com.varcal.cheermanager.repository.Personas.GeneroRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * Importacion masiva de deportistas (y de su acudiente principal) desde un CSV.
 *
 * Dos pasos: validar (no guarda nada, devuelve el informe fila por fila) e importar (guarda las filas correctas y
 * deja las incorrectas en el informe). Cada fila se guarda en su propia transaccion (PersonaService.registrarDeportista
 * es @Transactional), asi una fila mala no tumba a las demas. Esta clase NO es @Transactional a proposito.
 *
 * Reutiliza registrarDeportista: por eso se respeta el limite de deportistas del plan, se valida el documento unico,
 * se deja el nivel inicial en el historial y se crea el usuario del deportista igual que al registrarlo a mano.
 * Los consentimientos NO se importan: se recogen aparte (modulo Consentimientos).
 */
@Service
public class ImportacionDeportistasService {

    private static final Logger log = LoggerFactory.getLogger(ImportacionDeportistasService.class);

    public static final int MAX_FILAS = 1000;
    public static final int MAX_BYTES = 1_048_576;

    public static final List<String> COLUMNAS = List.of("nombre", "apellidos", "tipo_documento", "numero_documento",
            "fecha_nacimiento", "genero", "estado", "nivel", "convenio", "altura", "peso", "telefono", "direccion",
            "contacto_emergencia", "acudiente_nombre", "acudiente_apellidos", "acudiente_tipo_documento",
            "acudiente_documento", "acudiente_telefono", "acudiente_email", "acudiente_parentesco");

    static final Set<String> OBLIGATORIAS = Set.of("nombre", "apellidos", "numero_documento", "fecha_nacimiento");
    static final String ESTADO_POR_DEFECTO = "Activo";
    static final String ESTADO_RETIRADO = "Retirado";

    public record Fila(int numero, String nombre, String documento, String estado, List<String> errores,
            List<String> avisos) {
    }

    /** errores = problemas del archivo completo (columnas, tamano). estado de cada fila: OK, ERROR o IMPORTADA. */
    public record Resultado(boolean soloValidacion, int total, int validas, int conErrores, int importadas,
            Integer cupoDisponible, List<String> errores, List<Fila> filas) {
    }

    private final PersonaService personaService;
    private final AcudienteService acudienteService;
    private final SuscripcionService suscripcionService;
    private final PersonaRepository personaRepository;
    private final AcudienteRepository acudienteRepository;
    private final GeneroRepository generoRepository;
    private final EstadoPersonaRepository estadoRepository;
    private final NivelRepository nivelRepository;
    private final ConvenioRepository convenioRepository;

    public ImportacionDeportistasService(PersonaService personaService, AcudienteService acudienteService,
            SuscripcionService suscripcionService, PersonaRepository personaRepository,
            AcudienteRepository acudienteRepository, GeneroRepository generoRepository,
            EstadoPersonaRepository estadoRepository, NivelRepository nivelRepository,
            ConvenioRepository convenioRepository) {
        this.personaService = personaService;
        this.acudienteService = acudienteService;
        this.suscripcionService = suscripcionService;
        this.personaRepository = personaRepository;
        this.acudienteRepository = acudienteRepository;
        this.generoRepository = generoRepository;
        this.estadoRepository = estadoRepository;
        this.nivelRepository = nivelRepository;
        this.convenioRepository = convenioRepository;
    }

    /** Solo el encabezado: las columnas que se aceptan. La guia de cada una se muestra en la pantalla. */
    public byte[] plantilla() {
        return CsvUtil.escribir(List.of(COLUMNAS));
    }

    public Resultado validar(byte[] datos) {
        return procesar(datos, false);
    }

    public Resultado importar(byte[] datos) {
        Resultado r = procesar(datos, true);
        log.info("IMPORTACION deportistas club={} filas={} importadas={} conErrores={}", TenantContext.require(),
                r.total(), r.importadas(), r.conErrores());
        return r;
    }

    // ------------------------------------------------------------------ proceso

    private Resultado procesar(byte[] datos, boolean guardar) {
        List<String> globales = new ArrayList<>();
        if (datos == null || datos.length == 0) {
            return rechazo(guardar, "El archivo esta vacio");
        }
        if (datos.length > MAX_BYTES) {
            return rechazo(guardar, "El archivo pesa mas de 1 MB. Dividelo en varios archivos");
        }
        List<String[]> filas = CsvUtil.leer(datos);
        if (filas.size() < 2) {
            return rechazo(guardar, "El archivo no tiene filas de datos (solo el encabezado o nada)");
        }
        if (filas.size() - 1 > MAX_FILAS) {
            return rechazo(guardar, "El archivo tiene mas de " + MAX_FILAS + " filas. Dividelo en varios archivos");
        }

        // --- encabezado -> posicion de cada columna
        String[] enc = filas.get(0);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < enc.length; i++) {
            String nombre = ImportacionParseo.nombreColumna(enc[i]);
            if (nombre.isEmpty()) {
                continue;
            }
            if (!COLUMNAS.contains(nombre)) {
                globales.add("La columna '" + enc[i].trim() + "' no se reconoce y se ignorara");
            } else if (col.putIfAbsent(nombre, i) != null) {
                globales.add("La columna '" + nombre + "' esta repetida; se usa la primera");
            }
        }
        List<String> faltan = OBLIGATORIAS.stream().filter(c -> !col.containsKey(c)).sorted().toList();
        if (!faltan.isEmpty()) {
            globales.add(0, "Faltan columnas obligatorias: " + String.join(", ", faltan));
            return new Resultado(!guardar, filas.size() - 1, 0, filas.size() - 1, 0, null, globales, List.of());
        }

        // --- catalogos del club (una sola lectura)
        Catalogo<Genero> generos = indexar(generoRepository.findAll(), Genero::getGenero);
        Catalogo<EstadoPersona> estados = indexar(estadoRepository.findAll(), EstadoPersona::getEstado);
        Catalogo<Nivel> niveles = indexar(nivelRepository.findAll(), Nivel::getNombre);
        Catalogo<Convenio> convenios = indexar(convenioRepository.findAll(), Convenio::getNombreEmpresa);
        Integer cupo = suscripcionService.cupoDisponible();
        LocalDate hoy = SuscripcionService.hoy();

        Set<String> docsEnArchivo = new HashSet<>();
        List<Fila> informe = new ArrayList<>();
        int validas = 0, conErrores = 0, importadas = 0, aceptadas = 0;

        for (int i = 1; i < filas.size(); i++) {
            String[] f = filas.get(i);
            int numeroFila = i + 1; // la fila 1 del archivo es el encabezado
            List<String> errores = new ArrayList<>();
            List<String> avisos = new ArrayList<>();
            Lector v = new Lector(f, col);

            DeportistaDTO dto = new DeportistaDTO();
            dto.setNombre(limpiarNombre(v.get("nombre")));
            dto.setApellidos(limpiarNombre(v.get("apellidos")));
            if (dto.getNombre() == null) errores.add("El nombre es obligatorio");
            if (dto.getApellidos() == null) errores.add("Los apellidos son obligatorios");

            String doc = ImportacionParseo.documento(v.get("numero_documento"));
            dto.setNumeroDocumento(doc);
            if (doc == null) {
                errores.add("El numero de documento es obligatorio");
            } else if (!docsEnArchivo.add(doc)) {
                errores.add("El documento " + doc + " esta repetido en el archivo");
            } else if (personaRepository.findByNumeroDocumento(doc).isPresent()) {
                errores.add("El documento " + doc + " ya esta registrado en el sistema");
            }
            String tipoDoc = v.get("tipo_documento");
            dto.setTipoDocumento(tipoDoc == null ? null : recortar(tipoDoc.toUpperCase(), 20));

            LocalDate nacimiento = null;
            if (v.get("fecha_nacimiento") == null) {
                errores.add("La fecha de nacimiento es obligatoria");
            } else {
                try {
                    nacimiento = ImportacionParseo.fecha(v.get("fecha_nacimiento"), hoy);
                    dto.setFechaNacimiento(nacimiento);
                } catch (IllegalArgumentException e) {
                    errores.add(e.getMessage());
                }
            }

            dto.setGeneroId(buscarId(v.get("genero"), generos, "Genero", errores, Genero::getId));
            if (col.containsKey("genero") && v.get("genero") == null) {
                avisos.add("Sin genero");
            }

            String estadoTxt = v.get("estado") == null ? ESTADO_POR_DEFECTO : v.get("estado");
            EstadoPersona estado = estados.buscar(estadoTxt);
            if (estado == null) {
                errores.add("Estado '" + estadoTxt + "' no existe. Opciones: " + estados.opciones());
            } else if (ESTADO_RETIRADO.equalsIgnoreCase(estado.getEstado())) {
                errores.add("No se importa en estado Retirado: importalo como Activo y usa la opcion Retirar");
            } else {
                dto.setEstadoId(estado.getId());
            }

            dto.setNivelActualId(buscarId(v.get("nivel"), niveles, "Nivel", errores, Nivel::getId));
            dto.setConvenioId(buscarId(v.get("convenio"), convenios, "Convenio", errores, Convenio::getId));
            numero(v, "altura", 30, 260, dto::setAltura, errores);
            numero(v, "peso", 5, 300, dto::setPeso, errores);
            dto.setTelefono(recortar(v.get("telefono"), 30));
            dto.setDireccion(recortar(v.get("direccion"), 200));
            dto.setContactoEmergencia(recortar(v.get("contacto_emergencia"), 200));

            AcudienteDatos acu = acudiente(v, errores, avisos);
            if (acu == null && nacimiento != null && Period.between(nacimiento, hoy).getYears() < 18) {
                avisos.add("Menor de edad sin acudiente: no podra tener acceso al portal ni autorizacion firmada");
            }

            String estadoFila;
            if (errores.isEmpty() && cupo != null && aceptadas >= cupo) {
                errores.add("Excede el cupo de deportistas del plan (cupos libres al inicio: " + cupo + ")");
            }
            if (!errores.isEmpty()) {
                estadoFila = "ERROR";
                conErrores++;
            } else {
                aceptadas++;
                validas++;
                estadoFila = "OK";
                if (guardar) {
                    try {
                        Deportista d = personaService.registrarDeportista(dto);
                        importadas++;
                        estadoFila = "IMPORTADA";
                        if (acu != null) {
                            vincularAcudiente(d.getId(), acu, avisos);
                        }
                    } catch (RuntimeException e) {
                        // si fallo el registro del deportista no quedo nada guardado de esta fila
                        if (!"IMPORTADA".equals(estadoFila)) {
                            validas--;
                            conErrores++;
                            estadoFila = "ERROR";
                            errores.add(e.getMessage() == null ? "No se pudo guardar la fila" : e.getMessage());
                        } else {
                            avisos.add("El deportista se importo, pero no se pudo registrar al acudiente: "
                                    + e.getMessage());
                        }
                    }
                }
            }
            String nombreCompleto = ((dto.getNombre() == null ? "" : dto.getNombre()) + " "
                    + (dto.getApellidos() == null ? "" : dto.getApellidos())).trim();
            informe.add(new Fila(numeroFila, nombreCompleto, doc, estadoFila, errores, avisos));
        }
        return new Resultado(!guardar, filas.size() - 1, validas, conErrores, importadas, cupo, globales, informe);
    }

    // ------------------------------------------------------------------ acudiente

    private record AcudienteDatos(String nombre, String apellidos, String tipoDocumento, String documento,
            String telefono, String email, String parentesco) {
    }

    /** null si la fila no trae datos de acudiente. Agrega errores/avisos si vienen incompletos. */
    private AcudienteDatos acudiente(Lector v, List<String> errores, List<String> avisos) {
        String nombre = limpiarNombre(v.get("acudiente_nombre"));
        String apellidos = limpiarNombre(v.get("acudiente_apellidos"));
        String tipo = v.get("acudiente_tipo_documento");
        String doc = ImportacionParseo.documento(v.get("acudiente_documento"));
        String tel = recortar(v.get("acudiente_telefono"), 30);
        String email = recortar(v.get("acudiente_email"), 150);
        String parentesco = recortar(v.get("acudiente_parentesco"), 40);
        if (nombre == null && apellidos == null && doc == null && tel == null && email == null) {
            return null;
        }
        if (nombre == null || apellidos == null) {
            errores.add("Acudiente: nombre y apellidos son obligatorios si se indica un acudiente");
        }
        if (tel == null && email == null) {
            errores.add("Acudiente: indica al menos un telefono o un correo");
        }
        if (email != null && !ImportacionParseo.correoPlausible(email)) {
            errores.add("Acudiente: el correo '" + email + "' no parece valido");
        }
        if (doc == null) {
            avisos.add("Acudiente sin documento: se creara uno nuevo (si ya existe quedara duplicado)");
        } else if (acudienteRepository.findByNumeroDocumento(doc).isPresent()) {
            avisos.add("El acudiente con documento " + doc + " ya existe: se vinculara al deportista");
        }
        return new AcudienteDatos(nombre, apellidos, tipo == null ? null : recortar(tipo.toUpperCase(), 20), doc, tel,
                email, parentesco);
    }

    private void vincularAcudiente(Integer deportistaId, AcudienteDatos a, List<String> avisos) {
        Acudiente existente = a.documento() == null ? null
                : acudienteRepository.findByNumeroDocumento(a.documento()).orElse(null);
        Acudiente acudiente = existente != null ? existente
                : acudienteService.crear(new AcudienteService.AcudienteRequest(a.nombre(), a.apellidos(),
                        a.tipoDocumento(), a.documento(), a.telefono(), a.email(), null));
        acudienteService.vincular(deportistaId,
                new AcudienteService.VinculoRequest(acudiente.getId(), a.parentesco(), true, false));
    }

    // ------------------------------------------------------------------ utilidades

    /** Lee una celda por nombre de columna; vacio o columna ausente -> null. */
    private static final class Lector {
        private final String[] fila;
        private final Map<String, Integer> col;

        Lector(String[] fila, Map<String, Integer> col) {
            this.fila = fila;
            this.col = col;
        }

        String get(String columna) {
            Integer i = col.get(columna);
            if (i == null || i >= fila.length) {
                return null;
            }
            String s = fila[i].trim();
            return s.isEmpty() ? null : s;
        }
    }

    /** Un catalogo del club buscable por nombre sin importar mayusculas ni tildes, que recuerda los nombres originales. */
    private static final class Catalogo<T> {
        private final Map<String, T> porClave = new LinkedHashMap<>();
        private final List<String> nombres = new ArrayList<>();

        T buscar(String texto) {
            return porClave.get(ImportacionParseo.normalizar(texto));
        }

        String opciones() {
            String lista = nombres.stream().limit(8).collect(Collectors.joining(", "));
            return nombres.size() > 8 ? lista + "..." : lista;
        }
    }

    private static <T> Catalogo<T> indexar(List<T> items, java.util.function.Function<T, String> nombre) {
        Catalogo<T> c = new Catalogo<>();
        for (T t : items) {
            String n = nombre.apply(t);
            if (n != null && c.porClave.putIfAbsent(ImportacionParseo.normalizar(n), t) == null) {
                c.nombres.add(n);
            }
        }
        return c;
    }

    private static <T> Integer buscarId(String texto, Catalogo<T> catalogo, String etiqueta, List<String> errores,
            java.util.function.Function<T, Integer> id) {
        if (texto == null) {
            return null;
        }
        T t = catalogo.buscar(texto);
        if (t == null) {
            errores.add(etiqueta + " '" + texto + "' no existe. Opciones: " + catalogo.opciones());
            return null;
        }
        return id.apply(t);
    }

    private static void numero(Lector v, String columna, int min, int max,
            java.util.function.Consumer<Integer> destino, List<String> errores) {
        String txt = v.get(columna);
        if (txt == null) {
            return;
        }
        try {
            destino.accept(ImportacionParseo.entero(txt, columna, min, max));
        } catch (IllegalArgumentException e) {
            errores.add(e.getMessage());
        }
    }

    private static String limpiarNombre(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : (t.length() > 100 ? t.substring(0, 100) : t);
    }

    private static String recortar(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }

    private Resultado rechazo(boolean guardar, String mensaje) {
        return new Resultado(!guardar, 0, 0, 0, 0, null, List.of(mensaje), List.of());
    }
}
