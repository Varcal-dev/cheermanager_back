package com.varcal.cheermanager.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Guarda los tokens revocados en la tabla tokens_revocados (V14). Tabla global: no pasa por el filtro de club. */
@Component
public class TokenRevocadoJdbcAlmacen implements TokenBlacklistService.Almacen {

    private final JdbcTemplate jdbc;

    public TokenRevocadoJdbcAlmacen(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(String hash, long expiraMs) {
        try {
            jdbc.update("INSERT INTO tokens_revocados (token_hash, expira_ms) VALUES (?, ?)", hash, expiraMs);
        } catch (DuplicateKeyException e) {
            // ya estaba revocado (p. ej. dos cierres de sesion con el mismo token): nada que hacer
        }
    }

    @Override
    public Map<String, Long> cargarVigentes(long ahoraMs) {
        Map<String, Long> m = new HashMap<>();
        jdbc.query("SELECT token_hash, expira_ms FROM tokens_revocados WHERE expira_ms > ?",
                rs -> {
                    m.put(rs.getString(1), rs.getLong(2));
                }, ahoraMs);
        return m;
    }

    @Override
    public void purgar(long ahoraMs) {
        jdbc.update("DELETE FROM tokens_revocados WHERE expira_ms <= ?", ahoraMs);
    }
}
