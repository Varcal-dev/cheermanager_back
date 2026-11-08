-- L5: el club decide cuando exigir el consentimiento de salud (solo se puede activar sin pendientes).
ALTER TABLE clubes ADD COLUMN exigir_consentimiento_salud BOOLEAN NOT NULL DEFAULT FALSE;
