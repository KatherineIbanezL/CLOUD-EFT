package cl.duoc.cursos.shared.events;

import java.io.Serializable;

public record InscripcionCreadaEvent(
    Long inscripcionId,
    Long cursoId,
    String nombreCurso,
    String estudiante
) implements Serializable {}