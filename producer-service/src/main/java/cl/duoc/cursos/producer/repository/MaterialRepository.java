package cl.duoc.cursos.producer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.cursos.producer.model.Material;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    
    // Método para listar todo el material que pertenece a un curso específico
    List<Material> findByCursoId(Long cursoId);
}