package com.varcal.cheermanager.Controller.Importacion;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.varcal.cheermanager.Service.Importacion.ExportesService;
import com.varcal.cheermanager.Service.Importacion.ImportacionDeportistasService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/**
 * Importacion masiva de deportistas (CSV) y exportes de listados (CSV).
 * Permisos del club: importar_deportistas y exportar_listados (V13). Los exportes son GET: siguen funcionando con
 * la suscripcion vencida (solo lectura).
 */
@RestController
@RequestMapping("/api")
public class ImportacionController {

    private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");

    private final ImportacionDeportistasService importacion;
    private final ExportesService exportes;

    public ImportacionController(ImportacionDeportistasService importacion, ExportesService exportes) {
        this.importacion = importacion;
        this.exportes = exportes;
    }

    @GetMapping("/importaciones/deportistas/plantilla")
    @RequiresPermission("importar_deportistas")
    public ResponseEntity<byte[]> plantilla() {
        return csv(importacion.plantilla(), "plantilla-deportistas.csv");
    }

    /** Revisa el archivo y devuelve el informe fila por fila. No guarda nada. */
    @PostMapping("/importaciones/deportistas/validar")
    @RequiresPermission("importar_deportistas")
    public ImportacionDeportistasService.Resultado validar(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return importacion.validar(archivo.getBytes());
    }

    /** Guarda las filas correctas; las incorrectas quedan en el informe para corregirlas y volver a cargarlas. */
    @PostMapping("/importaciones/deportistas")
    @RequiresPermission("importar_deportistas")
    public ImportacionDeportistasService.Resultado importar(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return importacion.importar(archivo.getBytes());
    }

    @GetMapping("/exportes/deportistas")
    @RequiresPermission("exportar_listados")
    public ResponseEntity<byte[]> deportistas() {
        return csv(exportes.deportistas(), "deportistas.csv");
    }

    @GetMapping("/exportes/acudientes")
    @RequiresPermission("exportar_listados")
    public ResponseEntity<byte[]> acudientes() {
        return csv(exportes.acudientes(), "acudientes.csv");
    }

    private static ResponseEntity<byte[]> csv(byte[] datos, String nombre) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(CSV)
                .body(datos);
    }
}
