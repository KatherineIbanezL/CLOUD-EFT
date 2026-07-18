package cl.duoc.cursos.producer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.cursos.producer.model.Examen;

import java.util.List;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {

    // Busca todos los exámenes que pertenecen a las inscripciones de un curso específico
    List<Examen> findByInscripcionCursoId(Long cursoId);
}