package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import cl.duoc.inscripciones.service.AwsS3Service;
import cl.duoc.inscripciones.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    @Autowired
    private GuiaDespachoRepository repository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AwsS3Service s3Service;

    // 1. Crear guía (Envía a Cola 1)
    @PostMapping
    public ResponseEntity<GuiaDespacho> crearGuia(@RequestBody GuiaDespacho guia) {
        GuiaDespacho guardada = repository.save(guia);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_NORMAL, guardada);
        return ResponseEntity.ok(guardada);
    }

    // 2. Listar guías
    @GetMapping
    public List<GuiaDespacho> listar() {
        return repository.findAll();
    }

    // 3. Subir guía a S3
    @PostMapping("/{id}/subir")
    public ResponseEntity<String> subir(@PathVariable Long id) {
        GuiaDespacho guia = repository.findById(id).orElseThrow();
        String key = s3Service.subirGuiaAS3(guia.getRutaEfs(), guia.getTransportista(), guia.getNumeroGuia());
        guia.setUrlS3(key);
        repository.save(guia);
        return ResponseEntity.ok("Archivo subido con éxito a S3: " + key);
    }

    // 4. Actualizar guía
    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespacho> actualizarGuia(@PathVariable Long id, @RequestBody GuiaDespacho guiaDetalles) {
        GuiaDespacho guia = repository.findById(id).orElseThrow();
        guia.setNumeroGuia(guiaDetalles.getNumeroGuia());
        guia.setTransportista(guiaDetalles.getTransportista());
        guia.setFecha(guiaDetalles.getFecha());
        guia.setRutaEfs(guiaDetalles.getRutaEfs());
        return ResponseEntity.ok(repository.save(guia));
    }

    // 5. Eliminar guía
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGuia(@PathVariable Long id) {
        repository.deleteById(id);
        s3Service.borrarArchivo(id);
        return ResponseEntity.noContent().build();
    }
}