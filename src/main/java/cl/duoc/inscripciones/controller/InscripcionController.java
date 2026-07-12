package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.Inscripcion;
import cl.duoc.inscripciones.repository.InscripcionRepository;
import cl.duoc.inscripciones.service.AwsS3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api")
public class InscripcionController {

    private static final Logger logger = LoggerFactory.getLogger(InscripcionController.class);

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private AwsS3Service awsS3Service;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        logger.info("DEBUG: El nombre del bucket cargado es: {}", bucketName);
    }

    @PostMapping("/inscripciones/{id}/subir")
    public ResponseEntity<String> subirResumenS3(@PathVariable Long id) {
        logger.info("Iniciando proceso de subida a S3 para la inscripción ID: {}", id);
        
        try {
            // 1. Buscar la inscripción
            Inscripcion inscripcion = inscripcionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Inscripción no encontrada con ID: " + id));

            // 2. Generar el contenido (ajusta los parámetros según tu método)
            String contenido = awsS3Service.generarContenidoTexto(
                    inscripcion.getId(), 
                    inscripcion.getEstudiante(), 
                    "Resumen de Inscripción", 
                    inscripcion.getTotalPagar()
            );

            // 3. Intentar subir a S3
            awsS3Service.subirArchivo(id, contenido);
            
            logger.info("Archivo subido exitosamente para el ID: {}", id);
            return ResponseEntity.ok("Resumen subido exitosamente al bucket: " + bucketName);

        } catch (Exception e) {
            // Esto imprimirá el error completo con el "Caused by" en tu docker logs
            logger.error("ERROR CRÍTICO durante la subida a S3 para ID {}: ", id, e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir archivo a S3: " + e.getMessage());
        }
    }
}