package cl.duoc.inscripciones.service;

import cl.duoc.inscripciones.model.Inscripcion;
import cl.duoc.inscripciones.repository.InscripcionRepository;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsumidorService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

    /**
     * Escucha la cola normal. Si ocurre un error, la excepción AmqpRejectAndDontRequeueException
     * activa el mecanismo de Dead Letter Exchange (DLX) para mover el mensaje a la cola de errores.
     */
    @RabbitListener(queues = "cola_guias_normal") // Asegúrate que el nombre coincida con tu RabbitConfig
    public void recibirInscripcion(Inscripcion inscripcion) {
        try {
            System.out.println("Procesando inscripción para: " + inscripcion.getEstudiante());
            
            // Persistimos en la base de datos Oracle
            Inscripcion guardada = inscripcionRepository.save(inscripcion);
            
            System.out.println("Inscripción guardada exitosamente con ID: " + guardada.getId());
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje, enviando a cola de errores: " + e.getMessage());
            
            // ESTA ES LA CLAVE: Al lanzar esta excepción, RabbitMQ mueve el mensaje a cola_guias_errores
            throw new AmqpRejectAndDontRequeueException("Error en persistencia", e);
        }
    }
}