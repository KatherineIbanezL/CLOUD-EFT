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
@Table(name = "examenes_v3")
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: Muchos exámenes rendidos pueden pertenecer a una misma Inscripción
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @Column(nullable = false)
    private String respuestas;

    // La nota parte vacía (null) hasta que el Worker la calcule asíncronamente
    private Double nota;

    @Column(name = "fecha_rendicion", updatable = false)
    private LocalDateTime fechaRendicion;

    @PrePersist
    protected void onCreate() {
        this.fechaRendicion = LocalDateTime.now();
    }
}