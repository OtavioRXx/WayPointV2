package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.UsuarioLinhaFavorita;
import com.waypoint.project_tcc_app.model.UsuarioLinhaFavoritaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioLinhaFavoritaRepository extends JpaRepository<UsuarioLinhaFavorita, UsuarioLinhaFavoritaId> {

    List<UsuarioLinhaFavorita> findByUsuarioIdOrderByLinhaNomeAsc(Long usuarioId);

    List<UsuarioLinhaFavorita> findByLinhaId(Long linhaId);
}
