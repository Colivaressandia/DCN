package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
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
    
    private final GuiaDespachoRepository repository;
    private final AwsS3Service s3Service;

    public GuiaController(GuiaDespachoRepository repository, AwsS3Service s3Service) {
        this.repository = repository;
        this.s3Service = s3Service;
    }

    @PostMapping("/{id}/subir")
    public ResponseEntity<?> subir(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        logger.info("Recibiendo petición de subida para ID: {}", id);
        
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: El archivo enviado está vacío.");
            }

            GuiaDespacho guia = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guía no encontrada con ID: " + id));

            String key = s3Service.subirGuiaAS3(
                file.getInputStream(), 
                file.getSize(), 
                guia.getTransportista(), 
                guia.getNumeroGuia()
            );

            guia.setUrlS3(key);
            repository.save(guia);

            return ResponseEntity.ok("Subida completada. Referencia S3: " + key);

        } catch (Exception e) {
            logger.error("Error crítico procesando la subida: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error en el servidor: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(repository.findAll());
    }
}