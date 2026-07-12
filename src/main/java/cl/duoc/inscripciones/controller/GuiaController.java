package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/guias")
@CrossOrigin(origins = "*") // Reforzado para asegurar CORS
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

    // Endpoint corregido con logs para diagnóstico
    @GetMapping("/buscar")
    public ResponseEntity<List<GuiaDespacho>> buscarGuias(
            @RequestParam String transportista, 
            @RequestParam String fecha,
            HttpServletRequest request) {

        logger.info("--- PETICION RECIBIDA EN /api/guias/buscar ---");
        logger.info("URL completa: " + request.getRequestURL());
        logger.info("Transportista: " + transportista + " | Fecha: " + fecha);

        try {
            LocalDate fechaLocal = LocalDate.parse(fecha);
            List<GuiaDespacho> resultados = guiaRepository.findByTransportistaIgnoreCaseAndFecha(transportista, fechaLocal);
            return ResponseEntity.ok(resultados);
        } catch (Exception e) {
            logger.severe("Error al buscar guías: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> obtenerGuia(@PathVariable Long id) {
        return guiaRepository.findById(id)
            .map(guia -> ResponseEntity.ok("Enlace descarga S3: " + guia.getUrlS3()))
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