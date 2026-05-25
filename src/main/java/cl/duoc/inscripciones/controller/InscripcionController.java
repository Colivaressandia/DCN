package cl.duoc.inscripciones.controller;

import cl.duoc.inscripciones.model.Curso;
import cl.duoc.inscripciones.model.Inscripcion;
import cl.duoc.inscripciones.repository.CursoRepository;
import cl.duoc.inscripciones.repository.InscripcionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    // POST http://localhost:8080/api/inscripciones?estudiante=TuNombre
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
}