package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.Inscripcion;
import cl.duoc.inscripciones.repository.InscripcionRepository;
import cl.duoc.inscripciones.service.AwsS3Service;
import cl.duoc.inscripciones.config.RabbitConfig; // Importamos tus constantes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private static final Logger logger = LoggerFactory.getLogger(InscripcionController.class);

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private AwsS3Service awsS3Service;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        logger.info("DEBUG: El nombre del bucket cargado es: {}", bucketName);
    }

    // 1. Endpoint para crear y enviar a RabbitMQ
    @PostMapping("/create")
    public ResponseEntity<Inscripcion> crearInscripcion(@RequestBody Inscripcion inscripcion) {
        try {
            Inscripcion guardada = inscripcionRepository.save(inscripcion);
            logger.info("Inscripción guardada en BD con ID: {}", guardada.getId());

            // Enviamos al Exchange definido en RabbitConfig con su Routing Key
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_NORMAL, guardada);
            logger.info("Inscripción enviada a la cola correctamente.");

            return ResponseEntity.ok(guardada);
        } catch (Exception e) {
            logger.error("Error crítico al crear inscripción: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 2. Endpoint para subir a S3
    @PostMapping("/{id}/subir")
    public ResponseEntity<String> subirResumenS3(@PathVariable Long id) {
        logger.info("Iniciando proceso de subida a S3 para la inscripción ID: {}", id);
        
        try {
            Inscripcion inscripcion = inscripcionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Inscripción no encontrada con ID: " + id));

            String contenido = awsS3Service.generarContenidoTexto(
                    inscripcion.getId(), 
                    inscripcion.getEstudiante(), 
                    "Resumen de Inscripción", 
                    inscripcion.getTotalPagar()
            );

            awsS3Service.subirArchivo(id, contenido);
            
            logger.info("Archivo subido exitosamente para el ID: {}", id);
            return ResponseEntity.ok("Resumen subido exitosamente al bucket: " + bucketName);

        } catch (Exception e) {
            logger.error("ERROR CRÍTICO durante la subida a S3 para ID {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir archivo a S3: " + e.getMessage());
        }
    }
}