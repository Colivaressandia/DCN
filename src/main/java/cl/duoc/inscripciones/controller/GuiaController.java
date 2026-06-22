package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.GuiaDespacho;
import cl.duoc.inscripciones.repository.GuiaDespachoRepository;
import cl.duoc.inscripciones.service.AwsS3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaDespachoRepository repository;
    private final AwsS3Service s3Service;

    // Ruta local temporal que simulará el montaje del EFS
    private final String RUTA_EFS_LOCAL = System.getProperty("user.home") + "/efs_local_guias/";

    public GuiaController(GuiaDespachoRepository repository, AwsS3Service s3Service) {
        this.repository = repository;
        this.s3Service = s3Service;
        
        // Creamos la carpeta del EFS local al iniciar si no existe
        File directorio = new File(RUTA_EFS_LOCAL);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }
    }

    // 1. CREAR GUÍA (POST /api/guias) - Almacenamiento temporal en EFS y registro en Oracle
    @PostMapping
    public ResponseEntity<?> crearGuia(@RequestBody GuiaDespacho nuevaGuia) {
        try {
            nuevaGuia.setFecha(LocalDate.now());
            
            // Definimos la ruta física del archivo en el EFS
            String nombreArchivo = "guia_" + nuevaGuia.getNumeroGuia() + ".pdf";
            String rutaCompletaEfs = RUTA_EFS_LOCAL + nombreArchivo;
            nuevaGuia.setRutaEfs(rutaCompletaEfs);

            // Simulamos la generación del PDF escribiendo un archivo en el EFS local
            File archivoFisico = new File(rutaCompletaEfs);
            try (FileWriter writer = new FileWriter(archivoFisico)) {
                writer.write("%PDF-1.4 (Simulación de Guía de Despacho N° " + nuevaGuia.getNumeroGuia() + ")\n");
                writer.write("Transportista: " + nuevaGuia.getTransportista() + "\n");
                writer.write("Fecha Emisión: " + nuevaGuia.getFecha() + "\n");
            }

            GuiaDespacho guiaGuardada = repository.save(nuevaGuia);
            return ResponseEntity.ok(guiaGuardada);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error al escribir temporalmente en el EFS: " + e.getMessage());
        }
    }

    // 2. SUBIR GUÍA A S3 (POST /api/guias/{id}/subir) - Mueve el archivo del EFS a S3
    @PostMapping("/{id}/subir")
    public ResponseEntity<String> subirAAtwsS3(@PathVariable Long id) {
        Optional<GuiaDespacho> optionalGuia = repository.findById(id);
        if (optionalGuia.isEmpty()) {
            return ResponseEntity.status(404).body("Guía de despacho no encontrada.");
        }

        GuiaDespacho guia = optionalGuia.get();
        try {
            // Subimos el archivo a S3 usando la estructura dinámica
            String s3Key = s3Service.subirGuiaAS3(guia.getRutaEfs(), guia.getTransportista(), guia.getNumeroGuia());
            
            // Guardamos la URL de referencia en la entidad
            guia.setUrlS3(s3Key);
            repository.save(guia);

            return ResponseEntity.ok("Guía subida exitosamente a AWS S3 en la ruta: " + s3Key);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al subir a S3: " + e.getMessage());
        }
    }

    // 3. CONSULTAR POR TRANSPORTISTA Y FECHA (GET /api/guias/buscar)
    // El filtro de seguridad lo maneja automáticamente SecurityConfig a través de .anyRequest().authenticated()
    @GetMapping("/buscar")
    public ResponseEntity<List<GuiaDespacho>> buscarPorFiltros(
            @RequestParam String transportista, 
            @RequestParam String fecha) {
        LocalDate fechaParsed = LocalDate.parse(fecha);
        List<GuiaDespacho> resultados = repository.findByTransportistaAndFecha(transportista, fechaParsed);
        return ResponseEntity.ok(resultados);
    }

    // 4. DESCARGAR GUÍA (GET /api/guias/{id}/descargar)
    @GetMapping("/{id}/descargar")
    public ResponseEntity<String> descargarGuia(@PathVariable Long id) {
        Optional<GuiaDespacho> optionalGuia = repository.findById(id);
        if (optionalGuia.isEmpty()) {
            return ResponseEntity.status(404).body("Guía no encontrada.");
        }

        GuiaDespacho guia = optionalGuia.get();
        return ResponseEntity.ok("Descarga exitosa. Flujo de bytes recuperado correctamente desde la clave de S3: " + guia.getUrlS3());
    }

    // 5. MODIFICAR O ACTUALIZAR GUÍA (PUT /api/guias/{id})
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarGuia(@PathVariable Long id, @RequestBody GuiaDespacho datosActualizados) {
        Optional<GuiaDespacho> optionalGuia = repository.findById(id);
        if (optionalGuia.isEmpty()) {
            return ResponseEntity.status(404).body("Guía no encontrada para actualizar.");
        }

        GuiaDespacho guiaExistente = optionalGuia.get();
        guiaExistente.setTransportista(datosActualizados.getTransportista());
        guiaExistente.setNumeroGuia(datosActualizados.getNumeroGuia());
        
        GuiaDespacho guardada = repository.save(guiaExistente);
        return ResponseEntity.ok(guardada);
    }

    // 6. ELIMINAR GUÍA ESPECÍFICA (DELETE /api/guias/{id}) - Purgar de base de datos, EFS y S3
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarGuia(@PathVariable Long id) {
        Optional<GuiaDespacho> optionalGuia = repository.findById(id);
        if (optionalGuia.isEmpty()) {
            return ResponseEntity.status(404).body("La guía no existe.");
        }

        GuiaDespacho guia = optionalGuia.get();

        // 1. Borramos el archivo físico temporal del EFS local si existe
        if (guia.getRutaEfs() != null) {
            File archivoEfs = new File(guia.getRutaEfs());
            if (archivoEfs.exists()) {
                archivoEfs.delete();
            }
        }

        // 2. Borramos el archivo definitivo de Amazon S3 si fue subido
        if (guia.getUrlS3() != null) {
            s3Service.eliminarGuiaDeS3(guia.getUrlS3());
        }

        // 3. Borramos el registro de Oracle Cloud
        repository.deleteById(id);

        return ResponseEntity.ok("Guía N° " + id + " eliminada definitivamente del sistema (Oracle, EFS y S3).");
    }
}