package cl.duoc.inscripciones.service;

import cl.duoc.inscripciones.model.Inscripcion;
import cl.duoc.inscripciones.repository.InscripcionRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsumidorService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

    /**
     * @RabbitListener le indica a Spring que este método debe ejecutarse 
     * cada vez que llegue un mensaje a la cola especificada.
     */
    @RabbitListener(queues = "cola_inscripciones")
    public void recibirInscripcion(Inscripcion inscripcion) {
        try {
            System.out.println("Procesando inscripción recibida desde la cola para el estudiante: " + inscripcion.getEstudiante());
            
            // Persistimos en la base de datos Oracle
            Inscripcion guardada = inscripcionRepository.save(inscripcion);
            
            System.out.println("Inscripción guardada exitosamente en BD con ID: " + guardada.getId());
            
        } catch (Exception e) {
            System.err.println("Error crítico al procesar el mensaje desde la cola: " + e.getMessage());
            // Aquí podrías añadir lógica para reintentos si fuera necesario
        }
    }
}