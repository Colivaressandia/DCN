package cl.duoc.inscripciones.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cursos")
public class Curso {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    private String instructor;
    private String duracion;
    private Double costo;

    public Curso() {}

    public Curso(String nombre, String instructor, String duracion, Double costo) {
        this.nombre = nombre;
        this.instructor = instructor;
        this.duracion = duracion;
        this.costo = costo;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public String getDuracion() { return duracion; }
    public void setDuracion(String duracion) { this.duracion = duracion; }
    public Double getCosto() { return costo; }
    public void setCosto(Double costo) { this.costo = costo; }
}