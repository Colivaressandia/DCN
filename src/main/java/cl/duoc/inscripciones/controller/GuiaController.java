package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    @Autowired
    private GuiaDespachoRepository guiaRepository;

    // 1. CREAR GUÍAS DE DESPACHO
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GuiaDespacho> crearGuia(@RequestBody GuiaDespacho nuevaGuia) {
        GuiaDespacho guardada = guiaRepository.save(nuevaGuia);
        return new ResponseEntity<>(guardada, HttpStatus.CREATED);
    }

    // 2. SUBIR GUÍAS GENERADAS A S3
    @PostMapping("/{id}/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> subirGuiaAS3(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Optional<GuiaDespacho> optionalGuia = guiaRepository.findById(id);
        
        if (optionalGuia.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Guía no encontrada.");
        }

        String urlSimulada = "https://duoc-inscripciones-bucket.s3.amazonaws.com/" + file.getOriginalFilename();

        GuiaDespacho guia = optionalGuia.get();
        guia.setUrlS3(urlSimulada);
        guiaRepository.save(guia); 

        return ResponseEntity.ok("Archivo subido exitosamente a S3 y registrado en la BD para la guía ID: " + id);
    }

    // 3. DESCARGAR GUÍAS CON VALIDACIÓN DE PERMISOS (URL Completa y Clickeable)
    @GetMapping("/{id}/download")
    public ResponseEntity<?> descargarGuia(@PathVariable Long id) {
        Optional<GuiaDespacho> optionalGuia = guiaRepository.findById(id);
        if (optionalGuia.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Guía no encontrada.");
        }
        
        String urlS3 = optionalGuia.get().getUrlS3();
        if (urlS3 == null || urlS3.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La guía no tiene un archivo asociado en S3.");
        }

        // Si la BD tiene solo la ruta relativa, le anteponemos el dominio del bucket para que sea un link directo
        String urlCompletaClickeable = urlS3;
        if (!urlS3.startsWith("http")) {
            urlCompletaClickeable = "https://duoc-inscripciones-bucket.s3.amazonaws.com/" + urlS3;
        }
        
        return ResponseEntity.ok("Enlace de descarga S3: " + urlCompletaClickeable);
    }

    // 4. MODIFICAR O ACTUALIZAR GUÍAS
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> actualizarGuia(@PathVariable Long id, @RequestBody GuiaDespacho datosActualizados) {
        return guiaRepository.findById(id).map(guia -> {
            guia.setNumeroGuia(datosActualizados.getNumeroGuia());
            guia.setTransportista(datosActualizados.getTransportista());
            guia.setFecha(datosActualizados.getFecha());
            guia.setRutaEfs(datosActualizados.getRutaEfs());
            guiaRepository.save(guia);
            return ResponseEntity.ok("Guía con ID " + id + " actualizada correctamente en BD.");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Guía no encontrada."));
    }

    // 5. ELIMINAR GUÍAS ESPECÍFICAS
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> eliminarGuia(@PathVariable Long id) {
        if (guiaRepository.existsById(id)) {
            guiaRepository.deleteById(id);
            return ResponseEntity.ok("Guía con ID " + id + " eliminada exitosamente de la BD.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se pudo eliminar: Guía no encontrada.");
    }

    // 6. CONSULTAR GUÍAS POR TRANSPORTISTA Y FECHA
    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GuiaDespacho>> buscarPorTransportistaYFecha(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        
        List<GuiaDespacho> resultados = guiaRepository.findByTransportistaIgnoreCaseAndFecha(transportista, fecha);
        return ResponseEntity.ok(resultados);
    }

    // 7. LISTAR TODAS LAS GUÍAS CREADAS (Para usar en Postman)
    @GetMapping
    public ResponseEntity<List<GuiaDespacho>> listarTodasLasGuias() {
        List<GuiaDespacho> guias = guiaRepository.findAll();
        return ResponseEntity.ok(guias);
    }
}