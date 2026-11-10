-- =============================================================================
-- V39 (S1b-1): funciones por plan.
--
--  1. caracteristicas_saas: catalogo de funciones "con puerta".
--  2. planes_caracteristicas: que funciones trae cada plan (matriz de partida, EDITABLE).
--  3. clubes_caracteristicas: excepciones por club (Enterprise a medida, cortesias). Prevalecen sobre el plan.
--
--  El nucleo va en TODOS los planes y no se controla aqui: personas, matricula, asistencia, facturacion y pagos,
--  portal, consentimientos (Ley 1581), lesiones, vencimientos, carnet, autorizacion de viaje, eventos y resultados
--  basicos, evaluaciones y habilidades, cierre de caja, comunicados, pre-inscripcion.
--
--  Matriz: Start (orden 1) solo el nucleo; Pro (2) lo de competencia avanzada y comercio; Elite (3) conciliacion y
--  multisede; Enterprise (4) API. 'fundador' y el club Plataforma lo tienen todo.
--
--  Tablas de PLATAFORMA (sin club_id con @TenantId): se leen con SQL directo, como tokens_revocados.
--  Idempotente. Para dar una excepcion a un club:
--    INSERT INTO clubes_caracteristicas (club_id, caracteristica, habilitada, motivo) VALUES (7, 'VIAJES', 1, 'cortesia');
-- =============================================================================

CREATE TABLE IF NOT EXISTS caracteristicas_saas (
  codigo       VARCHAR(40)  NOT NULL,
  nombre       VARCHAR(100) NOT NULL,
  descripcion  VARCHAR(300) NULL,
  disponible   BIT(1)       NOT NULL DEFAULT b'1',
  orden        INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO caracteristicas_saas (codigo, nombre, descripcion, disponible, orden) VALUES
  ('RUBRICA_RUTINAS',        'Rutinas con rubrica FEDECOLCHEER/IASF', 'Evaluacion de rutinas, vista previa e historial',                          1, 10),
  ('ROSTER',                 'Rosters de competencia',                'Quien compite, suplentes, posiciones y aptitud a la fecha del evento',      1, 20),
  ('VIAJES',                 'Viajes a competencia',                  'Delegacion, autorizaciones de menores, abonos y saldos',                    1, 30),
  ('RANKING',                'Ranking e historial de resultados',     'Ranking por temporada y por deportista',                                    1, 40),
  ('GESTION_ENTRENADORES',   'Certificaciones y horas de entrenadores','Certificaciones, horas dictadas y liquidacion',                            1, 50),
  ('TIENDA',                 'Inventario y ventas',                   'Productos, variantes, inventario y ventas',                                 1, 60),
  ('CONVENIOS',              'Convenios y descuentos por convenio',   'Crear y editar convenios (consultarlos siempre se puede)',                  1, 70),
  ('RECORDATORIOS_MORA',     'Recordatorios de cartera',              'Enviar recordatorios de mora (consultarlos siempre se puede)',              1, 80),
  ('AUDITORIA',              'Auditoria de operaciones',              'Pantalla de auditoria de altas y bajas',                                    1, 90),
  ('ROLES_PERSONALIZADOS',   'Roles y permisos ampliados',            'Edicion de roles del club mas alla de la plantilla (control pendiente)',    1, 100),
  ('DASHBOARDS_AVANZADOS',   'Dashboards avanzados',                  'Dashboards de Finanzas y Administracion (control pendiente)',               1, 110),
  ('CONCILIACION_BANCARIA',  'Conciliacion bancaria',                 'Conciliacion de pagos contra extracto',                                     1, 120),
  ('MULTISEDE',              'Sedes y espacios',                      'Varias sedes, espacios y cruce de horarios',                                1, 130),
  ('REPORTES_CONSOLIDADOS',  'Reportes consolidados',                 'Pendiente de construir',                                                    0, 140),
  ('ANALISIS_HISTORICO',     'Analisis historico',                    'Pendiente de construir',                                                    0, 150),
  ('EXPORTES_AVANZADOS',     'Exportaciones avanzadas',               'Pendiente de construir',                                                    0, 160),
  ('API_INTEGRACIONES',      'API e integraciones',                   'Pendiente de construir',                                                    0, 170);

CREATE TABLE IF NOT EXISTS planes_caracteristicas (
  id             INT         NOT NULL AUTO_INCREMENT,
  plan_id        INT         NOT NULL,
  caracteristica VARCHAR(40) NOT NULL,
  habilitada     BIT(1)      NOT NULL DEFAULT b'1',
  limite_valor   INT         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_caracteristica (plan_id, caracteristica),
  CONSTRAINT fk_plancar_plan  FOREIGN KEY (plan_id)        REFERENCES planes_saas (id),
  CONSTRAINT fk_plancar_carac FOREIGN KEY (caracteristica) REFERENCES caracteristicas_saas (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- desde_orden usa planes_saas.orden (start=1, pro=2, elite=3, enterprise=4). 'fundador' (orden 0) lo trae todo.
INSERT IGNORE INTO planes_caracteristicas (plan_id, caracteristica, habilitada)
SELECT p.id, s.caracteristica,
       CASE WHEN p.codigo = 'fundador'    THEN 1
            WHEN p.orden >= s.desde_orden THEN 1
            ELSE 0 END
  FROM planes_saas p
  JOIN (SELECT 'RUBRICA_RUTINAS'        AS caracteristica, 2 AS desde_orden
        UNION ALL SELECT 'ROSTER',                 2
        UNION ALL SELECT 'VIAJES',                 2
        UNION ALL SELECT 'RANKING',                2
        UNION ALL SELECT 'GESTION_ENTRENADORES',   2
        UNION ALL SELECT 'TIENDA',                 2
        UNION ALL SELECT 'CONVENIOS',              2
        UNION ALL SELECT 'RECORDATORIOS_MORA',     2
        UNION ALL SELECT 'AUDITORIA',              2
        UNION ALL SELECT 'ROLES_PERSONALIZADOS',   2
        UNION ALL SELECT 'DASHBOARDS_AVANZADOS',   2
        UNION ALL SELECT 'CONCILIACION_BANCARIA',  3
        UNION ALL SELECT 'MULTISEDE',              3
        UNION ALL SELECT 'REPORTES_CONSOLIDADOS',  3
        UNION ALL SELECT 'ANALISIS_HISTORICO',     3
        UNION ALL SELECT 'EXPORTES_AVANZADOS',     3
        UNION ALL SELECT 'API_INTEGRACIONES',      4) s;

CREATE TABLE IF NOT EXISTS clubes_caracteristicas (
  id             INT          NOT NULL AUTO_INCREMENT,
  club_id        INT          NOT NULL,
  caracteristica VARCHAR(40)  NOT NULL,
  habilitada     BIT(1)       NOT NULL,
  limite_valor   INT          NULL,
  motivo         VARCHAR(300) NULL,
  registrado_por VARCHAR(80)  NULL,
  creado_en      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_club_caracteristica (club_id, caracteristica),
  CONSTRAINT fk_clubcar_club  FOREIGN KEY (club_id)        REFERENCES clubes (id),
  CONSTRAINT fk_clubcar_carac FOREIGN KEY (caracteristica) REFERENCES caracteristicas_saas (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
