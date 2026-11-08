package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista.Tipo;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Persona.DocumentoDeportistaService.Alertas;
import com.varcal.cheermanager.Service.Persona.DocumentoDeportistaService.Estado;
import com.varcal.cheermanager.Service.Persona.DocumentoDeportistaService.Solicitud;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.DocumentoDeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C4: vencimientos de documentos con alertas. */
class DocumentoDeportistaServiceTest {

    final LocalDate hoy = LocalDate.of(2026, 11, 8);
    final List<DocumentoDeportista> docs = new ArrayList<>();
    final List<Deportista> deportistas = new ArrayList<>();

    final DocumentoDeportistaService service = new DocumentoDeportistaService(
            Fakes.proxy(DocumentoDeportistaRepository.class, new Repo()
                    .on("save", a -> {
                        DocumentoDeportista d = (DocumentoDeportista) a[0];
                        d.setId(docs.size() + 1);
                        docs.add(d);
                        return d;
                    })
                    .on("findAllByOrderByIdDesc", a -> {
                        List<DocumentoDeportista> r = new ArrayList<>(docs);
                        java.util.Collections.reverse(r);
                        return r;
                    })
                    .on("findByDeportistaIdOrderByIdDesc", a -> {
                        List<DocumentoDeportista> r = new ArrayList<>();
                        for (DocumentoDeportista d : docs) if (d.getDeportistaId().equals(a[0])) r.add(0, d);
                        return r;
                    })),
            Fakes.proxy(DeportistaRepository.class, new Repo()
                    .on("findAll", a -> deportistas)
                    .on("findById", a -> deportistas.stream().filter(d -> d.getId().equals(a[0])).findFirst()))) {
        @Override
        protected LocalDate hoy() {
            return hoy;
        }
    };

    void dep(int id, String nombre, String estado) {
        Persona p = new Persona();
        p.setId(100 + id);
        p.setNombre(nombre);
        p.setApellidos("X");
        EstadoPersona e = new EstadoPersona();
        e.setEstado(estado);
        Deportista d = new Deportista();
        d.setId(id);
        d.setPersona(p);
        d.setEstado(e);
        deportistas.add(d);
    }

    void reiniciar() {
        docs.clear();
        deportistas.clear();
        dep(1, "Ana", "Activo");
        dep(2, "Bea", "Activo");
        dep(3, "Cami", "Retirado");
    }

    @Test
    void sinRegistroSoloParaActivosYTiposRequeridos() {
        reiniciar();
        Alertas a = service.alertas(30, null, false);
        assertEquals(4, a.sinRegistro()); // 2 activos x (certificado + EPS); el retirado no cuenta
        assertEquals(4, a.items().size());
        assertTrue(a.items().stream().noneMatch(i -> i.deportistaId() == 3));
    }

    @Test
    void clasificaVencidoPorVencerYVigente() {
        reiniciar();
        service.registrar(1, new Solicitud(Tipo.CERTIFICADO_MEDICO, "Clinica", null, null, hoy.minusDays(1), null), 9);
        service.registrar(2, new Solicitud(Tipo.CERTIFICADO_MEDICO, null, null, null, hoy.plusDays(30), null), 9);
        service.registrar(1, new Solicitud(Tipo.EPS, "Sanitas", "123", null, null, null), 9); // sin vencimiento
        service.registrar(2, new Solicitud(Tipo.EPS, null, null, null, hoy.plusDays(31), null), 9);
        Alertas a = service.alertas(30, null, false);
        assertEquals(1, a.vencidos());
        assertEquals(1, a.porVencer());
        assertEquals(0, a.sinRegistro());
        assertEquals(Estado.VENCIDO, a.items().get(0).estado()); // vencidos primero
        assertEquals(-1, a.items().get(0).diasRestantes());
        assertEquals(4, service.alertas(30, null, true).items().size());
    }

    @Test
    void elUltimoRegistroReemplazaAlAnterior() {
        reiniciar();
        service.registrar(1, new Solicitud(Tipo.CERTIFICADO_MEDICO, null, null, null, hoy.minusDays(10), null), 9);
        service.registrar(1, new Solicitud(Tipo.CERTIFICADO_MEDICO, null, null, null, hoy.plusDays(300), null), 9);
        Alertas a = service.alertas(30, Set.of(Tipo.CERTIFICADO_MEDICO), false);
        assertEquals(0, a.vencidos());
        assertEquals(1, a.sinRegistro()); // Bea no tiene
        assertEquals(2, service.historial(1).size());
        assertEquals(hoy.plusDays(300), service.historial(1).get(0).fechaVencimiento());
    }

    @Test
    void tiposOpcionalesSoloAlertanSiExisten() {
        reiniciar();
        assertEquals(0, service.alertas(30, Set.of(), false).items().size());
        service.registrar(1, new Solicitud(Tipo.LICENCIA, "Federacion", "L-1", null, hoy.minusDays(5), null), 9);
        Alertas a = service.alertas(30, Set.of(), false);
        assertEquals(1, a.items().size());
        assertEquals(Tipo.LICENCIA, a.items().get(0).tipo());
    }

    @Test
    void validaRegistro() {
        reiniciar();
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(null, null, null, null, hoy, null), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(99, new Solicitud(Tipo.EPS, null, null, null, null, null), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(Tipo.CERTIFICADO_MEDICO, null, null, null, null, null), 9)); // exige vencimiento
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(Tipo.LICENCIA, null, null, hoy.plusDays(1), hoy.plusDays(9), null), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(Tipo.LICENCIA, null, null, hoy, hoy.minusDays(1), null), 9));
        assertThrows(IllegalArgumentException.class, () -> service.alertas(400, null, false));
        assertTrue(docs.isEmpty());
    }
}
