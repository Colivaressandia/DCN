package cl.duoc.inscripciones.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GUIAS_DESPACHO")
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String numeroGuia;
    private String transportista;
    private LocalDate fecha;
    private String rutaEfs;
    private String urlS3;

    // Constructor vacío obligatorio para JPA
    public GuiaDespacho() {
    }

    // Constructor completo
    public GuiaDespacho(Long id, String numeroGuia, String transportista, LocalDate fecha, String rutaEfs, String urlS3) {
        this.id = id;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.fecha = fecha;
        this.rutaEfs = rutaEfs;
        this.urlS3 = urlS3;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public String getTransportista() {
        return transportista;
    }

    public void setTransportista(String transportista) {
        this.transportista = transportista;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getRutaEfs() {
        return rutaEfs;
    }

    public void setRutaEfs(String rutaEfs) {
        this.rutaEfs = rutaEfs;
    }

    public String getUrlS3() {
        return urlS3;
    }

    public void setUrlS3(String urlS3) {
        this.urlS3 = urlS3;
    }
}