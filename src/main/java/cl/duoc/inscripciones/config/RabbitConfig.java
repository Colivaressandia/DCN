package cl.duoc.inscripciones.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Esto le dice a Spring: "Si no existe la cola, créala al iniciar"
    @Bean
    public Queue colaInscripciones() {
        return new Queue("cola_inscripciones", true); // true = duradera (durable)
    }

    // Esto convierte tus objetos Java a JSON automáticamente
    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}