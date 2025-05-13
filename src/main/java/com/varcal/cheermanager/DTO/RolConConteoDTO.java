package com.varcal.cheermanager.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RolConConteoDTO {
    private Integer id;
    private String nombre;
    private Integer usuariosCount;
}
