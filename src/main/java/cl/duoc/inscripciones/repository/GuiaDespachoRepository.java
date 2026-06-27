package cl.duoc.inscripciones.repository;

import cl.duoc.inscripciones.model.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {
    
    // Spring Data genera la consulta automática en base al nombre del método
    List<GuiaDespacho> findByTransportistaIgnoreCaseAndFecha(String transportista, LocalDate fecha);
}