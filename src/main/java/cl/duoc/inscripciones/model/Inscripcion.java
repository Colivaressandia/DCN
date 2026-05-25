package cl.duoc.inscripciones.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "inscripciones")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String estudiante;
    
    @ElementCollection
    @CollectionTable(name = "inscripcion_cursos", joinColumns = @JoinColumn(name = "inscripcion_id"))
    @Column(name = "curso_nombre")
    private List<String> cursosSeleccionados;
    
    private Double totalPagar;

    public Inscripcion() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEstudiante() { return estudiante; }
    public void setEstudiante(String estudiante) { this.estudiante = estudiante; }
    public List<String> getCursosSeleccionados() { return cursosSeleccionados; }
    public void setCursosSeleccionados(List<String> cursosSeleccionados) { this.cursosSeleccionados = cursosSeleccionados; }
    public Double getTotalPagar() { return totalPagar; }
    public void setTotalPagar(Double totalPagar) { this.totalPagar = totalPagar; }
}