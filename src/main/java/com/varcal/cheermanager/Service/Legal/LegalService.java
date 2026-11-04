package com.varcal.cheermanager.Service.Legal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Legal.AceptacionLegal;
import com.varcal.cheermanager.repository.Legal.AceptacionLegalRepository;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * Aceptacion de los documentos legales por parte del club (bloque legal, L1).
 * Documentos y version vigente: para publicar una version nueva se cambia el valor aqui; todos los clubes
 * quedan pendientes hasta que su administrador acepte de nuevo (se conserva el historial de aceptaciones).
 */
@Service
public class LegalService {

    public static final String TERMINOS = "TERMINOS";
    public static final String POLITICA = "POLITICA";

    /** Version vigente de cada documento (formato AAAA-MM de su publicacion). */
    public static final Map<String, String> VERSIONES_VIGENTES = Map.of(TERMINOS, "2026-11", POLITICA, "2026-11");

    /** Orden estable para mostrar. */
    private static final List<String> ORDEN = List.of(TERMINOS, POLITICA);

    private final AceptacionLegalRepository repository;

    public LegalService(AceptacionLegalRepository repository) {
        this.repository = repository;
    }

    public record DocumentoEstado(String tipo, String version, boolean aceptado) {
        public String getTipo() { return tipo; }
        public String getVersion() { return version; }
        public boolean isAceptado() { return aceptado; }
    }

    public record Estado(boolean alDia, List<DocumentoEstado> documentos) {
        public boolean isAlDia() { return alDia; }
        public List<DocumentoEstado> getDocumentos() { return documentos; }
    }

    /** El club "Plataforma" (id 1, superadmin de VarCal Tech) no acepta terminos: es el propio proveedor. */
    private static boolean esPlataforma() {
        return TenantContext.isSet() && TenantContext.get() == Club.PLATAFORMA_ID;
    }

    /** True si el club ya acepto la version vigente de TODOS los documentos. */
    public boolean clubAlDia() {
        if (esPlataforma()) {
            return true;
        }
        for (String tipo : ORDEN) {
            if (!repository.existsByTipoAndVersion(tipo, VERSIONES_VIGENTES.get(tipo))) {
                return false;
            }
        }
        return true;
    }

    public Estado estado() {
        boolean plataforma = esPlataforma();
        List<DocumentoEstado> docs = new ArrayList<>();
        boolean todos = true;
        for (String tipo : ORDEN) {
            String version = VERSIONES_VIGENTES.get(tipo);
            boolean ok = plataforma || repository.existsByTipoAndVersion(tipo, version);
            todos &= ok;
            docs.add(new DocumentoEstado(tipo, version, ok));
        }
        return new Estado(todos, docs);
    }

    /**
     * Registra la aceptacion de los documentos vigentes. Debe confirmarse CADA documento pendiente
     * (IllegalArgumentException si falta alguno). Es idempotente: lo ya aceptado no se duplica.
     */
    @Transactional
    public Estado aceptar(Integer usuarioId, String ip, Set<String> confirmados) {
        Set<String> conf = confirmados == null ? Set.of() : confirmados;
        Map<String, String> pendientes = new LinkedHashMap<>();
        for (String tipo : ORDEN) {
            String version = VERSIONES_VIGENTES.get(tipo);
            if (!repository.existsByTipoAndVersion(tipo, version)) {
                pendientes.put(tipo, version);
            }
        }
        for (String tipo : pendientes.keySet()) {
            if (!conf.contains(tipo)) {
                throw new IllegalArgumentException("Debes aceptar todos los documentos para continuar ("
                        + (TERMINOS.equals(tipo) ? "Términos y Condiciones" : "Política de Tratamiento de Datos") + ").");
            }
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (Map.Entry<String, String> e : pendientes.entrySet()) {
            AceptacionLegal a = new AceptacionLegal();
            a.setUsuarioId(usuarioId);
            a.setTipo(e.getKey());
            a.setVersion(e.getValue());
            a.setFecha(ahora);
            a.setIp(recortarIp(ip));
            repository.save(a);
        }
        return estado();
    }

    public List<AceptacionLegal> historial() {
        return repository.findAllByOrderByFechaDescIdDesc();
    }

    static String recortarIp(String ip) {
        if (ip == null) return null;
        String r = ip.trim();
        if (r.isEmpty()) return null;
        return r.length() > 64 ? r.substring(0, 64) : r;
    }
}
