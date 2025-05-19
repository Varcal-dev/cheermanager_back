package com.varcal.cheermanager.MongoDB.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.MongoDB.model.Documento;

@Service
public class DocumentoMySqlSyncService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Crea una referencia en MySQL para un documento de MongoDB
     */
    public Integer crearReferencia(Documento documento) {
        String sql = "INSERT INTO documentos_ref (nombre, tipo_documento, entidad_origen, entidad_id, " +
                "cargado_por, fecha_carga, mongodb_id, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(
            sql,
            documento.getNombre(),
            documento.getTipoDocumento(),
            documento.getEntidadOrigen(),
            documento.getEntidadId(),
            documento.getCargadoPor(),
            java.sql.Timestamp.valueOf(documento.getFechaCarga()),
            documento.getId(),
            documento.getEstado()
        );
        
        // Obtener el ID generado
        return jdbcTemplate.queryForObject(
            "SELECT LAST_INSERT_ID()",
            Integer.class
        );
    }
    
    /**
     * Actualiza la referencia en MySQL
     */
    public void actualizarReferencia(Documento documento) {
        String sql = "UPDATE documentos_ref SET nombre = ?, tipo_documento = ?, estado = ? " +
                "WHERE mongodb_id = ?";
        
        jdbcTemplate.update(
            sql,
            documento.getNombre(),
            documento.getTipoDocumento(),
            documento.getEstado(),
            documento.getId()
        );
    }
    
    /**
     * Elimina la referencia en MySQL
     */
    public void eliminarReferencia(String mongoId) {
        String sql = "DELETE FROM documentos_ref WHERE mongodb_id = ?";
        jdbcTemplate.update(sql, mongoId);
    }
}
