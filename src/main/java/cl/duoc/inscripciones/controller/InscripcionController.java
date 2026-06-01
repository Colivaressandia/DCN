package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.Curso;
import cl.duoc.inscripciones.model.Inscripcion;
import cl.duoc.inscripciones.repository.CursoRepository;
import cl.duoc.inscripciones.repository.InscripcionRepository;
import cl.duoc.inscripciones.service.AwsS3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class InscripcionController {

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private AwsS3Service awsS3Service;

    // ===================================================================
    // ENDPOINTS DE LA SEMANA 1 (Mantenidos intactos para persistencia Cloud)
    // ===================================================================

    // 1. ENDPOINT: Consultar lista de cursos disponibles
    @GetMapping("/cursos")
    public List<Curso> listarCursos() {
        return cursoRepository.findAll();
    }

    // 2. ENDPOINT: Agregar nuevos cursos a la oferta educativa
    @PostMapping("/cursos")
    public Curso agregarCurso(@RequestBody Curso curso) {
        return cursoRepository.save(curso);
    }

    // 3. ENDPOINT: Inscribir estudiantes en uno o más cursos
    @PostMapping("/inscripciones")
    public Inscripcion inscribirEstudiante(@RequestParam String estudiante, @RequestBody List<Long> cursosIds) {
        List<String> nombresCursos = new ArrayList<>();
        Double total = 0.0;

        for (Long id : cursosIds) {
            Curso curso = cursoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Curso no encontrado con ID: " + id));
            nombresCursos.add(curso.getNombre() + " ($" + curso.getCosto() + ")");
            total += curso.getCosto();
        }

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setEstudiante(estudiante);
        inscripcion.setCursosSeleccionados(nombresCursos);
        inscripcion.setTotalPagar(total);

        return inscripcionRepository.save(inscripcion);
    }

    // ===================================================================
    // NUEVOS ENDPOINTS - SEMANA 2 (Gestión Cloud Native AWS S3)
    // ===================================================================

    // 4. ENDPOINT: Generar resumen de base de datos y subirlo a AWS S3
    // POST http://localhost:8080/api/inscripciones/{id}/subir
    @PostMapping("/inscripciones/{id}/subir")
    public ResponseEntity<String> subirResumenS3(@PathVariable Long id) {
        Inscripcion inscripcion = inscripcionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscripción no encontrada con ID: " + id));

        String cursosConcatenados = String.join("\n- ", inscripcion.getCursosSeleccionados());
        String contenidoTexto = awsS3Service.generarContenidoTexto(
                inscripcion.getId(),
                inscripcion.getEstudiante(),
                "- " + cursosConcatenados,
                inscripcion.getTotalPagar()
        );

        awsS3Service.subirArchivo(id, contenidoTexto);
        return ResponseEntity.ok("Resumen de inscripción N° " + id + " subido exitosamente a AWS S3.");
    }

    // 5. ENDPOINT: Descargar el archivo del resumen de inscripción desde S3
    // GET http://localhost:8080/api/inscripciones/{id}/descargar
    @GetMapping("/inscripciones/{id}/descargar")
    public ResponseEntity<byte[]> descargarResumenS3(@PathVariable Long id) {
        String contenido = awsS3Service.descargarArchivo(id);
        byte[] data = contenido.getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "resumen_inscripcion_" + id + ".txt");

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    // 6. ENDPOINT: Modificar el contenido del archivo en S3 en caso de error
    // PUT http://localhost:8080/api/inscripciones/{id}/modificar
    @PutMapping("/inscripciones/{id}/modificar")
    public ResponseEntity<String> modificarResumenS3(@PathVariable Long id, @RequestBody String nuevoContenido) {
        // Al enviar una petición PUT con el nuevo texto plano, S3 sobreescribe el archivo previo de forma segura
        awsS3Service.subirArchivo(id, nuevoContenido);
        return ResponseEntity.ok("Archivo de inscripción N° " + id + " modificado correctamente en AWS S3.");
    }

    // 7. ENDPOINT: Eliminar el archivo físico de S3
    // DELETE http://localhost:8080/api/inscripciones/{id}/borrar
    @DeleteMapping("/inscripciones/{id}/borrar")
    public ResponseEntity<String> borrarResumenS3(@PathVariable Long id) {
        awsS3Service.borrarArchivo(id);
        return ResponseEntity.ok("Archivo de inscripción N° " + id + " eliminado definitivamente del bucket AWS S3.");
    }
}