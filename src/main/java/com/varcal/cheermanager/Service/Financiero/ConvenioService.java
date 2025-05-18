package com.varcal.cheermanager.Service.Financiero;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Financiero.Convenio;
import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;

@Service
public class ConvenioService {

    @Autowired
    private ConvenioRepository convenioRepository;

    public List<Convenio> listarTodos() {
        return convenioRepository.findAll();
    }

    public Convenio obtenerPorId(Integer id) {
        return convenioRepository.findById(id).orElse(null);
    }

    public Convenio crearConvenio(Convenio convenio) {
        return convenioRepository.save(convenio);
    }

    public Convenio actualizarConvenio(Integer id, Convenio convenioActualizado) {
        return convenioRepository.findById(id).map(c -> {
            c.setNombreEmpresa(convenioActualizado.getNombreEmpresa());

            // Asignar por ID sin traer el descuento completo
            Descuento descuento = new Descuento();
            descuento.setId(convenioActualizado.getDescuento().getId());
            c.setDescuento(descuento);

            c.setFechaInicio(convenioActualizado.getFechaInicio());
            c.setFechaFin(convenioActualizado.getFechaFin());

            return convenioRepository.save(c);
        }).orElse(null);
    }

    public void eliminarConvenio(Integer id) {
        convenioRepository.deleteById(id);
    }
}
