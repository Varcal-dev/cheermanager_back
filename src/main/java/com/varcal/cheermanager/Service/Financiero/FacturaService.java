package com.varcal.cheermanager.Service.Financiero;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Financiero.FacturaDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Financiero.DescuentoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final PersonaRepository personaRepository;
    private final DescuentoRepository descuentoRepository;

    public FacturaService(FacturaRepository facturaRepository,
                          PersonaRepository personaRepository,
                          DescuentoRepository descuentoRepository) {
        this.facturaRepository = facturaRepository;
        this.personaRepository = personaRepository;
        this.descuentoRepository = descuentoRepository;
    }

    public Factura crearFactura(FacturaDTO dto) {
        Persona persona = personaRepository.findById(dto.getPersonaId())
            .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

        Descuento descuento = descuentoRepository.findById(dto.getDescuentoId())
            .orElseThrow(() -> new RuntimeException("Descuento no encontrado"));

        Factura factura = new Factura();
        factura.setNumeroFactura(dto.getNumeroFactura());
        factura.setFechaEmision(dto.getFechaEmision());
        factura.setPersona(persona);
        factura.setDescripcion(dto.getDescripcion());
        factura.setDescuento(descuento);
        factura.setTotal(dto.getTotal());

        return facturaRepository.save(factura);
    }

    // Se mantiene para uso interno (ej. InscripcionService necesita la
    // entidad completa para buscarla por inscripcionId), pero ya no se
    // expone directamente en ningún Controller.
    public List<Factura> listar() {
        return facturaRepository.findAll();
    }

    public Optional<Factura> buscarPorId(Integer id) {
        return facturaRepository.findById(id);
    }

    public void eliminar(Integer id) {
        facturaRepository.deleteById(id);
    }

    // ---------- Métodos usados por los Controllers (devuelven DTO plano) ----------

    public List<FacturaResponseDTO> listarDTO() {
        return facturaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public Optional<FacturaResponseDTO> buscarDTOPorId(Integer id) {
        return facturaRepository.findById(id).map(this::toResponseDTO);
    }

    // Antes era privado. Ahora público porque InscripcionController también
    // lo necesita para su endpoint GET /api/inscripciones/{id}/factura —
    // ese endpoint devolvía la entidad Factura cruda (con factura.inscripcion
    // completo incluido), en vez de pasar por este mismo DTO.
    public FacturaResponseDTO toResponseDTO(Factura factura) {
        FacturaResponseDTO dto = new FacturaResponseDTO();
        dto.setId(factura.getId());
        dto.setNumeroFactura(factura.getNumeroFactura());
        dto.setFechaEmision(factura.getFechaEmision());
        dto.setDescripcion(factura.getDescripcion());
        dto.setTotal(factura.getTotal());

        if (factura.getPersona() != null) {
            FacturaResponseDTO.PersonaResumenDTO persona = new FacturaResponseDTO.PersonaResumenDTO();
            persona.setId(factura.getPersona().getId());
            persona.setNombre(factura.getPersona().getNombre());
            persona.setApellidos(factura.getPersona().getApellidos());
            dto.setPersona(persona);
        }

        if (factura.getDescuento() != null) {
            FacturaResponseDTO.DescuentoResumenDTO descuento = new FacturaResponseDTO.DescuentoResumenDTO();
            descuento.setId(factura.getDescuento().getId());
            descuento.setNombre(factura.getDescuento().getNombre());
            descuento.setDescripcion(factura.getDescuento().getDescripcion());
            descuento.setPorcentaje(factura.getDescuento().getPorcentaje());
            descuento.setActivo(factura.getDescuento().getActivo());
            dto.setDescuento(descuento);
        }

        if (factura.getInscripcion() != null) {
            dto.setInscripcionId(factura.getInscripcion().getId());
        }

        return dto;
    }
}