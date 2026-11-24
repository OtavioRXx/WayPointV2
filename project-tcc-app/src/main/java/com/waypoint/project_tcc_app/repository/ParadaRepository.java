package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.Parada;
import com.waypoint.project_tcc_app.model.Sentido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParadaRepository extends JpaRepository<Parada, Long> {

    List<Parada> findByLinhaIdOrderBySentidoAscOrdemAsc(Long linhaId);

    List<Parada> findByLinhaIdAndSentidoOrderByOrdemAsc(Long linhaId, Sentido sentido);

    boolean existsByLinhaIdAndSentidoAndOrdem(Long linhaId, Sentido sentido, Integer ordem);

    boolean existsByLinhaIdAndSentidoAndOrdemAndIdNot(Long linhaId, Sentido sentido, Integer ordem, Long id);
}
