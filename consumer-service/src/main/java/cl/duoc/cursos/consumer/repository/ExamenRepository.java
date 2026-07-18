package cl.duoc.cursos.consumer.repository;

import cl.duoc.cursos.consumer.model.Examen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {
    
}