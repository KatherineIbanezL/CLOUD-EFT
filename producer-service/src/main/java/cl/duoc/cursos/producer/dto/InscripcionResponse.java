package cl.duoc.cursos.producer.dto;

public record InscripcionResponse(
    String mensaje,
    Long inscripcionId,
    String estado
) {}