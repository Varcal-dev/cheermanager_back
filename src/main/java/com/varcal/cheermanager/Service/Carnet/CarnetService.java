package com.varcal.cheermanager.Service.Carnet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * C13: carnet digital. El QR lleva un codigo firmado (HMAC) que identifica club y deportista; no contiene datos
 * personales. Al escanearlo, el personal con sesion lo verifica y ve nombre, grupo y estado actuales.
 * Formato: CM1.<clubId>.<deportistaId>.<firma>
 */
@Service
public class CarnetService {

    private static final String PREFIJO = "CM1";
    private final byte[] clave;
    private final DeportistaRepository deportistas;
    private final DeportistaPerteneceGrupoRepository membresias;
    private final ClubRepository clubes;

    public CarnetService(@Value("${jwt.secret}") String secreto, DeportistaRepository deportistas,
            DeportistaPerteneceGrupoRepository membresias, ClubRepository clubes) {
        this.clave = ("carnet:" + secreto).getBytes(StandardCharsets.UTF_8);
        this.deportistas = deportistas;
        this.membresias = membresias;
        this.clubes = clubes;
    }

    public record Carnet(Integer deportistaId, String codigo, String nombre, String club, String estado,
            boolean vigente, List<String> grupos, String foto) {
    }

    String firma(int clubId, int deportistaId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            byte[] h = mac.doFinal((clubId + "." + deportistaId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el carnet.");
        }
    }

    public String codigo(int clubId, int deportistaId) {
        return PREFIJO + "." + clubId + "." + deportistaId + "." + firma(clubId, deportistaId);
    }

    /** Carnet de un deportista del club actual. */
    public Carnet carnet(Integer deportistaId) {
        Deportista d = deportistas.findById(deportistaId)
                .orElseThrow(() -> new IllegalArgumentException("Deportista no encontrado."));
        return armar(d);
    }

    /** Verifica un codigo escaneado: firma valida y del club de quien escanea. */
    public Carnet verificar(String codigo) {
        String[] p = codigo == null ? new String[0] : codigo.trim().split("\\.");
        if (p.length != 4 || !PREFIJO.equals(p[0])) {
            throw new IllegalArgumentException("Código de carnet inválido.");
        }
        int clubId;
        int depId;
        try {
            clubId = Integer.parseInt(p[1]);
            depId = Integer.parseInt(p[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Código de carnet inválido.");
        }
        boolean firmaOk = MessageDigest.isEqual(firma(clubId, depId).getBytes(StandardCharsets.UTF_8),
                p[3].getBytes(StandardCharsets.UTF_8));
        if (!firmaOk) {
            throw new IllegalArgumentException("Código de carnet inválido.");
        }
        if (clubId != TenantContext.require()) {
            throw new IllegalArgumentException("Este carnet pertenece a otro club.");
        }
        return carnet(depId);
    }

    private Carnet armar(Deportista d) {
        int clubId = TenantContext.require();
        String estado = d.getEstado() == null ? "—" : d.getEstado().getEstado();
        List<String> grupos = membresias.findByDeportistaIdAndFechaFinIsNull(d.getId()).stream()
                .map(m -> m.getGrupo().getNombre()).toList();
        String club = clubes.findById(clubId).map(c -> c.getNombre()).orElse("");
        String nombre = (d.getPersona().getNombre() + " " + d.getPersona().getApellidos()).trim();
        return new Carnet(d.getId(), codigo(clubId, d.getId()), nombre, club, estado, "Activo".equalsIgnoreCase(estado),
                grupos, d.getFotoPerfil());
    }
}
