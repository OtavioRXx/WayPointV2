package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.AvisoOficial;
import com.waypoint.project_tcc_app.model.StatusNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvisoOficialRepository extends JpaRepository<AvisoOficial, Long> {

    List<AvisoOficial> findAllByOrderByDataCriacaoDesc();

    List<AvisoOficial> findByStatusOrderByDataCriacaoDesc(StatusNotificacao status);

    List<AvisoOficial> findByEnviarParaTodosTrueOrderByDataCriacaoDesc();

    List<AvisoOficial> findByEnviarParaTodosFalseAndLinhaIdInAndStatusOrderByDataCriacaoDesc(
            Collection<Long> linhaIds,
            StatusNotificacao status
    );

    List<AvisoOficial> findByLinhaIdAndStatusAndOcorrenciaIsNull(Long linhaId, StatusNotificacao status);

    List<AvisoOficial> findByOcorrenciaIdAndStatus(Long ocorrenciaId, StatusNotificacao status);

    List<AvisoOficial> findByOcorrenciaId(Long ocorrenciaId);

    Optional<AvisoOficial> findByGrupoNotificacao(String grupoNotificacao);
}
