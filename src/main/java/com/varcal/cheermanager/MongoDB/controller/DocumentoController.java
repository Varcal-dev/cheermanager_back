package com.varcal.cheermanager.MongoDB.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.varcal.cheermanager.MongoDB.Service.DocumentoService;
import com.varcal.cheermanager.MongoDB.model.Documento;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    /**
     * Sube un nuevo documento
     */
    @PostMapping
    public ResponseEntity<Documento> subirDocumento(
            @RequestPart("documento") Documento documento,
            @RequestPart("archivo") MultipartFile archivo) throws IOException {

        Documento nuevoDocumento = documentoService.crearDocumento(documento, archivo);
        return ResponseEntity.ok(nuevoDocumento);
    }

    /**
     * Obtiene la metadata de un documento por su ID
     */
    @GetMapping("/{id}/metadata")
    public ResponseEntity<Documento> obtenerMetadataDocumento(
            @PathVariable String id,
            @RequestHeader("Usuario-Id") Integer usuarioId) throws IOException {

        DocumentoService.DocumentoConContenido resultado = documentoService.obtenerDocumento(id, usuarioId);
        return ResponseEntity.ok(resultado.getDocumento());
    }

    /**
     * Descarga un documento por su ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ByteArrayResource> descargarDocumento(
            @PathVariable String id,
            @RequestHeader("Usuario-Id") Integer usuarioId) throws IOException {

        DocumentoService.DocumentoConContenido resultado = documentoService.obtenerDocumento(id, usuarioId);
        if (resultado == null || resultado.getContenido() == null) {
            return ResponseEntity.notFound().build();
        }

        Documento documento = resultado.getDocumento();
        String nombreSeguro = documento.getNombre().replaceAll("[^a-zA-Z0-9\\._-]", "_");
        String extension = documento.getFormato();
        String fileName = nombreSeguro + "." + extension;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(determinarContentType(extension)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new ByteArrayResource(resultado.getContenido()));
    }

    /**
     * Busca documentos por entidad
     */
    @GetMapping("/entidad/{entidadOrigen}/{entidadId}")
    public ResponseEntity<List<Documento>> obtenerDocumentosPorEntidad(
            @PathVariable String entidadOrigen,
            @PathVariable Integer entidadId) {

        List<Documento> documentos = documentoService.obtenerDocumentosPorEntidad(entidadOrigen, entidadId);
        return ResponseEntity.ok(documentos);
    }

    /**
     * Busca documentos por texto
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<Documento>> buscarDocumentos(@RequestParam String texto) {
        List<Documento> documentos = documentoService.buscarPorTexto(texto);
        return ResponseEntity.ok(documentos);
    }

    /**
     * Actualiza un documento existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Documento> actualizarDocumento(
            @PathVariable String id,
            @RequestPart("documento") Documento documento,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @RequestHeader("Usuario-Id") Integer usuarioId) throws IOException {

        Documento documentoActualizado = documentoService.actualizarDocumento(id, documento, archivo, usuarioId);
        return ResponseEntity.ok(documentoActualizado);
    }

    /**
     * Elimina un documento
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDocumento(
            @PathVariable String id,
            @RequestHeader("Usuario-Id") Integer usuarioId) {

        documentoService.eliminarDocumento(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Determina el tipo de contenido según la extensión del archivo
     */
    private String determinarContentType(String formato) {
        switch (formato.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            /*
             * case "jpg":
             * case "jpeg": return "image/jpeg";
             * case "png": return "image/png";
             * case "doc": return "application/msword";
             * case "docx": return
             * "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
             * case "xls": return "application/vnd.ms-excel";
             * case "xlsx": return
             * "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
             */
            default:
                return "application/octet-stream";
        }
    }
}