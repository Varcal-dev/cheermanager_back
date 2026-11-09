-- Pre-registros que llegan desde la landing (publico, sin sesion). Pertenecen al club Plataforma (id 1).
CREATE TABLE preregistros (
    id INT NOT NULL AUTO_INCREMENT,
    club_id INT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) NULL,
    club VARCHAR(150) NULL,
    ciudad VARCHAR(100) NULL,
    deportistas INT NULL,
    rol VARCHAR(80) NULL,
    mensaje VARCHAR(1000) NULL,
    datos_json VARCHAR(4000) NULL,
    version_consentimiento VARCHAR(20) NOT NULL,
    fecha_consentimiento DATETIME NOT NULL,
    ip VARCHAR(64) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'NUEVO',
    notas VARCHAR(500) NULL,
    PRIMARY KEY (id),
    INDEX idx_preregistros_club_correo (club_id, correo),
    INDEX idx_preregistros_club_fecha (club_id, fecha_consentimiento)
);
