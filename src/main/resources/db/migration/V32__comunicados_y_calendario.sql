-- C7: comunicados a un grupo (o a todo el club) por portal y correo, con registro de lo enviado.
-- Permisos nuevos: ver_comunicados (historial y calendarios) y enviar_comunicados. Solo Administrador; los demas roles
-- se ajustan desde Roles y Permisos. Idempotente.
CREATE TABLE IF NOT EXISTS comunicados (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  club_id            INT NOT NULL,
  grupo_id           INT NULL,
  asunto             VARCHAR(150) NOT NULL,
  mensaje            TEXT NOT NULL,
  canal_sistema      BIT(1) NOT NULL,
  canal_correo       BIT(1) NOT NULL,
  deportistas        INT NOT NULL,
  notificados        INT NOT NULL,
  correos_enviados   INT NOT NULL,
  correos_fallidos   INT NOT NULL,
  sin_contacto       INT NOT NULL,
  creado_por         INT NOT NULL,
  fecha_creacion     DATETIME NOT NULL,
  KEY idx_comunicados_club (club_id, fecha_creacion),
  CONSTRAINT fk_comunicados_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comunicado_envios (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  club_id        INT NOT NULL,
  comunicado_id  INT NOT NULL,
  canal          VARCHAR(10) NOT NULL,
  destino        VARCHAR(160) NOT NULL,
  estado         VARCHAR(10) NOT NULL,
  detalle        VARCHAR(200) NULL,
  KEY idx_comunicado_envios (club_id, comunicado_id),
  CONSTRAINT fk_comunicado_envios_comunicado FOREIGN KEY (comunicado_id) REFERENCES comunicados (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('ver_comunicados'), ('enviar_comunicados');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_comunicados', 'enviar_comunicados')
 WHERE r.nombre = 'Administrador';
