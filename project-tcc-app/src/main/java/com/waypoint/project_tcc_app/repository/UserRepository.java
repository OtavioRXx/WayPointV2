package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.model.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByTipoUsuarioOrderByNomeAsc(TipoUsuario tipoUsuario);
}
