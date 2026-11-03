-- =============================================================================
-- V16: rubrica 2026 completa — agrega Prep y Novice y corrige la siembra de Elite (V2).
-- Fuentes: rubricas FEDECOLCHEER/IASF 2026 (Elite N1-N4, Prep, Novice Tiny y Mini/Youth)
-- y la planilla real de juzgamiento Youth Prep (PerformHive).
--
-- La rubrica es GLOBAL (sin club_id): esta migracion cambia el calculo de evaluaciones NUEVAS
-- en todos los clubes. Las evaluaciones ya guardadas no se tocan (guardan sus puntajes finales).
--
-- Que corrige de la siembra de V2 (Elite):
--   1. Ejecucion: V2 tenia UN solo valor inicial (4.0) para elevaciones + piramides + gimnasia
--      y otro (2.0) para lanzamientos + saltos. La rubrica y la planilla real puntuan cada
--      elemento por separado (elevaciones 4.0, piramides 4.0, g. estatica 4.0, g. con carrera
--      4.0, saltos 2.0, lanzamientos 2.0).
--   2. Pisos: el escalon mas bajo ("no cumple los requerimientos de ...") exigia MAYORIA y por
--      eso un equipo que no llegaba daba 0 en vez del piso (2.5 / 1.0 / 0.5 / 1.5).
--   3. Elevaciones: el escalon 3.0 (4 habilidades NO sincronizadas) nunca se alcanzaba y 4
--      habilidades sin sincronia daban 4.5. Ahora se distingue "habilidades diferentes" de
--      "de ellas, sincronizadas/canon".
--   4. Lanzamientos: 1.5 y 2.0 tenian la misma condicion; ahora 2.0 exige sincronia.
--   5. Gimnasia: faltaba el escalon 1.5 y el 2.0 ("menos de la mayoria") era inalcanzable.
--   6. Piramides: el puntaje es un RANGO que decide el juez (2.0-4.0 Elite, 2.0-3.5 Prep), y
--      los drivers de piramide no tienen puntos numericos en la rubrica (solo guian al juez).
-- =============================================================================

-- ── 0. Columnas nuevas ───────────────────────────────────────────────────────
ALTER TABLE tope_seccion_nivel ADD COLUMN condicion_sincronizadas INT NULL;
ALTER TABLE registro_sub_criterio ADD COLUMN habilidades_sincronizadas INT NULL;

-- ── 1. Niveles nuevos ────────────────────────────────────────────────────────
INSERT INTO niveles_competencia (nombre, modalidad, activo) VALUES
('Prep 1.1',          'Prep',   TRUE),
('Prep 2.1',          'Prep',   TRUE),
('Novice Tiny',       'Novice', TRUE),
('Novice Mini/Youth', 'Novice', TRUE);

-- ── 2. Ejecucion: un sub-criterio por elemento ───────────────────────────────
-- Se conservan los ids de V2 (las evaluaciones viejas siguen apuntando a ellos):
--   "Ejecucion Elevaciones Piramides Gimnasia" -> "Ejecucion Elevaciones"
--   "Ejecucion Lanzamientos Saltos"            -> "Ejecucion Saltos"
UPDATE sub_criterios_rubrica SET nombre = 'Ejecucion Elevaciones'
 WHERE nombre = 'Ejecucion Elevaciones Piramides Gimnasia';
UPDATE sub_criterios_rubrica SET nombre = 'Ejecucion Saltos'
 WHERE nombre = 'Ejecucion Lanzamientos Saltos';

INSERT INTO sub_criterios_rubrica (nombre, seccion, tipo_calculo) VALUES
('Ejecucion Piramides',          'Ejecucion', 'VALOR_INICIAL_MENOS_DRIVERS'),
('Ejecucion Gimnasia Estatica',  'Ejecucion', 'VALOR_INICIAL_MENOS_DRIVERS'),
('Ejecucion Gimnasia con Carrera', 'Ejecucion', 'VALOR_INICIAL_MENOS_DRIVERS'),
('Ejecucion Lanzamientos',       'Ejecucion', 'VALOR_INICIAL_MENOS_DRIVERS');

-- Piramides: el puntaje lo decide el juez dentro de un rango (ya no escalon + drivers numericos).
UPDATE sub_criterios_rubrica SET tipo_calculo = 'RANGO_DIRECTO_JUEZ'
 WHERE nombre = 'Dificultad de Piramides';

-- ── 3. Limpiar topes de V2 que se vuelven a sembrar con la forma correcta ─────
DELETE FROM tope_seccion_nivel
 WHERE sub_criterio_id IN (
        SELECT id FROM sub_criterios_rubrica
         WHERE nombre IN ('Dificultad de Elevaciones', 'Dificultad de Piramides',
                          'Dificultad de Lanzamientos', 'Dificultad de Saltos',
                          'Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera'))
   AND nivel_id IN (SELECT id FROM niveles_competencia WHERE modalidad = 'Elite');

-- ── 4. ELITE: topes corregidos (N1, N2, N3, N4, N4.2) ────────────────────────
-- Elevaciones (orden mayor = escalon mas alto; la calculadora toma el primero que se cumple).
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, condicion_sincronizadas, descripcion_rubrica)
SELECT n.id, sc.id, 1, 2.50, NULL, NULL, NULL,
    'Las habilidades realizadas no cumplen con los requerimientos de 3.0'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 2, 3.00, 4, 'GRAN_PARTE', NULL,
    '4 habilidades diferentes Apropiadas del nivel realizadas por GRAN PARTE del equipo'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 3, 3.50, 2, 'GRAN_PARTE', 2,
    '2 habilidades diferentes Apropiadas, cada una en un mismo momento por GRAN PARTE, sincronizadas o en canon, sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 4, 4.00, 3, 'GRAN_PARTE', 3,
    '3 habilidades diferentes Apropiadas, cada una en un mismo momento por GRAN PARTE, sincronizadas o en canon, sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 5, 4.50, 4, 'GRAN_PARTE', 4,
    '4 habilidades diferentes Apropiadas, cada una en un mismo momento por GRAN PARTE, sincronizadas o en canon, sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Elevaciones';

-- Piramides (rango que decide el juez): minimo y maximo.
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, condicion_sincronizadas, descripcion_rubrica)
SELECT n.id, sc.id, 1, 2.00, NULL, NULL, NULL, 'Minimo (DEBAJO del rango bajo)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Piramides'
UNION ALL
SELECT n.id, sc.id, 2, 4.00, NULL, NULL, NULL, 'Maximo (ALTO: 3.5-4.0)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Piramides';

-- Lanzamientos
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, condicion_sincronizadas, descripcion_rubrica)
SELECT n.id, sc.id, 1, 1.00, NULL, NULL, NULL,
    'Menos de la MAYORIA del equipo realiza un lanzamiento'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Lanzamientos'
UNION ALL
SELECT n.id, sc.id, 2, 1.50, 1, 'MAYORIA', NULL,
    'MAYORIA del equipo realiza un lanzamiento Apropiado del nivel (no requiere que sean iguales)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Lanzamientos'
UNION ALL
SELECT n.id, sc.id, 3, 2.00, 1, 'MAYORIA', 1,
    'MAYORIA realiza en la MISMA SECCION un lanzamiento Apropiado sincronizado o en canon, sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Lanzamientos';

-- Saltos (Elite: los saltos conectados requieren whip)
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, condicion_sincronizadas, descripcion_rubrica)
SELECT n.id, sc.id, 1, 0.50, NULL, NULL, NULL,
    'Las habilidades realizadas no cumplen con el requerimiento de 1.0'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 2, 1.00, 1, 'GRAN_PARTE', NULL,
    'GRAN PARTE realiza 1 salto avanzado'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 3, 1.50, 2, 'GRAN_PARTE', 2,
    'GRAN PARTE realiza 2 saltos avanzados conectados (whip), sincronizados y con variedad'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 4, 2.00, 3, 'MAXIMO', 3,
    'MAXIMO realiza 3 saltos avanzados conectados, o 2 conectados + 1 adicional (sincronizados, con variedad)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Dificultad de Saltos';

-- Gimnasia estatica y con carrera (escalones identicos)
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, condicion_sincronizadas, descripcion_rubrica)
SELECT n.id, sc.id, 1, 1.50, NULL, NULL, NULL,
    'Las habilidades realizadas no cumplen con el requerimiento de 2.0'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre IN ('Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera')
UNION ALL
SELECT n.id, sc.id, 2, 2.00, NULL, 'MENOS_MAYORIA', NULL,
    'Menos de la MAYORIA del equipo realiza una habilidad/pase apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre IN ('Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera')
UNION ALL
SELECT n.id, sc.id, 3, 2.50, NULL, 'MAYORIA', NULL,
    'MAYORIA del equipo realiza una habilidad/pase apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre IN ('Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera')
UNION ALL
SELECT n.id, sc.id, 4, 3.00, NULL, 'GRAN_PARTE', NULL,
    'GRAN PARTE del equipo realiza una habilidad/pase apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre IN ('Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera');

-- ── 5. PREP (Prep 1.1 y Prep 2.1): tablas de cantidad ────────────────────────
-- Construcciones (unidad = grupos): atletas | mayoria | gran parte | maximo
INSERT INTO tabla_cantidad_nivel (nivel_id, tabla, unidad, rango_min_atletas, rango_max_atletas, mayoria, gran_parte, maximo)
SELECT n.id, 'Construcciones', 'grupos',  5, 11, 1, 1, 2 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 12, 15, 1, 2, 3 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 16, 19, 2, 3, 4 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 20, 23, 3, 4, 5 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 24, 30, 4, 5, 6 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
-- Saltos/Gimnasia (unidad = atletas)
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas',  5, 11,  5,  6,  7 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 12, 15,  6,  7,  9 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 16, 19,  8, 10, 12 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 20, 23, 10, 13, 16 FROM niveles_competencia n WHERE n.modalidad = 'Prep'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 24, 30, 12, 15, 19 FROM niveles_competencia n WHERE n.modalidad = 'Prep';

-- ── 6. PREP: topes de dificultad (sin lanzamientos; piramides sin nivel ALTO) ─
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, condicion_sincronizadas, descripcion_rubrica)
-- Elevaciones
SELECT n.id, sc.id, 1, 2.50, NULL, NULL, NULL,
    'Las habilidades realizadas no cumplen con los requerimientos de 3.0'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 2, 3.00, 4, 'GRAN_PARTE', NULL,
    '4 habilidades diferentes Apropiadas del nivel realizadas por GRAN PARTE del equipo'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 3, 3.50, 2, 'GRAN_PARTE', 2,
    '2 habilidades diferentes Apropiadas, cada una en un mismo momento por GRAN PARTE, sincronizadas o en canon, sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 4, 4.00, 3, 'GRAN_PARTE', 3,
    '3 habilidades diferentes Apropiadas, cada una en un mismo momento por GRAN PARTE, sincronizadas o en canon, sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Elevaciones'
-- Piramides: rango 2.0 - 3.5 (MEDIO es el tope, no hay nivel ALTO en Prep)
UNION ALL
SELECT n.id, sc.id, 1, 2.00, NULL, NULL, NULL, 'Minimo (DEBAJO del rango bajo)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Piramides'
UNION ALL
SELECT n.id, sc.id, 2, 3.50, NULL, NULL, NULL, 'Maximo (MEDIO: 3.0-3.5)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Piramides'
-- Saltos (Prep: conectados y variedad NO se exigen)
UNION ALL
SELECT n.id, sc.id, 1, 0.50, NULL, NULL, NULL,
    'Las habilidades realizadas no cumplen con el requerimiento de 1.0'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 2, 1.00, 1, 'MAYORIA', NULL,
    'MAYORIA del equipo realiza 1 salto avanzado'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 3, 1.50, 2, 'GRAN_PARTE', 2,
    'GRAN PARTE realiza 2 saltos avanzados sincronizados (no requieren estar conectados ni tener variedad)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 4, 2.00, 2, 'MAXIMO', 2,
    'MAXIMO realiza 2 saltos avanzados sincronizados (no requieren estar conectados ni tener variedad)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Dificultad de Saltos'
-- Gimnasia estatica y con carrera: 1.5 / 2.0 / 2.5
UNION ALL
SELECT n.id, sc.id, 1, 1.50, NULL, NULL, NULL,
    'Las habilidades realizadas no cumplen con el requerimiento de 2.0'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre IN ('Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera')
UNION ALL
SELECT n.id, sc.id, 2, 2.00, NULL, 'MAYORIA', NULL,
    'MAYORIA del equipo realiza una habilidad/pase apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre IN ('Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera')
UNION ALL
SELECT n.id, sc.id, 3, 2.50, NULL, 'GRAN_PARTE', NULL,
    'GRAN PARTE del equipo realiza una habilidad/pase apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre IN ('Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera');

-- ── 7. Drivers de dificultad (topes por nivel) ───────────────────────────────
-- Prep elevaciones: Grado de Dificultad 0-0.6 (3 habilidades x 0.1 / 0.2) + Participacion Maxima 0.4.
-- Prep gimnasia estatica y con carrera: Grado de Dificultad 0-0.5 (0.3 Apropiada por MAXIMO / 0.5 Avanzada-Elite por GRAN PARTE).
-- Prep NO tiene Participacion Maxima en gimnasia ni drivers numericos en piramides.
INSERT INTO tope_driver_nivel (driver_id, nivel_id, tope_maximo, valor_nivel_basico, valor_nivel_alto)
SELECT d.id, n.id, 0.60, 0.10, 0.20
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.modalidad = 'Prep'
WHERE d.nombre = 'Grado de Dificultad' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT d.id, n.id, 0.40, NULL, NULL
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.modalidad = 'Prep'
WHERE d.nombre = 'Participacion Maxima' AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT d.id, n.id, 0.50, 0.30, 0.50
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.modalidad = 'Prep'
WHERE d.nombre = 'Grado de Dificultad'
  AND sc.nombre IN ('Dificultad de Gimnasia Estatica', 'Dificultad de Gimnasia con Carrera');

INSERT INTO escalon_driver_nivel (driver_id, nivel_id, orden, descripcion, valor)
SELECT d.id, n.id, 1,
    'Habilidad Apropiada del nivel por MAXIMO o Avanzada por GRAN PARTE', 0.40
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.modalidad = 'Prep'
WHERE d.nombre = 'Participacion Maxima' AND sc.nombre = 'Dificultad de Elevaciones';

-- ── 8. EJECUCION: valor inicial por elemento (todos los niveles que lo puntuan) ─
-- Elite y Prep: elevaciones 4.0, piramides 4.0, g. estatica 4.0, g. con carrera 4.0, saltos 2.0.
-- Elite ademas: lanzamientos 2.0. Los de Elite elevaciones/saltos ya existen desde V2.
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, condicion_sincronizadas, descripcion_rubrica)
SELECT n.id, sc.id, 1, 4.00, NULL, NULL, NULL, 'Valor inicial de ejecucion'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Elite', 'Prep')
  AND sc.nombre IN ('Ejecucion Piramides', 'Ejecucion Gimnasia Estatica', 'Ejecucion Gimnasia con Carrera')
UNION ALL
SELECT n.id, sc.id, 1, 2.00, NULL, NULL, NULL, 'Valor inicial de ejecucion'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Elite' AND sc.nombre = 'Ejecucion Lanzamientos'
UNION ALL
SELECT n.id, sc.id, 1, 4.00, NULL, NULL, NULL, 'Valor inicial de ejecucion'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Ejecucion Elevaciones'
UNION ALL
SELECT n.id, sc.id, 1, 2.00, NULL, NULL, NULL, 'Valor inicial de ejecucion'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad = 'Prep' AND sc.nombre = 'Ejecucion Saltos'
-- Novice Tiny: solo saltos (2.0)
UNION ALL
SELECT n.id, sc.id, 1, 2.00, NULL, NULL, NULL, 'Valor inicial de ejecucion'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre = 'Novice Tiny' AND sc.nombre = 'Ejecucion Saltos'
-- Novice Mini/Youth: elevaciones 4.0, piramides 4.0, saltos 2.0 (cuadra con el total /18)
UNION ALL
SELECT n.id, sc.id, 1, 4.00, NULL, NULL, NULL, 'Valor inicial de ejecucion'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre = 'Novice Mini/Youth' AND sc.nombre IN ('Ejecucion Elevaciones', 'Ejecucion Piramides')
UNION ALL
SELECT n.id, sc.id, 1, 2.00, NULL, NULL, NULL, 'Valor inicial de ejecucion'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre = 'Novice Mini/Youth' AND sc.nombre = 'Ejecucion Saltos';

-- ── 9. GENERAL: rangos (Prep y Novice; Elite ya los trae V2) ─────────────────
-- Formaciones 1.0-2.0 | Creatividad 1.5-2.0 | Dance dificultad 0.5-1.0 |
-- Dance ejecucion 0.5-1.0 | Showmanship 1.0-2.0
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, condicion_sincronizadas, descripcion_rubrica)
SELECT n.id, sc.id, 1, 1.00, NULL, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Prep', 'Novice') AND sc.nombre = 'Formaciones y Transiciones'
UNION ALL
SELECT n.id, sc.id, 2, 2.00, NULL, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Prep', 'Novice') AND sc.nombre = 'Formaciones y Transiciones'
UNION ALL
SELECT n.id, sc.id, 1, 1.50, NULL, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Prep', 'Novice') AND sc.nombre = 'Creatividad de Rutina'
UNION ALL
SELECT n.id, sc.id, 2, 2.00, NULL, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Prep', 'Novice') AND sc.nombre = 'Creatividad de Rutina'
UNION ALL
SELECT n.id, sc.id, 1, 0.50, NULL, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Prep', 'Novice') AND sc.nombre IN ('Dance Dificultad Elementos', 'Dance Ejecucion')
UNION ALL
SELECT n.id, sc.id, 2, 1.00, NULL, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Prep', 'Novice') AND sc.nombre IN ('Dance Dificultad Elementos', 'Dance Ejecucion')
UNION ALL
SELECT n.id, sc.id, 1, 1.00, NULL, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Prep', 'Novice') AND sc.nombre = 'Showmanship'
UNION ALL
SELECT n.id, sc.id, 2, 2.00, NULL, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.modalidad IN ('Prep', 'Novice') AND sc.nombre = 'Showmanship';
