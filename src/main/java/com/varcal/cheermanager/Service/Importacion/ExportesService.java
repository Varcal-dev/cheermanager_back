package com.varcal.cheermanager.Service.Importacion;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Exportes de listados en CSV (UTF-8 con BOM, separador ";": se abren directo en Excel).
 * Son consultas nativas filtradas a mano por club_id (las nativas no pasan por el filtro @TenantId).
 * Son de solo lectura, asi que siguen disponibles aunque la suscripcion del club este vencida.
 * No incluyen datos de salud ni de acceso (claves, usuarios).
 */
@Service
public class ExportesService {

    private static final Logger log = LoggerFactory.getLogger(ExportesService.class);

    private static final String SQL_DEPORTISTAS = "SELECT p.numero_documento, p.tipo_documento, p.nombre, p.apellidos, "
            + "p.fecha_nacimiento, g.genero, ep.estado, n.nombre, c.nombre_empresa, p.telefono, p.direccion, "
            + "d.contacto_emergencia, d.altura, d.peso, d.fecha_inscripcion, "
            + "a.nombre, a.apellidos, a.telefono, a.email, da.parentesco "
            + "FROM deportistas d "
            + "JOIN personas p ON p.id = d.persona_id "
            + "LEFT JOIN generos g ON g.id = p.genero_id "
            + "LEFT JOIN estados_persona ep ON ep.id = d.estado_id "
            + "LEFT JOIN niveles n ON n.id = d.nivel_actual_id AND n.club_id = d.club_id "
            + "LEFT JOIN convenios c ON c.id = d.convenio_id AND c.club_id = d.club_id "
            + "LEFT JOIN deportista_acudientes da ON da.deportista_id = d.id AND da.principal = 1 AND da.club_id = d.club_id "
            + "LEFT JOIN acudientes a ON a.id = da.acudiente_id AND a.club_id = d.club_id "
            + "WHERE d.club_id = :club ORDER BY p.apellidos, p.nombre";

    private static final String SQL_ACUDIENTES = "SELECT a.numero_documento, a.tipo_documento, a.nombre, a.apellidos, "
            + "a.telefono, a.email, a.direccion, CASE WHEN a.usuario_id IS NULL THEN 'No' ELSE 'Si' END, "
            + "GROUP_CONCAT(CONCAT(p.nombre, ' ', p.apellidos) ORDER BY p.apellidos SEPARATOR ' | ') "
            + "FROM acudientes a "
            + "LEFT JOIN deportista_acudientes da ON da.acudiente_id = a.id AND da.club_id = a.club_id "
            + "LEFT JOIN deportistas d ON d.id = da.deportista_id AND d.club_id = a.club_id "
            + "LEFT JOIN personas p ON p.id = d.persona_id "
            + "WHERE a.club_id = :club GROUP BY a.id ORDER BY a.apellidos, a.nombre";

    @PersistenceContext
    private EntityManager em;

    public byte[] deportistas() {
        List<List<String>> filas = new ArrayList<>();
        filas.add(List.of("numero_documento", "tipo_documento", "nombre", "apellidos", "fecha_nacimiento", "edad",
                "genero", "estado", "nivel", "convenio", "telefono", "direccion", "contacto_emergencia", "altura",
                "peso", "fecha_inscripcion", "acudiente_nombre", "acudiente_apellidos", "acudiente_telefono",
                "acudiente_email", "acudiente_parentesco"));
        LocalDate hoy = SuscripcionService.hoy();
        for (Object[] r : consulta(SQL_DEPORTISTAS)) {
            LocalDate nac = fecha(r[4]);
            List<String> f = new ArrayList<>();
            f.add(txt(r[0]));
            f.add(txt(r[1]));
            f.add(txt(r[2]));
            f.add(txt(r[3]));
            f.add(nac == null ? "" : nac.toString());
            f.add(nac == null ? "" : String.valueOf(Period.between(nac, hoy).getYears()));
            for (int i = 5; i <= 13; i++) {
                f.add(txt(r[i]));
            }
            LocalDate insc = fecha(r[14]);
            f.add(insc == null ? "" : insc.toString());
            for (int i = 15; i <= 19; i++) {
                f.add(txt(r[i]));
            }
            filas.add(f);
        }
        registrar("deportistas", filas.size() - 1);
        return CsvUtil.escribir(filas);
    }

    public byte[] acudientes() {
        List<List<String>> filas = new ArrayList<>();
        filas.add(List.of("numero_documento", "tipo_documento", "nombre", "apellidos", "telefono", "email",
                "direccion", "tiene_acceso_portal", "deportistas"));
        for (Object[] r : consulta(SQL_ACUDIENTES)) {
            List<String> f = new ArrayList<>();
            for (Object o : r) {
                f.add(txt(o));
            }
            filas.add(f);
        }
        registrar("acudientes", filas.size() - 1);
        return CsvUtil.escribir(filas);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> consulta(String sql) {
        return em.createNativeQuery(sql).setParameter("club", TenantContext.require()).getResultList();
    }

    private static String txt(Object o) {
        return o == null ? "" : o.toString();
    }

    private static LocalDate fecha(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDate d) {
            return d;
        }
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    private static void registrar(String que, int filas) {
        log.info("EXPORTE {} club={} filas={}", que, TenantContext.require(), filas);
    }
}
