package com.varcal.cheermanager.Service.Financiero;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Financiero.FacturaDTO;
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

    public List<Factura> listar() {
        return facturaRepository.findAll();
    }

    public Optional<Factura> buscarPorId(Integer id) {
        return facturaRepository.findById(id);
    }

    public void eliminar(Integer id) {
        facturaRepository.deleteById(id);
    }
}
