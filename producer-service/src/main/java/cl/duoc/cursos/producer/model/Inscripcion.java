package cl.duoc.cursos.producer.model;

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

    // Relación lógica: Muchas inscripciones pertenecen a un solo Curso
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false)
    private String estudiante;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_inscripcion", updatable = false)
    private LocalDateTime fechaInscripcion;

    // Inicializa campos obligatorios automáticamente al crear el registro
    @PrePersist
    protected void onCreate() {
        this.fechaInscripcion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = "PENDIENTE"; // Estado inicial mandatorio antes de ir a la cola
        }
    }
}