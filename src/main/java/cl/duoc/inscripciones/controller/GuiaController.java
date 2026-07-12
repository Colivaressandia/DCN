package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import cl.duoc.inscripciones.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private static final Logger logger = Logger.getLogger(GuiaController.class.getName());

    @Autowired
    private GuiaDespachoRepository guiaRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Esta variable se alimenta de application.properties o variables de entorno
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        logger.info("DEBUG: El nombre del bucket cargado es: " + bucketName);
    }

    @PostMapping
    public ResponseEntity<String> crearGuia(@RequestBody GuiaDespacho nuevaGuia) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_NORMAL, nuevaGuia);
        return new ResponseEntity<>("Guía enviada a la cola para procesamiento asíncrono.", HttpStatus.ACCEPTED);
    }

    @PostMapping("/{id}/upload")
    public ResponseEntity<String> subirGuiaAS3(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return guiaRepository.findById(id).map(guia -> {
            // Construcción dinámica: NUNCA escribir el nombre del bucket directamente aquí
            String url = "https://" + bucketName + ".s3.amazonaws.com/" + file.getOriginalFilename();
            guia.setUrlS3(url);
            guiaRepository.save(guia);
            logger.info("Archivo subido con éxito al bucket: " + bucketName + " con URL: " + url);
            return ResponseEntity.ok("Archivo subido correctamente al bucket: " + bucketName);
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Guía no encontrada."));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> descargarGuia(@PathVariable Long id) {
        return guiaRepository.findById(id)
            .map(guia -> {
                // Validación para asegurar que la URL sea correcta incluso si se recupera de BD
                String urlS3 = guia.getUrlS3();
                if (urlS3 != null && !urlS3.contains(bucketName)) {
                    logger.warning("Alerta: La URL en BD no coincide con el bucket actual configurado.");
                }
                return ResponseEntity.ok("Enlace descarga S3: " + urlS3);
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> actualizarGuia(@PathVariable Long id, @RequestBody GuiaDespacho d) {
        return guiaRepository.findById(id).map(guia -> {
            guia.setNumeroGuia(d.getNumeroGuia());
            guia.setTransportista(d.getTransportista());
            guiaRepository.save(guia);
            return ResponseEntity.ok("Guía actualizada correctamente.");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrada."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarGuia(@PathVariable Long id) {
        if (guiaRepository.existsById(id)) {
            guiaRepository.deleteById(id);
            return ResponseEntity.ok("Eliminada exitosamente.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrada.");
    }

    @GetMapping
    public ResponseEntity<List<GuiaDespacho>> listar() {
        return ResponseEntity.ok(guiaRepository.findAll());
    }
}