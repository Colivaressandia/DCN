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

    // Supongo que tienes un servicio para S3 de la semana pasada, lo puedes inyectar aquí
    // @Autowired
    // private S3Service s3Service;

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

        // Aquí llamas a tu método para subir a S3
        // String urlS3Result = s3Service.uploadFile(file);
        String urlSimulada = "https://duoc-inscripciones-bucket.s3.amazonaws.com/" + file.getOriginalFilename();

        GuiaDespacho guia = optionalGuia.get();
        guia.setUrlS3(urlSimulada);
        guiaRepository.save(guia); // Actualiza la entidad en H2 con el link de S3

        return ResponseEntity.ok("Archivo subido exitosamente a S3 y registrado en la BD para la guía ID: " + id);
    }

    // 3. DESCARGAR GUÍAS CON VALIDACIÓN DE PERMISOS (Acceso a ambos roles)
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'DESCARGAR')")
    public ResponseEntity<?> descargarGuia(@PathVariable Long id) {
        Optional<GuiaDespacho> optionalGuia = guiaRepository.findById(id);
        if (optionalGuia.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Guía no encontrada.");
        }
        
        String urlS3 = optionalGuia.get().getUrlS3();
        if (urlS3 == null || urlS3.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La guía no tiene un archivo asociado en S3.");
        }

        // Retornamos la URL o puedes meter la lógica de descarga binaria de S3 aquí
        return ResponseEntity.ok("Enlace de descarga S3: " + urlS3);
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
}