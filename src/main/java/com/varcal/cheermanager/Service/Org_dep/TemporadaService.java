package com.varcal.cheermanager.Service.Org_dep;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Org_dep.Temporada;
import com.varcal.cheermanager.repository.Org_dep.TemporadaRepository;

@Service
public class TemporadaService {

    public record TemporadaRequest(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
    }

    private final TemporadaRepository repo;

    public TemporadaService(TemporadaRepository repo) {
        this.repo = repo;
    }

    public List<Temporada> listar() {
        return repo.findAllByOrderByFechaInicioDesc();
    }

    public Optional<Temporada> activa() {
        return repo.findFirstByActivaTrue();
    }

    @Transactional
    public Temporada crear(TemporadaRequest req) {
        validar(req);
        if (repo.existsByNombre(req.nombre().trim())) {
            throw new RuntimeException("Ya existe una temporada con ese nombre");
        }
        Temporada t = new Temporada();
        t.setNombre(req.nombre().trim());
        t.setFechaInicio(req.fechaInicio());
        t.setFechaFin(req.fechaFin());
        t.setActiva(false);
        return repo.save(t);
    }

    @Transactional
    public Temporada actualizar(Integer id, TemporadaRequest req) {
        validar(req);
        Temporada t = obtener(id);
        repo.findAll().stream().filter(o -> o.getNombre().equals(req.nombre().trim()) && !o.getId().equals(id))
                .findAny().ifPresent(o -> {
                    throw new RuntimeException("Ya existe otra temporada con ese nombre");
                });
        t.setNombre(req.nombre().trim());
        t.setFechaInicio(req.fechaInicio());
        t.setFechaFin(req.fechaFin());
        return repo.save(t);
    }

    /** Activa una temporada y desactiva las demas: siempre hay a lo sumo una activa. */
    @Transactional
    public Temporada activar(Integer id) {
        Temporada t = obtener(id);
        List<Temporada> activas = repo.findByActivaTrue();
        activas.forEach(o -> o.setActiva(false));
        repo.saveAll(activas);
        t.setActiva(true);
        return repo.save(t);
    }

    @Transactional
    public Temporada cerrar(Integer id) {
        Temporada t = obtener(id);
        t.setActiva(false);
        return repo.save(t);
    }

    public Temporada obtener(Integer id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Temporada no encontrada con ID: " + id));
    }

    private void validar(TemporadaRequest r) {
        if (r == null || r.nombre() == null || r.nombre().isBlank() || r.fechaInicio() == null
                || r.fechaFin() == null) {
            throw new RuntimeException("Nombre, fecha de inicio y fecha de fin son obligatorios");
        }
        if (r.fechaFin().isBefore(r.fechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }
}
