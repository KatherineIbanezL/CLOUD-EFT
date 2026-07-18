package cl.duoc.cursos.producer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.cursos.producer.model.Curso;

import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    
    // Método para buscar un curso específico por su código único
    Optional<Curso> findByCodigo(String codigo);
}