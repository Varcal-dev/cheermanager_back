package com.varcal.cheermanager.repository.Financiero;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Financiero.Convenio;

@Repository
public interface ConvenioRepository extends JpaRepository<Convenio, Integer> {

    @NonNull
    Optional<Convenio> findById(Integer convenioId);

    // Usado por DescuentoService para impedir borrar un descuento que
    // todavía está referenciado por algún convenio (evita el 500 genérico
    // de integridad referencial de MySQL y da un mensaje claro al usuario).
    boolean existsByDescuentoId(Integer descuentoId);
}