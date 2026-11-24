package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.exception.RecursoNaoEncontradoException;
import com.waypoint.project_tcc_app.model.Linha;
import com.waypoint.project_tcc_app.model.Ocorrencia;
import com.waypoint.project_tcc_app.model.Prioridade;
import com.waypoint.project_tcc_app.model.StatusLinha;
import com.waypoint.project_tcc_app.repository.LinhaRepository;
import com.waypoint.project_tcc_app.repository.OcorrenciaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class OcorrenciaService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final LinhaRepository linhaRepository;
    private final NotificacaoService notificacaoService;

    public OcorrenciaService(
            OcorrenciaRepository ocorrenciaRepository,
            LinhaRepository linhaRepository,
            NotificacaoService notificacaoService
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.linhaRepository = linhaRepository;
        this.notificacaoService = notificacaoService;
    }

    public List<Ocorrencia> listarTodas(Boolean ativa) {
        if (ativa == null) {
            return ocorrenciaRepository.findAllByOrderByDataHoraDesc();
        }

        return ocorrenciaRepository.findByAtivaOrderByDataHoraDesc(ativa);
    }

    public List<Ocorrencia> listarPorLinha(Long linhaId, Boolean ativa) {
        validarLinhaExistente(linhaId);

        if (ativa == null) {
            return ocorrenciaRepository.findByLinhaIdOrderByDataHoraDesc(linhaId);
        }

        return ocorrenciaRepository.findByLinhaIdAndAtivaOrderByDataHoraDesc(linhaId, ativa);
    }

    public Ocorrencia buscarPorId(Long id) {
        return ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrencia nao encontrada."));
    }

    public Ocorrencia salvar(Ocorrencia ocorrencia) {
        prepararOcorrencia(ocorrencia);
        Ocorrencia ocorrenciaSalva = ocorrenciaRepository.save(ocorrencia);
        atualizarStatusDaLinha(ocorrenciaSalva.getLinha().getId());
        notificarOcorrenciaAtiva(ocorrenciaSalva);
        return ocorrenciaSalva;
    }

    public Ocorrencia atualizar(Long id, Ocorrencia ocorrenciaAtualizada) {
        Ocorrencia ocorrencia = buscarPorId(id);
        Long linhaAnteriorId = ocorrencia.getLinha().getId();
        boolean estavaAtiva = Boolean.TRUE.equals(ocorrencia.getAtiva());
        boolean ficaraAtiva = Boolean.TRUE.equals(ocorrenciaAtualizada.getAtiva());

        ocorrencia.setLinha(ocorrenciaAtualizada.getLinha());
        ocorrencia.setTipo(ocorrenciaAtualizada.getTipo());
        ocorrencia.setDescricao(ocorrenciaAtualizada.getDescricao());
        ocorrencia.setPrioridade(ocorrenciaAtualizada.getPrioridade());
        ocorrencia.setAtiva(ocorrenciaAtualizada.getAtiva());
        ocorrencia.setDataHora(ocorrenciaAtualizada.getDataHora());
        atualizarDataEncerramento(ocorrencia, estavaAtiva, ficaraAtiva);

        prepararOcorrencia(ocorrencia);
        Ocorrencia ocorrenciaSalva = ocorrenciaRepository.save(ocorrencia);

        atualizarStatusDaLinha(linhaAnteriorId);
        atualizarStatusDaLinha(ocorrenciaSalva.getLinha().getId());
        notificarAtualizacaoDaOcorrencia(ocorrenciaSalva);

        return ocorrenciaSalva;
    }

    public Ocorrencia encerrar(Long id) {
        Ocorrencia ocorrencia = buscarPorId(id);
        ocorrencia.setAtiva(false);
        if (ocorrencia.getDataEncerramento() == null) {
            ocorrencia.setDataEncerramento(LocalDateTime.now());
        }

        Ocorrencia ocorrenciaSalva = ocorrenciaRepository.save(ocorrencia);
        atualizarStatusDaLinha(ocorrenciaSalva.getLinha().getId());
        notificarOcorrenciaEncerrada(ocorrenciaSalva);

        return ocorrenciaSalva;
    }

    public void excluir(Long id) {
        Ocorrencia ocorrencia = buscarPorId(id);
        Long linhaId = ocorrencia.getLinha().getId();

        notificacaoService.desvincularOcorrencia(id, true);
        ocorrenciaRepository.delete(ocorrencia);
        atualizarStatusDaLinha(linhaId);
    }

    private void prepararOcorrencia(Ocorrencia ocorrencia) {
        if (ocorrencia.getTipo() != null) {
            ocorrencia.setTipo(ocorrencia.getTipo().trim());
        }

        if (ocorrencia.getDescricao() != null) {
            ocorrencia.setDescricao(ocorrencia.getDescricao().trim());
        }

        if (ocorrencia.getPrioridade() == null) {
            ocorrencia.setPrioridade(Prioridade.MEDIA);
        }

        if (ocorrencia.getAtiva() == null) {
            ocorrencia.setAtiva(true);
        }

        if (Boolean.TRUE.equals(ocorrencia.getAtiva())) {
            ocorrencia.setDataEncerramento(null);
        } else if (ocorrencia.getDataEncerramento() == null) {
            ocorrencia.setDataEncerramento(LocalDateTime.now());
        }

        if (ocorrencia.getDataHora() == null) {
            ocorrencia.setDataHora(LocalDateTime.now());
        }

        Linha linha = buscarLinhaDaOcorrencia(ocorrencia);
        ocorrencia.setLinha(linha);
    }

    private Linha buscarLinhaDaOcorrencia(Ocorrencia ocorrencia) {
        if (ocorrencia.getLinha() == null || ocorrencia.getLinha().getId() == null) {
            throw new IllegalArgumentException("Linha deve ser informada.");
        }

        return linhaRepository.findById(ocorrencia.getLinha().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));
    }

    private void atualizarDataEncerramento(Ocorrencia ocorrencia, boolean estavaAtiva, boolean ficaraAtiva) {
        if (estavaAtiva && !ficaraAtiva) {
            ocorrencia.setDataEncerramento(LocalDateTime.now());
            return;
        }

        if (ficaraAtiva) {
            ocorrencia.setDataEncerramento(null);
        }
    }

    private void validarLinhaExistente(Long linhaId) {
        if (!linhaRepository.existsById(linhaId)) {
            throw new RecursoNaoEncontradoException("Linha nao encontrada.");
        }
    }

    private void atualizarStatusDaLinha(Long linhaId) {
        Linha linha = linhaRepository.findById(linhaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));

        StatusLinha novoStatus = ocorrenciaRepository.findFirstByLinhaIdAndAtivaTrueOrderByDataHoraDesc(linhaId)
                .map(this::definirStatusPorOcorrencia)
                .orElse(StatusLinha.NORMAL);

        linha.setStatus(novoStatus);
        linhaRepository.save(linha);
    }

    private StatusLinha definirStatusPorOcorrencia(Ocorrencia ocorrencia) {
        String texto = ((ocorrencia.getTipo() == null ? "" : ocorrencia.getTipo()) + " "
                + (ocorrencia.getDescricao() == null ? "" : ocorrencia.getDescricao()))
                .toUpperCase(Locale.ROOT);

        if (texto.contains("UNIFIC")) {
            return StatusLinha.UNIFICADA;
        }

        if (texto.contains("INTERROMP") || texto.contains("QUEBR")) {
            return StatusLinha.INTERROMPIDA;
        }

        if (texto.contains("LOT")) {
            return StatusLinha.LOTADA;
        }

        if (texto.contains("ATRAS") || texto.contains("DESVIO")) {
            return StatusLinha.ATRASADA;
        }

        if (ocorrencia.getPrioridade() == Prioridade.ALTA) {
            return StatusLinha.INTERROMPIDA;
        }

        if (ocorrencia.getPrioridade() == Prioridade.MEDIA) {
            return StatusLinha.ATRASADA;
        }

        return StatusLinha.NORMAL;
    }

    private void notificarOcorrenciaAtiva(Ocorrencia ocorrencia) {
        if (!Boolean.TRUE.equals(ocorrencia.getAtiva())) {
            return;
        }

        String titulo = "Nova ocorrencia na linha " + ocorrencia.getLinha().getCodigo();
        String mensagem = ocorrencia.getTipo();

        if (ocorrencia.getDescricao() != null && !ocorrencia.getDescricao().isBlank()) {
            mensagem += ": " + ocorrencia.getDescricao();
        }

        notificacaoService.notificarOcorrenciaAtiva(ocorrencia, titulo, mensagem);
    }

    private void notificarAtualizacaoDaOcorrencia(Ocorrencia ocorrencia) {
        if (!Boolean.TRUE.equals(ocorrencia.getAtiva())) {
            notificarOcorrenciaEncerrada(ocorrencia);
            return;
        }

        String titulo = "Atualizacao na linha " + ocorrencia.getLinha().getCodigo();
        String mensagem = ocorrencia.getTipo();

        if (ocorrencia.getDescricao() != null && !ocorrencia.getDescricao().isBlank()) {
            mensagem += ": " + ocorrencia.getDescricao();
        }

        notificacaoService.notificarOcorrenciaAtiva(ocorrencia, titulo, mensagem);
    }

    private void notificarOcorrenciaEncerrada(Ocorrencia ocorrencia) {
        Linha linhaAtualizada = linhaRepository.findById(ocorrencia.getLinha().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));

        String titulo = "Atualizacao na linha " + linhaAtualizada.getCodigo();
        String mensagem = linhaAtualizada.getStatus() == StatusLinha.NORMAL
                ? "A ocorrencia foi encerrada e a linha voltou a operar normalmente."
                : "A ocorrencia foi encerrada, mas a linha ainda possui status " + linhaAtualizada.getStatus() + ".";

        notificacaoService.notificarOcorrenciaEncerrada(ocorrencia, titulo, mensagem);
    }
}
