package cl.duoc.inscripciones.service;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GuiaConsumer {

    @Autowired
    private GuiaDespachoRepository repository;

    @RabbitListener(queues = "cola_guias_normal")
    public void recibirGuia(GuiaDespacho guia) {
        try {
            repository.save(guia);
            System.out.println("Guía procesada: " + guia.getNumeroGuia());
        } catch (Exception e) {
            throw new AmqpRejectAndDontRequeueException("Error al guardar guía", e);
        }
    }
}