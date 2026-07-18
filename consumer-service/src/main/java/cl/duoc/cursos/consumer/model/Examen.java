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
@Table(name = "examenes_v3") 
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inscripcion_id", nullable = false)
    private Long inscripcion;

    @Column(nullable = false)
    private String respuestas;

    private Double nota;

    @Column(name = "fecha_rendicion", updatable = false)
    private LocalDateTime fechaRendicion; 

    @PrePersist
    protected void onCreate() {
        this.fechaRendicion = LocalDateTime.now();
    }
}