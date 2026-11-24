package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.Notificacao;
import com.waypoint.project_tcc_app.model.StatusNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findAllByOrderByDataCriacaoDesc();

    List<Notificacao> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    List<Notificacao> findByUsuarioIdAndLidaOrderByDataCriacaoDesc(Long usuarioId, Boolean lida);

    List<Notificacao> findByUsuarioIdAndStatusOrderByDataCriacaoDesc(Long usuarioId, StatusNotificacao status);

    List<Notificacao> findByUsuarioIdAndLidaAndStatusOrderByDataCriacaoDesc(
            Long usuarioId,
            Boolean lida,
            StatusNotificacao status
    );

    List<Notificacao> findByOcorrenciaIdAndStatus(Long ocorrenciaId, StatusNotificacao status);

    List<Notificacao> findByOcorrenciaId(Long ocorrenciaId);

    List<Notificacao> findByGrupoNotificacao(String grupoNotificacao);

    List<Notificacao> findByAvisoOficialId(Long avisoOficialId);

    List<Notificacao> findByAvisoOficialIsNull();

    boolean existsByUsuarioIdAndAvisoOficialId(Long usuarioId, Long avisoOficialId);

    boolean existsByUsuarioIdAndGrupoNotificacao(Long usuarioId, String grupoNotificacao);

    void deleteByAvisoOficialId(Long avisoOficialId);

    void deleteByGrupoNotificacao(String grupoNotificacao);

    List<Notificacao> findByLinhaIdAndTituloAndMensagemAndOcorrenciaIsNull(
            Long linhaId,
            String titulo,
            String mensagem
    );

    List<Notificacao> findByLinhaIdAndStatusAndOcorrenciaIsNull(Long linhaId, StatusNotificacao status);
}
