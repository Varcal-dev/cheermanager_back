package com.varcal.cheermanager.MongoDB.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "documentos")
public class Documento {
    
    @Id
    private String id;
    
    @TextIndexed
    private String nombre;
    
    @TextIndexed
    private String descripcion;
    
    @TextIndexed
    private String tipoDocumento;
    
    // Referencia cruzada a la base de datos SQL
    private String entidadOrigen; // 'deportista', 'evento', etc.
    private Integer entidadId;       // ID en la base de datos MySQL
    private Integer mysqlDocumentoId; // Referencia opcional a la tabla documentos en MySQL
    
    // Seguimiento de usuario (también desde MySQL)
    private Integer cargadoPor;      // ID del usuario en MySQL
    
    // Propiedades del archivo
    private String formato;       // 'pdf', 'jpg', etc.
    private Integer tamaño;          // Tamaño en bytes
    
    private Almacenamiento almacenamiento;
    
    // Para archivos pequeños (menores a 16MB), almacenar directamente
    private byte[] contenido;
    
    // Metadatos
    private String hash;          // Para verificación de integridad
    private Integer version = 1;
    
    @TextIndexed
    private List<String> etiquetas = new ArrayList<>(); // Etiquetas para búsqueda
    
    // Control de acceso
    private Permisos permisos = new Permisos();
    
    // Estado y seguimiento
    private String estado = "activo"; // 'activo', 'archivado', 'eliminado'
    
    @CreatedDate
    private LocalDateTime fechaCarga;
    
    @LastModifiedDate
    private LocalDateTime fechaModificacion;
    
    // Control de versiones e historial
    private List<Historia> historia = new ArrayList<>();
    
    @Data
    public static class Almacenamiento {
        private String tipo = "gridfs"; // 'embedded', 'gridfs', 's3', 'local'
        private String ruta;           // Ruta o ID de referencia
        private String url;            // URL para acceso (si aplica)
    }
    
    @Data
    public static class Permisos {
        private String nivelAcceso = "privado"; // 'público', 'privado', 'restringido'
        private List<String> rolesPermitidos = new ArrayList<>();  // Roles que pueden acceder
        private List<Integer> usuariosPermitidos = new ArrayList<>(); // IDs de usuarios que pueden acceder
    }
    
    @Data
    public static class Historia {
        private String accion;       // 'creación', 'modificación', 'eliminación', etc.
        private Integer usuarioId;      // ID de Usuario desde MySQL
        private LocalDateTime fecha = LocalDateTime.now();
        private Object detalles;     // Detalles adicionales
    }
}