package cl.duoc.cursos.producer.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record InscripcionRequest(
    @NotNull(message = "El ID del curso es obligatorio") Long cursoId,
    @NotBlank(message = "El nombre del estudiante es obligatorio") String estudiante
) {}