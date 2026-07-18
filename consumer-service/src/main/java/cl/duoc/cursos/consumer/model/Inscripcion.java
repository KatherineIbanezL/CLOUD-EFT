package cl.duoc.cursos.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "inscripciones_v3") 
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "curso_id", nullable = false)
    private Long curso;

    @Column(nullable = false)
    private String estudiante;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_inscripcion", updatable = false)
    private LocalDateTime fechaInscripcion; 

    @PrePersist
    protected void onCreate() {
        this.fechaInscripcion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = "PENDIENTE";
        }
    }
}