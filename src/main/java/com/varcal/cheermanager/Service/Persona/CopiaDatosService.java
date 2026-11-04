package com.varcal.cheermanager.Service.Persona;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.ObjetivoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Historiales.HistorialMedicoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

/**
 * L6: copia de TODOS los datos que el club tiene de una persona (derecho de acceso, Ley 1581 art. 8).
 * Devuelve secciones genericas (titulo + columnas + filas de texto) para que el front las muestre, las imprima
 * o las descargue como CSV sin conocer cada modelo. Es de SOLO LECTURA y debe usarla quien atiende la solicitud
 * del titular (permiso exportar_datos_titular); incluye datos de salud, por eso no se reparte a roles comunes.
 * Limite: cubre a la persona y, si es deportista, su ficha deportiva, acudientes, inscripciones, asistencia,
 * evaluaciones y objetivos. Un acudiente sin ficha de deportista aparece dentro de la copia de su deportista.
 */
@Service
public class CopiaDatosService {

    public record Seccion(String titulo, List<String> columnas, List<List<String>> filas) {
        public String getTitulo() { return titulo; }
        public List<String> getColumnas() { return columnas; }
        public List<List<String>> getFilas() { return filas; }
    }

    public record Copia(String titular, LocalDateTime generadoEn, List<Seccion> secciones) {
        public String getTitular() { return titular; }
        public LocalDateTime getGeneradoEn() { return generadoEn; }
        public List<Seccion> getSecciones() { return secciones; }
    }

    private final PersonaRepository personaRepository;
    private final DeportistaRepository deportistaRepository;
    private final DeportistaAcudienteRepository vinculoRepository;
    private final ConsentimientoService consentimientos;
    private final HistorialMedicoRepository historialRepository;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final ObjetivoRepository objetivoRepository;

    public CopiaDatosService(PersonaRepository personaRepository, DeportistaRepository deportistaRepository,
            DeportistaAcudienteRepository vinculoRepository, ConsentimientoService consentimientos,
            HistorialMedicoRepository historialRepository, FacturaRepository facturaRepository,
            PagoRepository pagoRepository, InscripcionRepository inscripcionRepository,
            AsistenciaRepository asistenciaRepository, EvaluacionRepository evaluacionRepository,
            ObjetivoRepository objetivoRepository) {
        this.personaRepository = personaRepository;
        this.deportistaRepository = deportistaRepository;
        this.vinculoRepository = vinculoRepository;
        this.consentimientos = consentimientos;
        this.historialRepository = historialRepository;
        this.facturaRepository = facturaRepository;
        this.pagoRepository = pagoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.objetivoRepository = objetivoRepository;
    }

    public Copia copia(Integer personaId) {
        Persona p = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        List<Seccion> out = new ArrayList<>();

        out.add(seccion("Datos personales", List.of("Campo", "Valor"), List.of(
                fila("Nombre", p.getNombre()), fila("Apellidos", p.getApellidos()),
                fila("Tipo de documento", p.getTipoDocumento()), fila("Número de documento", p.getNumeroDocumento()),
                fila("Fecha de nacimiento", p.getFechaNacimiento()), fila("Dirección", p.getDireccion()),
                fila("Teléfono", p.getTelefono()))));

        Deportista d = deportistaRepository.findByPersonaId(personaId).orElse(null);
        if (d != null) {
            out.add(seccion("Ficha deportiva", List.of("Campo", "Valor"), List.of(
                    fila("Estado", d.getEstado() == null ? null : d.getEstado().getEstado()),
                    fila("Fecha de registro", d.getFechaRegistro()), fila("Altura (cm)", d.getAltura()),
                    fila("Peso (kg)", d.getPeso()), fila("Contacto de emergencia", d.getContactoEmergencia()))));

            List<List<String>> acudientes = new ArrayList<>();
            vinculoRepository.findByDeportistaId(d.getId()).forEach(v -> acudientes.add(fila(
                    v.getAcudiente().getNombre() + " " + v.getAcudiente().getApellidos(), v.getParentesco(),
                    v.getAcudiente().getTipoDocumento() + " " + v.getAcudiente().getNumeroDocumento(),
                    v.getAcudiente().getTelefono(), v.getAcudiente().getEmail(),
                    Boolean.TRUE.equals(v.getPrincipal()) ? "Sí" : "No")));
            out.add(seccion("Acudientes", List.of("Nombre", "Parentesco", "Documento", "Teléfono", "Correo", "Principal"),
                    acudientes));

            List<List<String>> insc = new ArrayList<>();
            inscripcionRepository.findByDeportistaId(d.getId().longValue()).forEach(i -> insc.add(fila(
                    i.getTemporada() == null ? null : i.getTemporada().getNombre(), i.getEstado(),
                    i.getFechaInscripcion(), i.getFechaVencimiento())));
            out.add(seccion("Inscripciones", List.of("Temporada", "Estado", "Fecha inscripción", "Vence"), insc));

            List<List<String>> asist = new ArrayList<>();
            asistenciaRepository.findByDeportistaIdOrderByFechaDesc(d.getId()).forEach(a -> asist.add(fila(a.getFecha(),
                    a.getEstadoAsistencia() == null ? null : a.getEstadoAsistencia().getEstado())));
            out.add(seccion("Asistencia", List.of("Fecha", "Estado"), asist));

            List<List<String>> evals = new ArrayList<>();
            evaluacionRepository.findByDeportistaIdOrderByFechaDesc(d.getId()).forEach(e -> evals.add(fila(e.getFecha(),
                    e.getCriterioEvaluacion() == null ? null : e.getCriterioEvaluacion().getNombre(),
                    e.getPuntajeObtenido())));
            out.add(seccion("Evaluaciones", List.of("Fecha", "Criterio", "Puntaje"), evals));

            List<List<String>> objs = new ArrayList<>();
            objetivoRepository.findByDeportistaIdOrderByFechaCreacionDesc(d.getId()).forEach(o -> objs.add(fila(
                    o.getFechaCreacion(), o.getNombre(), o.getDescripcion(),
                    o.getEstadoObjetivo() == null ? null : o.getEstadoObjetivo().getNombreEstado())));
            out.add(seccion("Objetivos", List.of("Fecha", "Objetivo", "Descripción", "Estado"), objs));
        }

        List<List<String>> salud = new ArrayList<>();
        historialRepository.findByPersonaId(personaId, Pageable.unpaged()).getContent().forEach(h -> salud.add(fila(
                h.getFechaRegistro(), h.getTipoRegistro(), h.getDescripcion(), h.getGravedad(), h.getMedicoTratante())));
        out.add(seccion("Historial médico (dato sensible)",
                List.of("Fecha", "Tipo", "Descripción", "Gravedad", "Médico tratante"), salud));

        List<List<String>> cons = new ArrayList<>();
        consentimientos.listarDePersona(personaId).forEach(c -> cons.add(fila(c.tipo(), c.versionTexto(), c.canal(),
                c.fechaOtorgado(), c.fechaRevocado(), c.motivoRevocacion())));
        out.add(seccion("Autorizaciones (consentimientos)",
                List.of("Tipo", "Versión del texto", "Canal", "Otorgado", "Revocado", "Motivo de revocatoria"), cons));

        List<List<String>> facturas = new ArrayList<>();
        facturaRepository.findByPersonaIdOrderByFechaEmisionDesc(personaId).forEach(f -> facturas.add(fila(
                f.getNumeroFactura(), f.getFechaEmision(), f.getConcepto() != null ? f.getConcepto() : f.getDescripcion(),
                f.getTotal(), Boolean.TRUE.equals(f.getAnulada()) ? "Anulada: " + nz(f.getMotivoAnulacion()) : "Vigente")));
        out.add(seccion("Facturas", List.of("Número", "Fecha", "Concepto", "Total", "Estado"), facturas));

        List<List<String>> pagos = new ArrayList<>();
        pagoRepository.findByPersonaId(personaId).forEach(pg -> pagos.add(fila(pg.getFecha(),
                pg.getFactura() == null ? null : pg.getFactura().getNumeroFactura(), pg.getMonto(), pg.getEstado(),
                pg.getMetodoPago() == null ? null : pg.getMetodoPago().getMetodo(), pg.getReferencia())));
        out.add(seccion("Pagos", List.of("Fecha", "Factura", "Monto", "Estado", "Método", "Referencia"), pagos));

        return new Copia(p.getNombre() + " " + p.getApellidos(), LocalDateTime.now(), out);
    }

    // ------------------------------------------------------------------ utilidades

    private static Seccion seccion(String titulo, List<String> columnas, List<List<String>> filas) {
        return new Seccion(titulo, columnas, filas);
    }

    private static List<String> fila(Object... celdas) {
        List<String> r = new ArrayList<>();
        for (Object c : celdas) r.add(c == null ? "" : String.valueOf(c));
        return r;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
