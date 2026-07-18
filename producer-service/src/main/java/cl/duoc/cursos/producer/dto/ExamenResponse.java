package cl.duoc.cursos.producer.dto;

public record ExamenResponse(
    String mensaje,
    Long examenId,
    String estado
) {}