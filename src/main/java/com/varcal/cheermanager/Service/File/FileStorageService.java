package com.varcal.cheermanager.Service.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.documents.upload-dir:./uploads}")
    private String uploadDir;

    private static final String[] TIPOS_PERMITIDOS = {"image/jpeg", "image/png", "image/gif", "image/webp"};
    private static final long TAMAÑO_MAXIMO = 5 * 1024 * 1024; // 5MB

    public String guardarFoto(MultipartFile file, String carpeta) throws IOException {
        validarArchivo(file);

        // Crear directorio si no existe
        Path dirPath = Paths.get(uploadDir, carpeta);
        Files.createDirectories(dirPath);

        // Generar nombre único
        String extension = obtenerExtension(file.getOriginalFilename());
        String nombreArchivo = UUID.randomUUID().toString() + "." + extension;

        // Guardar archivo
        Path filePath = dirPath.resolve(nombreArchivo);
        Files.write(filePath, file.getBytes());

        return carpeta + "/" + nombreArchivo;
    }

    public void eliminarFoto(String rutaArchivo) throws IOException {
        if (rutaArchivo == null || rutaArchivo.isEmpty()) {
            return;
        }

        Path filePath = Paths.get(uploadDir, rutaArchivo);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    public byte[] descargarFoto(String rutaArchivo) throws IOException {
        if (rutaArchivo == null || rutaArchivo.isEmpty()) {
            throw new RuntimeException("Ruta de archivo inválida");
        }

        Path filePath = Paths.get(uploadDir, rutaArchivo);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Archivo no encontrado: " + rutaArchivo);
        }

        return Files.readAllBytes(filePath);
    }

    private void validarArchivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("El archivo está vacío");
        }

        // Validar tipo MIME
        boolean tipoValido = false;
        for (String tipo : TIPOS_PERMITIDOS) {
            if (file.getContentType() != null && file.getContentType().equals(tipo)) {
                tipoValido = true;
                break;
            }
        }
        if (!tipoValido) {
            throw new RuntimeException("Tipo de archivo no permitido. Solo se aceptan: JPEG, PNG, GIF, WebP");
        }

        // Validar tamaño
        if (file.getSize() > TAMAÑO_MAXIMO) {
            throw new RuntimeException("El archivo excede el tamaño máximo de 5MB");
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "jpg";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();
    }
}
