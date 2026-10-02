package com.varcal.cheermanager.Service.Financiero;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Financiero.ConvenioDto;
import com.varcal.cheermanager.Models.Financiero.Convenio;
import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Financiero.DescuentoRepository;

@Service
public class ConvenioService {

    @Autowired
    private ConvenioRepository convenioRepository;

    @Autowired
    private DescuentoRepository descuentoRepository;

    public List<Convenio> listarTodos() {
        return convenioRepository.findAll();
    }

    public Convenio obtenerPorId(Integer id) {
        return convenioRepository.findById(id).orElse(null);
    }

    public Convenio crearConvenio(Convenio convenio) {
        return convenioRepository.save(convenio);
    }

    // Unificado con crearConvenio: recibe el mismo ConvenioDto (descuentoId
    // plano) en vez de la entidad Convenio completa, que obligaba al
    // frontend a mandar un shape distinto solo para editar.
    public Convenio actualizarConvenio(Integer id, ConvenioDto dto) {
        Descuento descuento = descuentoRepository.findById(dto.getDescuentoId())
                .orElseThrow(() -> new RuntimeException("Descuento no encontrado"));

        return convenioRepository.findById(id).map(c -> {
            c.setNombreEmpresa(dto.getNombreEmpresa());
            c.setDescuento(descuento);
            c.setFechaInicio(dto.getFechaInicio());
            c.setFechaFin(dto.getFechaFin());
            return convenioRepository.save(c);
        }).orElse(null);
    }

    public void eliminarConvenio(Integer id) {
        convenioRepository.deleteById(id);
    }
}