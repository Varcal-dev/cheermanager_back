package com.varcal.cheermanager.MongoDB.Service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.varcal.cheermanager.MongoDB.Repo.DocumentoRepositoryMongo;
import com.varcal.cheermanager.MongoDB.model.Documento;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepositoryMongo documentoRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    // Añadir esta importación en DocumentoService
    @Autowired
    private DocumentoMySqlSyncService syncService;

    /**
     * Crea un nuevo documento en MongoDB
     */
    public Documento crearDocumento(Documento documento, MultipartFile archivo) throws IOException {
        try {
            byte[] bytes = archivo.getBytes();
            String hash = calcularHash(bytes);

            documento.setFormato(obtenerExtension(archivo.getOriginalFilename()));
            documento.setTamaño((int) archivo.getSize());
            documento.setHash(hash);

            Documento.Historia historia = new Documento.Historia();
            historia.setAccion("creación");
            historia.setUsuarioId(documento.getCargadoPor());
            historia.setFecha(LocalDateTime.now());
            historia.setDetalles(new Object() {
                public final int version = 1;
            });

            documento.getHistoria().add(historia);

            // Determinar tipo de almacenamiento según tamaño
            if (bytes.length < 16_000_000) { // Menos de 16MB, guardar directamente
                documento.setContenido(bytes);

                Documento.Almacenamiento almacenamiento = new Documento.Almacenamiento();
                almacenamiento.setTipo("embedded");
                documento.setAlmacenamiento(almacenamiento);
            } else { // Archivo grande, usar GridFS
                ObjectId fileId = almacenarEnGridFS(bytes, documento);

                Documento.Almacenamiento almacenamiento = new Documento.Almacenamiento();
                almacenamiento.setTipo("gridfs");
                almacenamiento.setRuta(fileId.toString());
                documento.setAlmacenamiento(almacenamiento);
            }

            Documento documentoGuardado = documentoRepository.save(documento);

            // Ahora creamos la referencia en MySQL y actualizamos el documento con el ID de
            // MySQL
            Integer mysqlId = syncService.crearReferencia(documentoGuardado);
            documentoGuardado.setMysqlDocumentoId(mysqlId);
            return documentoRepository.save(documentoGuardado);
        } catch (

        Exception e) {
            throw new RuntimeException("Error al crear documento: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene un documento por su ID
     */
    public DocumentoConContenido obtenerDocumento(String id, Integer usuarioId) throws IOException {
        Optional<Documento> documentoOpt = documentoRepository.findById(id);

        if (!documentoOpt.isPresent()) {
            throw new RuntimeException("Documento no encontrado");
        }

        Documento documento = documentoOpt.get();

        // Verificar permisos
        if (!tienePermiso(documento, usuarioId)) {
            throw new RuntimeException("No tiene permiso para acceder a este documento");
        }

        // Obtener contenido según tipo de almacenamiento
        byte[] contenido;
        if ("embedded".equals(documento.getAlmacenamiento().getTipo())) {
            contenido = documento.getContenido();
        } else if ("gridfs".equals(documento.getAlmacenamiento().getTipo())) {
            contenido = obtenerDesdeGridFS(documento.getAlmacenamiento().getRuta());
        } else {
            throw new RuntimeException(
                    "Tipo de almacenamiento no soportado: " + documento.getAlmacenamiento().getTipo());
        }

        // Registrar acceso
        Documento.Historia historia = new Documento.Historia();
        historia.setAccion("acceso");
        historia.setUsuarioId(usuarioId);
        historia.setFecha(LocalDateTime.now());
        documento.getHistoria().add(historia);
        documentoRepository.save(documento);

        return new DocumentoConContenido(documento, contenido);
    }

    /**
     * Busca documentos por entidad
     */
    public List<Documento> obtenerDocumentosPorEntidad(String entidadOrigen, Integer entidadId) {
        return documentoRepository.findByEntidadOrigenAndEntidadId(entidadOrigen, entidadId);
    }

    /**
     * Busca documentos por texto
     */
    public List<Documento> buscarPorTexto(String texto) {
        return documentoRepository.buscarPorTexto(texto);
    }

    /**
     * Actualiza un documento existente
     */
    public Documento actualizarDocumento(String id, Documento actualizacion, MultipartFile archivo, Integer usuarioId)
            throws IOException {
        Optional<Documento> documentoOpt = documentoRepository.findById(id);

        if (!documentoOpt.isPresent()) {
            throw new RuntimeException("Documento no encontrado");
        }

        Documento documento = documentoOpt.get();

        // Verificar permisos
        if (!tienePermiso(documento, usuarioId)) {
            throw new RuntimeException("No tiene permiso para modificar este documento");
        }

        // Actualizar campos básicos
        documento.setNombre(actualizacion.getNombre());
        documento.setDescripcion(actualizacion.getDescripcion());
        documento.setTipoDocumento(actualizacion.getTipoDocumento());
        documento.setEtiquetas(actualizacion.getEtiquetas());
        documento.setPermisos(actualizacion.getPermisos());
        documento.setVersion(documento.getVersion() + 1);

        // Si hay un nuevo archivo, actualizar contenido
        if (archivo != null && !archivo.isEmpty()) {
            byte[] bytes = archivo.getBytes();
            String hash = calcularHash(bytes);

            documento.setFormato(obtenerExtension(archivo.getOriginalFilename()));
            documento.setTamaño((int) archivo.getSize());
            documento.setHash(hash);

            // Si cambia el tamaño y cruza el umbral, cambiar tipo de almacenamiento
            if (bytes.length < 16_000_000) { // Menos de 16MB, guardar directamente
                // Si antes estaba en GridFS, eliminar
                if ("gridfs".equals(documento.getAlmacenamiento().getTipo())) {
                    eliminarDeGridFS(documento.getAlmacenamiento().getRuta());
                }

                documento.setContenido(bytes);

                Documento.Almacenamiento almacenamiento = new Documento.Almacenamiento();
                almacenamiento.setTipo("embedded");
                documento.setAlmacenamiento(almacenamiento);
            } else { // Archivo grande, usar GridFS
                // Si antes estaba en GridFS, eliminar el anterior
                if ("gridfs".equals(documento.getAlmacenamiento().getTipo())) {
                    eliminarDeGridFS(documento.getAlmacenamiento().getRuta());
                }

                ObjectId fileId = almacenarEnGridFS(bytes, documento);

                Documento.Almacenamiento almacenamiento = new Documento.Almacenamiento();
                almacenamiento.setTipo("gridfs");
                almacenamiento.setRuta(fileId.toString());
                documento.setAlmacenamiento(almacenamiento);
            }
        }

        // Registrar modificación
        Documento.Historia historia = new Documento.Historia();
        historia.setAccion("modificación");
        historia.setUsuarioId(usuarioId);
        historia.setFecha(LocalDateTime.now());
        historia.setDetalles(new Object() {
            public final int versionNueva = documento.getVersion();
        });
        documento.getHistoria().add(historia);

        return documentoRepository.save(documento);
    }

    /**
     * Elimina un documento
     */
    public void eliminarDocumento(String id, Integer usuarioId) {
        Optional<Documento> documentoOpt = documentoRepository.findById(id);

        if (!documentoOpt.isPresent()) {
            throw new RuntimeException("Documento no encontrado");
        }

        Documento documento = documentoOpt.get();

        // Verificar permisos
        if (!tienePermiso(documento, usuarioId)) {
            throw new RuntimeException("No tiene permiso para eliminar este documento");
        }

        // Si está en GridFS, eliminar archivo
        if ("gridfs".equals(documento.getAlmacenamiento().getTipo())) {
            eliminarDeGridFS(documento.getAlmacenamiento().getRuta());
        }

        // Eliminar el documento
        documentoRepository.delete(documento);
    }

    /**
     * Verifica si un usuario tiene permiso para acceder al documento
     */
    private boolean tienePermiso(Documento documento, Integer usuarioId) {
        // Implementar lógica de verificación de permisos
        if ("público".equals(documento.getPermisos().getNivelAcceso()))
            return true;
        if (documento.getCargadoPor().equals(usuarioId))
            return true;
        if (documento.getPermisos().getUsuariosPermitidos().contains(usuarioId))
            return true;
        // Verificar roles también si es necesario
        return false;
    }

    /**
     * Almacena un archivo en GridFS
     */
    private ObjectId almacenarEnGridFS(byte[] bytes, Documento documento) throws IOException {
        String nombreArchivo = documento.getNombre() + "." + documento.getFormato();
        return gridFsTemplate.store(
                new ByteArrayInputStream(bytes),
                nombreArchivo,
                determinarContentType(documento.getFormato()));
    }

    /**
     * Obtiene un archivo desde GridFS
     */
    private byte[] obtenerDesdeGridFS(String fileId) throws IOException {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        if (file == null) {
            throw new RuntimeException("Archivo no encontrado en GridFS");
        }

        try (GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(file.getObjectId());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = downloadStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    /**
     * Elimina un archivo de GridFS
     */
    private void eliminarDeGridFS(String fileId) {
        gridFSBucket.delete(new ObjectId(fileId));
    }

    /**
     * Calcula el hash SHA-256 de un array de bytes
     */
    private String calcularHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular hash SHA-256", e);
        }
    }

    /**
     * Determina el tipo de contenido según la extensión del archivo
     */
    private String determinarContentType(String formato) {
        switch (formato.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Obtiene la extensión de un nombre de archivo
     */
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null)
            return "";
        int ultimoPunto = nombreArchivo.lastIndexOf(".");
        if (ultimoPunto < 0)
            return "";
        return nombreArchivo.substring(ultimoPunto + 1);
    }

    /**
     * Clase para retornar documento con su contenido
     */
    public static class DocumentoConContenido {
        private final Documento documento;
        private final byte[] contenido;

        public DocumentoConContenido(Documento documento, byte[] contenido) {
            this.documento = documento;
            this.contenido = contenido;
        }

        public Documento getDocumento() {
            return documento;
        }

        public byte[] getContenido() {
            return contenido;
        }
    }
}