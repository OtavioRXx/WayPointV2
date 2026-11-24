package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.model.AvisoOficial;
import com.waypoint.project_tcc_app.model.Notificacao;
import com.waypoint.project_tcc_app.model.Ocorrencia;
import com.waypoint.project_tcc_app.model.ReporteUsuario;
import com.waypoint.project_tcc_app.repository.AvisoOficialRepository;
import com.waypoint.project_tcc_app.repository.ConfirmacaoReporteRepository;
import com.waypoint.project_tcc_app.repository.NotificacaoRepository;
import com.waypoint.project_tcc_app.repository.OcorrenciaRepository;
import com.waypoint.project_tcc_app.repository.ReporteUsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DadosTemporariosCleanupService {

    public static final Duration RETENCAO_OCORRENCIAS_ENCERRADAS = Duration.ofHours(1);

    private final ReporteUsuarioRepository reporteUsuarioRepository;
    private final ConfirmacaoReporteRepository confirmacaoReporteRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final AvisoOficialRepository avisoOficialRepository;

    public DadosTemporariosCleanupService(
            ReporteUsuarioRepository reporteUsuarioRepository,
            ConfirmacaoReporteRepository confirmacaoReporteRepository,
            OcorrenciaRepository ocorrenciaRepository,
            NotificacaoRepository notificacaoRepository,
            AvisoOficialRepository avisoOficialRepository
    ) {
        this.reporteUsuarioRepository = reporteUsuarioRepository;
        this.confirmacaoReporteRepository = confirmacaoReporteRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.avisoOficialRepository = avisoOficialRepository;
    }

    @Scheduled(initialDelay = 300000, fixedDelay = 300000)
    @Transactional
    public void limparDadosTemporarios() {
        limparReportesExpirados();
        limparOcorrenciasEncerradasAntigas();
    }

    @Transactional
    public int limparReportesExpirados() {
        LocalDateTime limite = LocalDateTime.now().minus(ReporteUsuarioService.VALIDADE_FEED_REPORTES);
        List<ReporteUsuario> reportesExpirados = reporteUsuarioRepository.findByDataHoraBefore(limite);

        reportesExpirados.forEach(reporte -> {
            confirmacaoReporteRepository.deleteByReporteId(reporte.getId());
            reporteUsuarioRepository.delete(reporte);
        });

        return reportesExpirados.size();
    }

    @Transactional
    public int limparOcorrenciasEncerradasAntigas() {
        LocalDateTime limite = LocalDateTime.now().minus(RETENCAO_OCORRENCIAS_ENCERRADAS);
        List<Ocorrencia> ocorrenciasAntigas = ocorrenciaRepository.findByAtivaFalseAndDataEncerramentoBefore(limite);

        ocorrenciasAntigas.forEach(ocorrencia -> {
            List<AvisoOficial> avisos = avisoOficialRepository.findByOcorrenciaId(ocorrencia.getId());
            avisos.forEach(aviso -> aviso.setOcorrencia(null));
            avisoOficialRepository.saveAll(avisos);

            List<Notificacao> notificacoes = notificacaoRepository.findByOcorrenciaId(ocorrencia.getId());
            notificacoes.forEach(notificacao -> notificacao.setOcorrencia(null));
            notificacaoRepository.saveAll(notificacoes);
            ocorrenciaRepository.delete(ocorrencia);
        });

        return ocorrenciasAntigas.size();
    }
}
