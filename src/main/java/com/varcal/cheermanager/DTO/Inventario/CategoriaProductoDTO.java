package com.varcal.cheermanager.DTO.Inventario;

import lombok.Data;

@Data
public class CategoriaProductoDTO {
    private String nombre;
    private String codigo; // 3 letras, ej. ROP, CAL, DIG
    private String descripcion;
}