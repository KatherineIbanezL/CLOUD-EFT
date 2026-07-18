package cl.duoc.cursos.producer.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record ExamenRequest(
    @NotNull(message = "El ID de inscripción es obligatorio") Long inscripcionId,
    @NotBlank(message = "Las respuestas no pueden estar vacías") String respuestas
) {}