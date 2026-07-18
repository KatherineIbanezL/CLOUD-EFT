package cl.duoc.cursos.shared.events;

import java.io.Serializable;

public record ExamenRendidoEvent(
    Long examenId,
    Long inscripcionId,
    String respuestas
) implements Serializable {}