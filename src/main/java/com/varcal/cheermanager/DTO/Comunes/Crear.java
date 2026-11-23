package com.varcal.cheermanager.DTO.Comunes;

import jakarta.validation.groups.Default;

/**
 * Grupo de validación "alta". Extiende Default: en un alta (@Validated(Crear.class)) se exigen las
 * reglas de siempre MÁS las marcadas con groups = Crear (campos obligatorios solo al crear).
 * En ediciones parciales (@Valid) solo rigen las reglas Default (formato y largo).
 */
public interface Crear extends Default {
}
