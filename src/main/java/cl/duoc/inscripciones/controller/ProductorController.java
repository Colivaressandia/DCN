package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.Inscripcion;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inscripciones")
public class ProductorController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Este endpoint reemplaza la lógica de guardado directo.
     * Ahora solo envía el objeto a la cola.
     */
    @PostMapping("/send")
    public String enviarInscripcion(@RequestBody Inscripcion inscripcion) {
        try {
            // Enviamos el objeto inscripcion a la cola llamada "cola_inscripciones"
            rabbitTemplate.convertAndSend("cola_inscripciones", inscripcion);
            
            return "Inscripción recibida y enviada a la cola de procesamiento correctamente.";
        } catch (Exception e) {
            return "Error al enviar la inscripción a la cola: " + e.getMessage();
        }
    }
}