package cl.duoc.inscripciones.service;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import cl.duoc.inscripciones.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GuiaConsumer {

    @Autowired
    private GuiaDespachoRepository guiaRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Escucha automáticamente la cola normal definida en RabbitConfig
    @RabbitListener(queues = RabbitConfig.QUEUE_NORMAL)
    public void recibirYGuardar(GuiaDespacho guia) {
        try {
            System.out.println("Procesando guía automáticamente: " + guia.getNumeroGuia());
            guiaRepository.save(guia);
        } catch (Exception e) {
            // Manejo de excepciones: si falla la BD, redirigimos a la cola de errores
            System.err.println("Error en persistencia, redirigiendo a Cola de Errores: " + e.getMessage());
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_ERROR, guia);
        }
    }
}