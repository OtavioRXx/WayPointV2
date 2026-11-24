package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.dto.ConfirmacaoReporteResponse;
import com.waypoint.project_tcc_app.dto.ReporteParadaRequest;
import com.waypoint.project_tcc_app.dto.ReporteFeedResponse;
import com.waypoint.project_tcc_app.dto.ReporteUsuarioRequest;
import com.waypoint.project_tcc_app.exception.RecursoNaoEncontradoException;
import com.waypoint.project_tcc_app.exception.TaxaReporteExcedidaException;
import com.waypoint.project_tcc_app.model.ConfirmacaoReporte;
import com.waypoint.project_tcc_app.model.Linha;
import com.waypoint.project_tcc_app.model.Parada;
import com.waypoint.project_tcc_app.model.ReporteUsuario;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.repository.ConfirmacaoReporteRepository;
import com.waypoint.project_tcc_app.repository.LinhaRepository;
import com.waypoint.project_tcc_app.repository.ParadaRepository;
import com.waypoint.project_tcc_app.repository.ReporteUsuarioRepository;
import com.waypoint.project_tcc_app.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteUsuarioService {

    public static final BigDecimal CONFIABILIDADE_INICIAL = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal INCREMENTO_CONFIABILIDADE = new BigDecimal("0.10");
    public static final BigDecimal CONFIABILIDADE_MAXIMA = new BigDecimal("2.00");
    public static final int CONFIRMACOES_POR_BLOCO_CONFIABILIDADE = 5;
    public static final Duration COOLDOWN_REPORTE_PARADA = Duration.ofMinutes(15);
    public static final Duration VALIDADE_FEED_REPORTES = Duration.ofMinutes(20);

    private final ReporteUsuarioRepository reporteUsuarioRepository;
    private final UserRepository userRepository;
    private final LinhaRepository linhaRepository;
    private final ParadaRepository paradaRepository;
    private final ConfirmacaoReporteRepository confirmacaoReporteRepository;

    public ReporteUsuarioService(
            ReporteUsuarioRepository reporteUsuarioRepository,
            UserRepository userRepository,
            LinhaRepository linhaRepository,
            ParadaRepository paradaRepository,
            ConfirmacaoReporteRepository confirmacaoReporteRepository
    ) {
        this.reporteUsuarioRepository = reporteUsuarioRepository;
        this.userRepository = userRepository;
        this.linhaRepository = linhaRepository;
        this.paradaRepository = paradaRepository;
        this.confirmacaoReporteRepository = confirmacaoReporteRepository;
    }

    public List<ReporteUsuario> listarTodos() {
        return reporteUsuarioRepository.findAllByOrderByDataHoraDesc();
    }

    public List<ReporteUsuario> listarPorLinha(Long linhaId) {
        validarLinhaExistente(linhaId);
        return reporteUsuarioRepository.findByLinhaIdOrderByDataHoraDesc(linhaId);
    }

    public List<ReporteUsuario> listarPorUsuario(Long usuarioId) {
        validarUsuarioExistente(usuarioId);
        return reporteUsuarioRepository.findByUsuarioIdOrderByDataHoraDesc(usuarioId);
    }

    public ReporteUsuario buscarPorId(Long id) {
        return reporteUsuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reporte nao encontrado."));
    }

    public ReporteUsuario salvar(ReporteUsuario reporteUsuario) {
        prepararReporte(reporteUsuario);
        return reporteUsuarioRepository.save(reporteUsuario);
    }

    @Transactional
    public ReporteUsuario salvarAutenticado(ReporteUsuarioRequest request, String emailUsuarioAutenticado) {
        User usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        Parada parada = request.paradaId() == null ? null : buscarParada(request.paradaId());
        Linha linha = parada == null ? buscarLinha(request.linhaId()) : parada.getLinha();

        if (parada != null
                && request.linhaId() != null
                && !request.linhaId().equals(linha.getId())) {
            throw new IllegalArgumentException("A parada informada nao pertence a linha informada.");
        }

        LocalDateTime agora = LocalDateTime.now();
        validarCooldownReporte(usuario, linha, parada, agora);

        ReporteUsuario reporteUsuario = new ReporteUsuario();
        reporteUsuario.setUsuario(usuario);
        reporteUsuario.setLinha(linha);
        reporteUsuario.setParada(parada);
        reporteUsuario.setTipoReporte(request.tipoReporte());
        reporteUsuario.setDataHora(agora);

        return reporteUsuarioRepository.save(reporteUsuario);
    }

    @Transactional
    public ReporteUsuario salvarNaParada(ReporteParadaRequest request, String emailUsuarioAutenticado) {
        User usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        Parada parada = buscarParada(request.paradaId());
        LocalDateTime agora = LocalDateTime.now();
        validarCooldownReporte(usuario, parada.getLinha(), parada, agora);

        ReporteUsuario reporteUsuario = new ReporteUsuario();
        reporteUsuario.setUsuario(usuario);
        reporteUsuario.setLinha(parada.getLinha());
        reporteUsuario.setParada(parada);
        reporteUsuario.setTipoReporte(request.tipoReporte());
        reporteUsuario.setDataHora(agora);

        return reporteUsuarioRepository.save(reporteUsuario);
    }

    public List<ReporteFeedResponse> listarFeed(Long linhaId, String emailUsuarioAutenticado) {
        Long usuarioAutenticadoId = null;
        if (emailUsuarioAutenticado != null && !emailUsuarioAutenticado.isBlank()) {
            usuarioAutenticadoId = userRepository.findByEmail(emailUsuarioAutenticado.trim().toLowerCase())
                    .map(User::getId)
                    .orElse(null);
        }

        LocalDateTime limite = LocalDateTime.now().minus(VALIDADE_FEED_REPORTES);
        List<ReporteUsuario> reportes = linhaId == null
                ? reporteUsuarioRepository.findByDataHoraAfterOrderByDataHoraDesc(limite)
                : listarFeedPorLinha(linhaId, limite);

        Long usuarioIdFinal = usuarioAutenticadoId;
        return reportes.stream()
                .map(reporte -> toFeedResponse(reporte, usuarioIdFinal))
                .toList();
    }

    @Transactional
    public ConfirmacaoReporteResponse confirmar(Long reporteId, String emailUsuarioAutenticado) {
        ReporteUsuario reporte = buscarPorId(reporteId);
        validarReporteAindaVigente(reporte);
        User usuarioConfirmacao = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        User autor = reporte.getUsuario();

        if (autor.getId().equals(usuarioConfirmacao.getId())) {
            throw new IllegalArgumentException("Voce nao pode confirmar seu proprio reporte.");
        }

        if (confirmacaoReporteRepository.existsByReporteIdAndUsuarioId(reporteId, usuarioConfirmacao.getId())) {
            throw new IllegalArgumentException("Voce ja confirmou este reporte.");
        }

        ConfirmacaoReporte confirmacao = new ConfirmacaoReporte();
        confirmacao.setReporte(reporte);
        confirmacao.setUsuario(usuarioConfirmacao);
        confirmacao.setDataHora(LocalDateTime.now());

        try {
            confirmacaoReporteRepository.saveAndFlush(confirmacao);
            long quantidadeConfirmacoes = confirmacaoReporteRepository.countByReporteId(reporte.getId());
            atualizarConfiabilidadeDoAutorSeNecessario(reporte, autor, quantidadeConfirmacoes);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Voce ja confirmou este reporte.");
        }

        return new ConfirmacaoReporteResponse(
                reporte.getId(),
                usuarioConfirmacao.getId(),
                confirmacaoReporteRepository.countByReporteId(reporte.getId()),
                autor.getConfiabilidade(),
                "Reporte confirmado com sucesso."
        );
    }

    public void excluir(Long id) {
        ReporteUsuario reporteUsuario = buscarPorId(id);
        reporteUsuarioRepository.delete(reporteUsuario);
    }

    private void prepararReporte(ReporteUsuario reporteUsuario) {
        User usuario = buscarUsuarioDoReporte(reporteUsuario);
        Parada parada = buscarParadaDoReporte(reporteUsuario);
        Linha linha = parada == null ? buscarLinhaDoReporte(reporteUsuario) : parada.getLinha();

        if (parada != null
                && reporteUsuario.getLinha() != null
                && reporteUsuario.getLinha().getId() != null
                && !reporteUsuario.getLinha().getId().equals(linha.getId())) {
            throw new IllegalArgumentException("A parada informada nao pertence a linha informada.");
        }

        reporteUsuario.setUsuario(usuario);
        reporteUsuario.setLinha(linha);
        reporteUsuario.setParada(parada);

        if (reporteUsuario.getDataHora() == null) {
            reporteUsuario.setDataHora(LocalDateTime.now());
        }
    }

    private User buscarUsuarioDoReporte(ReporteUsuario reporteUsuario) {
        if (reporteUsuario.getUsuario() == null || reporteUsuario.getUsuario().getId() == null) {
            throw new IllegalArgumentException("Usuario deve ser informado.");
        }

        return userRepository.findById(reporteUsuario.getUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado."));
    }

    private Linha buscarLinhaDoReporte(ReporteUsuario reporteUsuario) {
        if (reporteUsuario.getLinha() == null || reporteUsuario.getLinha().getId() == null) {
            throw new IllegalArgumentException("Linha deve ser informada.");
        }

        return buscarLinha(reporteUsuario.getLinha().getId());
    }

    private Parada buscarParadaDoReporte(ReporteUsuario reporteUsuario) {
        if (reporteUsuario.getParada() == null || reporteUsuario.getParada().getId() == null) {
            return null;
        }

        return buscarParada(reporteUsuario.getParada().getId());
    }

    private Parada buscarParada(Long paradaId) {
        if (paradaId == null) {
            throw new IllegalArgumentException("Parada deve ser informada.");
        }

        return paradaRepository.findById(paradaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parada nao encontrada."));
    }

    private Linha buscarLinha(Long linhaId) {
        if (linhaId == null) {
            throw new IllegalArgumentException("Linha deve ser informada.");
        }

        return linhaRepository.findById(linhaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));
    }

    private void validarLinhaExistente(Long linhaId) {
        if (!linhaRepository.existsById(linhaId)) {
            throw new RecursoNaoEncontradoException("Linha nao encontrada.");
        }
    }

    private List<ReporteUsuario> listarFeedPorLinha(Long linhaId, LocalDateTime limite) {
        validarLinhaExistente(linhaId);
        return reporteUsuarioRepository.findByLinhaIdAndDataHoraAfterOrderByDataHoraDesc(linhaId, limite);
    }

    private void validarUsuarioExistente(Long usuarioId) {
        if (!userRepository.existsById(usuarioId)) {
            throw new RecursoNaoEncontradoException("Usuario nao encontrado.");
        }
    }

    private ReporteFeedResponse toFeedResponse(ReporteUsuario reporte, Long usuarioAutenticadoId) {
        User autor = reporte.getUsuario();
        Linha linha = reporte.getLinha();
        Parada parada = reporte.getParada();
        return new ReporteFeedResponse(
                reporte.getId(),
                linha.getId(),
                linha.getCodigo(),
                linha.getNome(),
                parada == null ? null : parada.getId(),
                parada == null ? null : parada.getNome(),
                parada == null ? null : parada.getSentido().name(),
                reporte.getTipoReporte(),
                reporte.getDataHora(),
                autor.getNome(),
                autor.getConfiabilidade(),
                confirmacaoReporteRepository.countByReporteId(reporte.getId()),
                usuarioAutenticadoId != null && confirmacaoReporteRepository.existsByReporteIdAndUsuarioId(reporte.getId(), usuarioAutenticadoId),
                usuarioAutenticadoId != null && autor.getId().equals(usuarioAutenticadoId)
        );
    }

    private User buscarUsuarioPorEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Usuario autenticado deve ser informado.");
        }

        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado."));
    }

    private void validarCooldownReporte(User usuario, Linha linha, Parada parada, LocalDateTime agora) {
        var ultimoReporteEncontrado = parada == null
                ? reporteUsuarioRepository.findTopByUsuarioIdAndLinhaIdAndParadaIsNullOrderByDataHoraDesc(
                        usuario.getId(),
                        linha.getId()
                )
                : reporteUsuarioRepository.findTopByUsuarioIdAndParadaIdOrderByDataHoraDesc(
                        usuario.getId(),
                        parada.getId()
                );

        ultimoReporteEncontrado
                .ifPresent(ultimoReporte -> {
                    LocalDateTime liberadoEm = ultimoReporte.getDataHora().plus(COOLDOWN_REPORTE_PARADA);
                    if (liberadoEm.isAfter(agora)) {
                        long minutosRestantes = Math.max(1, Duration.between(agora, liberadoEm).toMinutes() + 1);
                        throw new TaxaReporteExcedidaException(
                                "Voce ja fez um reporte neste trecho recentemente. Aguarde aproximadamente "
                                        + minutosRestantes
                                        + " minuto"
                                        + (minutosRestantes == 1 ? "" : "s")
                                        + " para fazer outro reporte."
                        );
                    }
                });
    }

    private void validarReporteAindaVigente(ReporteUsuario reporte) {
        if (reporte.getDataHora() == null || reporte.getDataHora().isBefore(LocalDateTime.now().minus(VALIDADE_FEED_REPORTES))) {
            throw new IllegalArgumentException("Este reporte expirou e nao pode mais ser confirmado.");
        }
    }

    private void atualizarConfiabilidadeDoAutorSeNecessario(
            ReporteUsuario reporte,
            User autor,
            long quantidadeConfirmacoes
    ) {
        int blocosAtuais = (int) (quantidadeConfirmacoes / CONFIRMACOES_POR_BLOCO_CONFIABILIDADE);
        int blocosRecompensados = reporte.getBlocosConfiabilidadeRecompensados() == null
                ? 0
                : reporte.getBlocosConfiabilidadeRecompensados();

        if (blocosAtuais <= blocosRecompensados) {
            return;
        }

        int novosBlocos = blocosAtuais - blocosRecompensados;
        BigDecimal confiabilidadeAtual = autor.getConfiabilidade() == null
                ? CONFIABILIDADE_INICIAL
                : autor.getConfiabilidade();
        BigDecimal novaConfiabilidade = confiabilidadeAtual.add(
                INCREMENTO_CONFIABILIDADE.multiply(BigDecimal.valueOf(novosBlocos))
        );

        if (novaConfiabilidade.compareTo(CONFIABILIDADE_MAXIMA) > 0) {
            novaConfiabilidade = CONFIABILIDADE_MAXIMA;
        }

        autor.setConfiabilidade(novaConfiabilidade.setScale(2, RoundingMode.HALF_UP));
        reporte.setBlocosConfiabilidadeRecompensados(blocosAtuais);
        reporteUsuarioRepository.save(reporte);
        userRepository.save(autor);
    }
}
