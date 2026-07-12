package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import cl.duoc.inscripciones.service.AwsS3Service;
import cl.duoc.inscripciones.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
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

    @PostMapping
    public ResponseEntity<GuiaDespacho> crearGuia(@RequestBody GuiaDespacho guia) {
        GuiaDespacho guardada = repository.save(guia);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_NORMAL, guardada);
        return ResponseEntity.ok(guardada);
    }

    @GetMapping
    public List<GuiaDespacho> listar() {
        return repository.findAll();
    }

    @PostMapping("/{id}/subir")
    public ResponseEntity<String> subir(@PathVariable Long id) {
        GuiaDespacho guia = repository.findById(id).orElseThrow();
        String key = s3Service.subirGuiaAS3(guia.getRutaEfs(), guia.getTransportista(), guia.getNumeroGuia());
        guia.setUrlS3(key);
        repository.save(guia);
        return ResponseEntity.ok("Subido con éxito: " + key);
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        GuiaDespacho guia = repository.findById(id).orElseThrow();
        
        var responseBytes = s3Service.descargarGuiaDeS3(guia.getUrlS3());
        byte[] content = responseBytes.asByteArray();
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"guia_" + guia.getNumeroGuia() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespacho> actualizarGuia(@PathVariable Long id, @RequestBody GuiaDespacho guiaDetalles) {
        GuiaDespacho guia = repository.findById(id).orElseThrow();
        guia.setNumeroGuia(guiaDetalles.getNumeroGuia());
        guia.setTransportista(guiaDetalles.getTransportista());
        guia.setFecha(guiaDetalles.getFecha());
        guia.setRutaEfs(guiaDetalles.getRutaEfs());
        return ResponseEntity.ok(repository.save(guia));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGuia(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}