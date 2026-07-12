package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private static final Logger logger = Logger.getLogger(GuiaController.class.getName());

    private final GuiaDespachoRepository guiaRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public GuiaController(GuiaDespachoRepository guiaRepository) {
        this.guiaRepository = guiaRepository;
    }

    @PostConstruct
    public void init() {
        logger.info("DEBUG: El nombre del bucket cargado es: " + bucketName);
    }

    @PostMapping
    public ResponseEntity<GuiaDespacho> crearGuia(@RequestBody GuiaDespacho guia) {
        return ResponseEntity.ok(guiaRepository.save(guia));
    }

    @GetMapping
    public ResponseEntity<List<GuiaDespacho>> listar() {
        return ResponseEntity.ok(guiaRepository.findAll());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<GuiaDespacho>> buscarGuias(@RequestParam String transportista, @RequestParam String fecha) {
        // La fecha debe venir en formato YYYY-MM-DD desde Postman
        LocalDate fechaLocal = LocalDate.parse(fecha);
        return ResponseEntity.ok(guiaRepository.findByTransportistaIgnoreCaseAndFecha(transportista, fechaLocal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> obtenerGuia(@PathVariable Long id) {
        return guiaRepository.findById(id)
            .map(guia -> {
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
            guia.setFecha(d.getFecha());
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
}