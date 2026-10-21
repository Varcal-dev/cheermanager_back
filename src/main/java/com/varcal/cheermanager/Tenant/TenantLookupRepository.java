package com.varcal.cheermanager.tenant;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Consultas que, POR DISEÑO, atraviesan clubes. Son nativas porque @TenantId solo filtra HQL/JPA:
 * se usan unicamente donde todavia no se sabe en que club esta el usuario (login, recuperar
 * contrasena, arranque). Devuelven solo el id del club, nunca datos del usuario.
 * Cualquier otra consulta cross-club debe pasar por una revision explicita.
 */
@Repository
public class TenantLookupRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Integer> clubIdPorEmail(String email) {
        if (email == null) return Optional.empty();
        return primerEntero("SELECT club_id FROM usuarios WHERE email = ?1 AND activo = true", email.trim());
    }

    public Optional<Integer> clubIdPorTokenReset(String tokenHash) {
        if (tokenHash == null) return Optional.empty();
        return primerEntero("SELECT club_id FROM usuarios WHERE token_reset_password = ?1 AND activo = true", tokenHash);
    }

    /** Ids de los clubes reales activos (excluye el club "Plataforma", id 1). Para procesos programados. */
    @SuppressWarnings("unchecked")
    public java.util.List<Integer> idsClubesActivos() {
        java.util.List<Object> rows = em.createNativeQuery("SELECT id FROM clubes WHERE activo = true AND id <> 1 ORDER BY id")
                .getResultList();
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        for (Object r : rows) {
            ids.add(((Number) r).intValue());
        }
        return ids;
    }

    public boolean existeUsuarioEnAlgunClub(String username, String email) {

        Number n = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM usuarios WHERE username = ?1 OR email = ?2")
                .setParameter(1, username).setParameter(2, email).getSingleResult();
        return n.longValue() > 0;
    }

    /** Unicidad GLOBAL (username y email son unicos en toda la plataforma, no por club). */
    public boolean usernameExisteEnAlgunClub(String username) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE username = ?1", username) > 0;
    }

    public boolean emailExisteEnAlgunClub(String email) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE email = ?1", email) > 0;
    }

    public long contarUsernamesConPrefijo(String prefijo) {
        return contar("SELECT COUNT(*) FROM usuarios WHERE username LIKE ?1", prefijo + "%");
    }

    private long contar(String sql, String param) {
        return ((Number) em.createNativeQuery(sql).setParameter(1, param).getSingleResult()).longValue();
    }

    private Optional<Integer> primerEntero(String sql, String param) {
        var rows = em.createNativeQuery(sql).setParameter(1, param).setMaxResults(1).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(((Number) rows.get(0)).intValue());
    }
}