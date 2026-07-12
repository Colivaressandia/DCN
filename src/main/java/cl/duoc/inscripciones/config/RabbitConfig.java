package cl.duoc.inscripciones.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Nombres constantes para que coincidan en todo el proyecto
    public static final String QUEUE_NORMAL = "cola_guias_normal";
    public static final String QUEUE_ERROR = "cola_guias_errores";
    public static final String EXCHANGE = "exchange_guias";

    // Routing Keys
    public static final String ROUTING_KEY_NORMAL = "key_guia_normal";
    public static final String ROUTING_KEY_ERROR = "key_guia_error";

    // 1. Declarar la Cola Normal
    @Bean
    public Queue colaNormal() {
        return new Queue(QUEUE_NORMAL, true);
    }

    // 2. Declarar la Cola de Errores
    @Bean
    public Queue colaErrores() {
        return new Queue(QUEUE_ERROR, true);
    }

    // 3. Declarar el Exchange
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    // 4. Vincular Cola Normal
    @Bean
    public Binding bindingNormal(Queue colaNormal, DirectExchange exchange) {
        return BindingBuilder.bind(colaNormal).to(exchange).with(ROUTING_KEY_NORMAL);
    }

    // 5. Vincular Cola de Errores
    @Bean
    public Binding bindingError(Queue colaErrores, DirectExchange exchange) {
        return BindingBuilder.bind(colaErrores).to(exchange).with(ROUTING_KEY_ERROR);
    }

    // 6. Conversor a JSON
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}