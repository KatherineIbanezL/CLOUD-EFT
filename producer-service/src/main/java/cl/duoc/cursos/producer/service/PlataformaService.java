package cl.duoc.cursos.producer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.cursos.producer.dto.CursoDto;
import cl.duoc.cursos.producer.dto.ExamenRequest;
import cl.duoc.cursos.producer.dto.ExamenResponse;
import cl.duoc.cursos.producer.dto.InscripcionRequest;
import cl.duoc.cursos.producer.dto.InscripcionResponse;
import cl.duoc.cursos.producer.model.Curso;
import cl.duoc.cursos.producer.model.Examen;
import cl.duoc.cursos.producer.model.Inscripcion;
import cl.duoc.cursos.producer.model.Material;
import cl.duoc.cursos.producer.repository.CursoRepository;
import cl.duoc.cursos.producer.repository.ExamenRepository;
import cl.duoc.cursos.producer.repository.InscripcionRepository;
import cl.duoc.cursos.producer.repository.MaterialRepository;
import cl.duoc.cursos.shared.events.ExamenRendidoEvent;
import cl.duoc.cursos.shared.events.InscripcionCreadaEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PlataformaService {

    private final CursoRepository cursoRepository;
    private final ExamenRepository examenRepository;
    private final InscripcionRepository inscripcionRepository;
    private final MaterialRepository materialRepository;
    private final PlataformaProducerService plataformaProducerService; 
    private final S3Service s3Service;

    // Inyección por constructor limpia
    public PlataformaService(CursoRepository cursoRepository, 
                             ExamenRepository examenRepository,
                             InscripcionRepository inscripcionRepository, 
                             MaterialRepository materialRepository,
                             PlataformaProducerService plataformaProducerService, 
                             S3Service s3Service) {
        this.cursoRepository = cursoRepository;
        this.examenRepository = examenRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.materialRepository = materialRepository;
        this.plataformaProducerService = plataformaProducerService;
        this.s3Service = s3Service;
    }

    // ==========================================
    // 1. CRUD DE CURSOS (Para el Instructor)
    // ==========================================

    public List<Curso> obtenerTodosLosCursos() {
        return cursoRepository.findAll();
    }

    public Curso obtenerCursoPorId(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de curso no encontrado: " + id));
    }

    @Transactional
    public Curso crearCurso(Curso curso) {
        // Genera un código único automático si no viene en el request (ej: CRS-A1B2C3)
        if (curso.getCodigo() == null || curso.getCodigo().isEmpty()) {
            String codigoAuto = "CRS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            curso.setCodigo(codigoAuto);
        }
        return cursoRepository.save(curso);
    }

    @Transactional
    public Curso actualizarCursoCompleto(Long id, CursoDto dto) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado para actualizar"));

        if (dto.getNombre() != null) {
            curso.setNombre(dto.getNombre());
        }
        if (dto.getInstructor() != null) {
            curso.setInstructor(dto.getInstructor());
        }
        if (dto.getCupos() != null) {
            curso.setCupos(dto.getCupos());
        }

        // Registra la auditoría de modificación
        curso.setFechaModificacion(LocalDateTime.now());

        return cursoRepository.save(curso);
    }

    @Transactional
    public void eliminarCursoFisico(Long id) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se puede eliminar un curso inexistente con ID: " + id));
        
        // se borrarán automáticamente sus inscripciones y materiales en Oracle
        cursoRepository.delete(curso);
    }

    // ==========================================
    // 2. INSCRIPCIONES ASÍNCRONAS (RabbitMQ)
    // ==========================================

    @Transactional
    public InscripcionResponse procesarInscripcion(InscripcionRequest request) {
        Curso curso = cursoRepository.findById(request.cursoId())
                .orElseThrow(() -> new IllegalArgumentException("Error: El curso solicitado no existe."));

        // Validar cupos disponibles antes de encolar
        if (curso.getCupos() <= 0) {
            throw new IllegalStateException("Operación rechazada: no quedan cupos disponibles para el curso: " + curso.getNombre());
        }

        // Crea la inscripción en Oracle Cloud en estado PENDIENTE
        Inscripcion inscripcion = Inscripcion.builder()
                .curso(curso)
                .estudiante(request.estudiante())
                .estado("PENDIENTE")
                .build();

        Inscripcion guardada = inscripcionRepository.save(inscripcion);

        // Descuenta un cupo temporalmente
        curso.setCupos(curso.getCupos() - 1);
        cursoRepository.save(curso);

        // Instanciar el Record del Evento
        InscripcionCreadaEvent evento = new InscripcionCreadaEvent(
                guardada.getId(),
                curso.getId(),
                curso.getNombre(),
                guardada.getEstudiante()
        );

        // Despacha a RabbitMQ (Cola 1) con Exchange
        plataformaProducerService.enviarEventoInscripcion(evento);

        return new InscripcionResponse(
                "Inscripción recibida con éxito. El comprobante se está generando en segundo plano.",
                guardada.getId(),
                guardada.getEstado()
        );
    }

    // ==========================================
    // 3. GESTIÓN DE MATERIALES Y AWS S3
    // ==========================================

    @Transactional
    public Material subirMaterialCurso(Long cursoId, String nombreArchivo, String instructor, byte[] archivoBytes) throws Exception {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado."));

        String s3KeyEstructurada = "materiales/curso_" + cursoId + "/" + nombreArchivo;
        
        // Subir los bytes reales a AWS S3
        String s3Key = s3Service.subirArchivoBytes(s3KeyEstructurada, archivoBytes); 

        Material material = Material.builder()
                .curso(curso)
                .nombreArchivo(nombreArchivo)
                .s3Key(s3Key)
                .subidoPor(instructor)
                .build();

        return materialRepository.save(material);
    }

    public byte[] descargarMaterialS3(Long materialId) throws Exception {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el material de estudio con ID: " + materialId));
        
        if (material.getS3Key() == null || material.getS3Key().isEmpty()) {
            throw new IllegalStateException("El archivo no posee una llave válida de AWS S3 asociada.");
        }
        
        // Descarga desde AWS S3
        return s3Service.descargarArchivo(material.getS3Key());
    }

    @Transactional
    public void eliminarMaterialFisicoYLogico(Long materialId) throws Exception {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material no encontrado con ID: " + materialId));

        // Remoción física en AWS S3
        if (material.getS3Key() != null && !material.getS3Key().isEmpty()) {
            try {
                s3Service.eliminarArchivo(material.getS3Key());
            } catch (Exception e) {
                System.err.println("Advertencia AWS: No se pudo borrar el archivo físico: " + e.getMessage());
            }
        }

        // Remoción lógica en Oracle
        materialRepository.delete(material);
    }

    // ==========================================
    // 4. CONSULTA DE HISTORIALES
    // ==========================================

    public List<Inscripcion> consultarHistorialInscripciones() {

        // Este método listará todas las inscripciones
        return inscripcionRepository.findAll();
    }

    // =========================================================================
    // 5. MÓDULO DE EXÁMENES Y NOTAS EN TIEMPO REAL (Requerimiento Completo)
    // =========================================================================

    @Transactional
    public ExamenResponse rendirExamenAsincrono(ExamenRequest request) {
        // Valida que el alumno realmente esté inscrito en la plataforma
        Inscripcion inscripcion = inscripcionRepository.findById(request.inscripcionId())
                .orElseThrow(() -> new IllegalArgumentException("Error: La inscripción del alumno no existe."));

        // Crea el registro del examen con la nota vacía (En revisión)
        Examen examen = Examen.builder()
                .inscripcion(inscripcion)
                .respuestas(request.respuestas())
                .build();

        Examen guardado = examenRepository.save(examen);

        // Instancia el evento para RabbitMQ
        ExamenRendidoEvent evento = new ExamenRendidoEvent(
                guardado.getId(),
                inscripcion.getId(),
                guardado.getRespuestas()
        );

        plataformaProducerService.enviarEventoExamen(evento);

        return new ExamenResponse(
                "Examen enviado con éxito. Las respuestas están siendo procesadas por el motor de evaluación.",
                guardado.getId(),
                "EN_REVISION"
        );
    }

    // Endpoint Instructor para calificaciones en tiempo real
    public List<Examen> obtenerCalificacionesPorCurso(Long cursoId) {
        return examenRepository.findByInscripcionCursoId(cursoId);
    }
}