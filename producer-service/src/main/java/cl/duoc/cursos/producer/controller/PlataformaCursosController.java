package cl.duoc.cursos.producer.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.cursos.producer.dto.CursoDto;
import cl.duoc.cursos.producer.dto.ExamenRequest;
import cl.duoc.cursos.producer.dto.ExamenResponse;
import cl.duoc.cursos.producer.dto.InscripcionRequest;
import cl.duoc.cursos.producer.dto.InscripcionResponse;
import cl.duoc.cursos.producer.model.Curso;
import cl.duoc.cursos.producer.model.Examen;
import cl.duoc.cursos.producer.model.Inscripcion;
import cl.duoc.cursos.producer.model.Material;
import cl.duoc.cursos.producer.service.PlataformaService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/plataforma")
public class PlataformaCursosController {

    private final PlataformaService plataformaService;

    // Inyección limpia por constructor
    public PlataformaCursosController(PlataformaService plataformaService) {
        this.plataformaService = plataformaService;
    }

    // =========================================================================
    // MÓDULO 1: CRUD DE CURSOS (Acceso Mixto / Gestión Instructores)
    // =========================================================================

    @GetMapping("/cursos")
    @PreAuthorize("hasAnyAuthority('ROLE_INSTRUCTOR', 'ROLE_ESTUDIANTE')")
    public ResponseEntity<List<Curso>> listarCursos() {
        return ResponseEntity.ok(plataformaService.obtenerTodosLosCursos());
    }

    @GetMapping("/cursos/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_INSTRUCTOR', 'ROLE_ESTUDIANTE')")
    public ResponseEntity<Curso> obtenerCurso(@PathVariable Long id) {
        return ResponseEntity.ok(plataformaService.obtenerCursoPorId(id));
    }

    @PostMapping("/cursos")
    @PreAuthorize("hasAuthority('ROLE_INSTRUCTOR')")
    public ResponseEntity<Curso> crearNuevoCurso(@RequestBody Curso curso) {
        Curso creado = plataformaService.crearCurso(curso);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/cursos/{id}")
    @PreAuthorize("hasAuthority('ROLE_INSTRUCTOR')")
    public ResponseEntity<Curso> actualizarCurso(@PathVariable Long id, @RequestBody CursoDto dto) {
        return ResponseEntity.ok(plataformaService.actualizarCursoCompleto(id, dto));
    }

    @DeleteMapping("/cursos/{id}")
    @PreAuthorize("hasAuthority('ROLE_INSTRUCTOR')")
    public ResponseEntity<Void> borrarCurso(@PathVariable Long id) {
        plataformaService.eliminarCursoFisico(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // MÓDULO 2: INSCRIPCIONES (Exclusivo Estudiantes - Asíncrono RabbitMQ)
    // =========================================================================

    @PostMapping("/inscripciones")
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    public ResponseEntity<InscripcionResponse> inscribirEstudiante(@Valid @RequestBody InscripcionRequest request) {
        InscripcionResponse response = plataformaService.procesarInscripcion(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response); // 202 Accepted: Procesándose
    }

    @GetMapping("/inscripciones/historial")
    @PreAuthorize("hasAuthority('ROLE_INSTRUCTOR')")
    public ResponseEntity<List<Inscripcion>> verHistorialInscripciones() {
        return ResponseEntity.ok(plataformaService.consultarHistorialInscripciones());
    }

    // =========================================================================
    // MÓDULO 3: MATERIALES DE ESTUDIO (Carga y Descarga AWS S3)
    // =========================================================================

    @PostMapping(value = "/cursos/{cursoId}/material", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_INSTRUCTOR')")
    public ResponseEntity<Material> subirMaterial(
            @PathVariable Long cursoId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("instructor") String instructor) throws Exception {
        
        Material subido = plataformaService.subirMaterialCurso(
                cursoId, 
                file.getOriginalFilename(), 
                instructor, 
                file.getBytes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(subido);
    }

    @GetMapping("/materiales/{materialId}/descargar")
    @PreAuthorize("hasAnyAuthority('ROLE_INSTRUCTOR', 'ROLE_ESTUDIANTE')")
    public ResponseEntity<byte[]> descargarMaterial(@PathVariable Long materialId) throws Exception {
        byte[] archivoBytes = plataformaService.descargarMaterialS3(materialId);
        
        // Retornamos los bytes del archivo adjunto simulando una descarga real del navegador
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"material_estudio.pdf\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(archivoBytes);
    }

    @DeleteMapping("/materiales/{materialId}")
    @PreAuthorize("hasAuthority('ROLE_INSTRUCTOR')")
    public ResponseEntity<Void> eliminarMaterial(@PathVariable Long materialId) throws Exception {
        plataformaService.eliminarMaterialFisicoYLogico(materialId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // MÓDULO 4: EXÁMENES Y CALIFICACIONES
    // =========================================================================

    @PostMapping("/examenes/rendir")
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    public ResponseEntity<ExamenResponse> enviarExamen(@Valid @RequestBody ExamenRequest request) {
        ExamenResponse response = plataformaService.rendirExamenAsincrono(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response); // Encolado de examen para evaluación
    }

    @GetMapping("/cursos/{cursoId}/calificaciones")
    @PreAuthorize("hasAuthority('ROLE_INSTRUCTOR')")
    public ResponseEntity<List<Examen>> verCalificacionesRealTime(@PathVariable Long cursoId) {
        List<Examen> calificaciones = plataformaService.obtenerCalificacionesPorCurso(cursoId);
        return ResponseEntity.ok(calificaciones);
    }
}