package cl.duoc.cursos.shared.events;

import java.io.Serializable;

// Implementación de Serializable para que Spring AMQP pueda convertirlo a bytes/JSON 
public record InscripcionCreadaEvent(
    Long inscripcionId,
    Long cursoId,
    String nombreCurso,
    String estudiante
) implements Serializable {}