package cl.duoc.cursos.consumer.service;

import cl.duoc.cursos.consumer.model.Examen;
import cl.duoc.cursos.consumer.model.Inscripcion;
import cl.duoc.cursos.consumer.repository.ExamenRepository;
import cl.duoc.cursos.consumer.repository.InscripcionRepository;
import cl.duoc.cursos.shared.events.ExamenRendidoEvent;
import cl.duoc.cursos.shared.events.InscripcionCreadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
// ARQUITECTURA: Escucha la cola dinámica definida
@RabbitListener(queues = "${app.rabbitmq.queue}")
public class PlataformaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(PlataformaConsumerService.class);

    private final InscripcionRepository inscripcionRepository;
    private final ExamenRepository examenRepository;
    private final S3Service s3Service;

    // Inyección limpia por constructor
    public PlataformaConsumerService(InscripcionRepository inscripcionRepository, 
                                     ExamenRepository examenRepository, 
                                     S3Service s3Service) {
        this.inscripcionRepository = inscripcionRepository;
        this.examenRepository = examenRepository;
        this.s3Service = s3Service;
    }

    // =========================================================================
    //  TRABAJO 1: PROCESAMIENTO DE INSCRIPCIONES Y GENERACIÓN DE COMPROBANTES
    // =========================================================================
    @RabbitHandler
    @Transactional
    public void procesarInscripcion(InscripcionCreadaEvent evento) {
        log.info("Evento recibido desde RabbitMQ -> Procesando inscripción ID: {}", evento.inscripcionId());

        try {
            // 1. Busca el registro en Oracle Cloud que el productor dejó en "PENDIENTE"
            Inscripcion inscripcion = inscripcionRepository.findById(evento.inscripcionId())
                    .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada en la BD"));

            // 2. Genera un archivo físico de comprobante en memoria
            String contenidoPdfSimulado = String.format(
            "COMPROBANTE OFICIAL DE INSCRIPCIÓN%n%nID Inscripción: %d%nEstudiante: %s%nCurso: %s%nEstado: COMPLETADA",
                    inscripcion.getId(), 
                    inscripcion.getEstudiante(), 
                    evento.nombreCurso()
            );
            byte[] pdfBytes = contenidoPdfSimulado.getBytes(StandardCharsets.UTF_8);

            // 3. Sube el comprobante directamente al bucket de AWS S3
            String s3KeyDestino = "comprobantes/inscripcion_" + inscripcion.getId() + ".txt";
            String s3KeyAsignada = s3Service.subirArchivoBytes(s3KeyDestino, pdfBytes);

            // 4. Actualiza el registro de la BD con la ruta del PDF y el estado final
            inscripcion.setS3Key(s3KeyAsignada);
            inscripcion.setEstado("COMPLETADA");
            inscripcionRepository.save(inscripcion);

            log.info("Inscripción ID: {} procesada con éxito. Comprobante guardado en S3 Key: {}", 
                    inscripcion.getId(), s3KeyAsignada);

        } catch (Exception e) {
            log.error("Error crítico al procesar la inscripción: {}", e.getMessage());
            // Al lanzar la excepción, Spring y RabbitMQ reintentarán o mandarán el mensaje a la DLQ automáticamente
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // TRABAJO 2: MOTOR DE CALIFICACIÓN AUTOMÁTICA EN TIEMPO REAL
    // =========================================================================

    @RabbitHandler
    @Transactional
    public void procesarEvaluacionExamen(ExamenRendidoEvent evento) {
        log.info("Evento recibido desde RabbitMQ -> Corrigiendo Examen ID: {}", evento.examenId());

        try {
            // 1. Recupera el examen de la base de datos
            Examen examen = examenRepository.findById(evento.examenId())
                    .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado en la BD"));

            // 2. SIMULACIÓN: Lógica del motor de corrección de respuestas
            double notaCalculada = 4.0; // Nota base por entregar
            
            if (evento.respuestas() != null && !evento.respuestas().isEmpty()) {
                // Lógica de ejemplo
                if (evento.respuestas().length() > 10) {
                    notaCalculada = 7.0;
                } else {
                    notaCalculada = 5.5;
                }
            }

            // 3. Inyecta la nota calculada de forma asíncrona en Oracle Cloud
            examen.setNota(notaCalculada);
            examenRepository.save(examen);

            log.info("Examen ID: {} corregido asíncronamente. Nota final: {}", 
                    examen.getId(), notaCalculada);

        } catch (Exception e) {
            log.error("Error crítico en el motor de calificación de exámenes: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}