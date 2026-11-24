package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.AvisoDescartadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvisoDescartadoUsuarioRepository extends JpaRepository<AvisoDescartadoUsuario, Long> {

    List<AvisoDescartadoUsuario> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndAvisoOficialId(Long usuarioId, Long avisoOficialId);

    void deleteByAvisoOficialId(Long avisoOficialId);
}
