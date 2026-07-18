package cl.duoc.cursos.producer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CursoDto {
    private Long id;
    private String codigo;
    private String nombre;
    private String instructor;
    private Integer cupos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
}