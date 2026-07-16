package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaRepository;
import cl.duoc.inscripciones.service.AwsS3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private static final Logger logger = LoggerFactory.getLogger(GuiaController.class);
    
    private final GuiaRepository repository;
    private final AwsS3Service s3Service;

    public GuiaController(GuiaRepository repository, AwsS3Service s3Service) {
        this.repository = repository;
        this.s3Service = s3Service;
    }

    @PostMapping("/{id}/subir")
    public ResponseEntity<?> subir(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        logger.info("Recibiendo petición de subida para ID: {}", id);
        
        try {
            if (file == null || file.isEmpty()) {
                logger.error("El archivo enviado para el ID {} está vacío", id);
                return ResponseEntity.badRequest().body("Error: El archivo enviado está vacío.");
            }

            GuiaDespacho guia = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guía no encontrada con ID: " + id));

            // Usamos el InputStream directo para evitar problemas con rutas locales del sistema de archivos
            String key = s3Service.subirGuiaAS3(
                file.getInputStream(), 
                file.getSize(), 
                guia.getTransportista(), 
                guia.getNumeroGuia()
            );

            // Actualizamos la referencia en la base de datos
            guia.setUrlS3(key);
            repository.save(guia);

            logger.info("Archivo subido con éxito a S3: {} para la guía ID: {}", key, id);
            return ResponseEntity.ok("Subida completada. Referencia S3: " + key);

        } catch (Exception e) {
            logger.error("Error crítico procesando la subida para el ID {}: {}", id, e.getMessage());
            // Esto devolverá el error real (Token expirado, Acceso denegado, etc.) a Postman
            return ResponseEntity.status(500).body("Error en el servidor: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(repository.findAll());
    }
}