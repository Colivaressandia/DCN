package cl.duoc.inscripciones;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableRabbit 
@EntityScan(basePackages = "cl.duoc.inscripciones.model")
@EnableJpaRepositories(basePackages = "cl.duoc.inscripciones.repository")
public class InscripcionesApplication {
    public static void main(String[] args) {
        SpringApplication.run(InscripcionesApplication.class, args);
    }
}