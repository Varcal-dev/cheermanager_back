package com.varcal.cheermanager.DTO.Comunes;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Respuesta paginada comun para los listados grandes (facturas, pagos, inscripciones). */
public record PaginaDTO<T>(
        List<T> contenido,
        int pagina,          // base 0
        int tamano,
        long totalElementos,
        int totalPaginas) {

    public static <E, T> PaginaDTO<T> de(Page<E> page, Function<List<E>, List<T>> convertir) {
        return new PaginaDTO<>(
                convertir.apply(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
