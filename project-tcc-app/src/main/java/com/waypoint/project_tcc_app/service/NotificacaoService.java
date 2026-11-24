package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.dto.AvisoNotificacaoResponse;
import com.waypoint.project_tcc_app.dto.AvisoPublicoResponse;
import com.waypoint.project_tcc_app.dto.LimpezaHistoricoNotificacoesResponse;
import com.waypoint.project_tcc_app.dto.NotificacaoLinhaResponse;
import com.waypoint.project_tcc_app.exception.RecursoNaoEncontradoException;
import com.waypoint.project_tcc_app.model.AvisoDescartadoUsuario;
import com.waypoint.project_tcc_app.model.AvisoOficial;
import com.waypoint.project_tcc_app.model.Linha;
import com.waypoint.project_tcc_app.model.Notificacao;
import com.waypoint.project_tcc_app.model.Ocorrencia;
import com.waypoint.project_tcc_app.model.StatusNotificacao;
import com.waypoint.project_tcc_app.model.TipoNotificacao;
import com.waypoint.project_tcc_app.model.TipoUsuario;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.repository.AvisoDescartadoUsuarioRepository;
import com.waypoint.project_tcc_app.repository.AvisoOficialRepository;
import com.waypoint.project_tcc_app.repository.LinhaRepository;
import com.waypoint.project_tcc_app.repository.NotificacaoRepository;
import com.waypoint.project_tcc_app.repository.UserRepository;
import com.waypoint.project_tcc_app.repository.UsuarioLinhaFavoritaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final AvisoOficialRepository avisoOficialRepository;
    private final AvisoDescartadoUsuarioRepository avisoDescartadoUsuarioRepository;
    private final UserRepository userRepository;
    private final LinhaRepository linhaRepository;
    private final UsuarioLinhaFavoritaRepository usuarioLinhaFavoritaRepository;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentHashMap<Long, Object> syncLocks = new ConcurrentHashMap<>();

    public NotificacaoService(
            NotificacaoRepository notificacaoRepository,
            AvisoOficialRepository avisoOficialRepository,
            AvisoDescartadoUsuarioRepository avisoDescartadoUsuarioRepository,
            UserRepository userRepository,
            LinhaRepository linhaRepository,
            UsuarioLinhaFavoritaRepository usuarioLinhaFavoritaRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.notificacaoRepository = notificacaoRepository;
        this.avisoOficialRepository = avisoOficialRepository;
        this.avisoDescartadoUsuarioRepository = avisoDescartadoUsuarioRepository;
        this.userRepository = userRepository;
        this.linhaRepository = linhaRepository;
        this.usuarioLinhaFavoritaRepository = usuarioLinhaFavoritaRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public List<Notificacao> listarTodas() {
        return notificacaoRepository.findAllByOrderByDataCriacaoDesc();
    }

    public List<Notificacao> listarPorUsuario(Long usuarioId, Boolean lida, StatusNotificacao status) {
        validarUsuarioExistente(usuarioId);
        sincronizarAvisosParaUsuario(usuarioId);

        if (lida == null && status == null) {
            return notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId);
        }

        if (lida == null) {
            return notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(usuarioId, status);
        }

        if (status == null) {
            return notificacaoRepository.findByUsuarioIdAndLidaOrderByDataCriacaoDesc(usuarioId, lida);
        }

        return notificacaoRepository.findByUsuarioIdAndLidaAndStatusOrderByDataCriacaoDesc(usuarioId, lida, status);
    }

    public void sincronizarAvisosParaUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return;
        }

        Object lock = syncLocks.computeIfAbsent(usuarioId, id -> new Object());
        synchronized (lock) {
            transactionTemplate.executeWithoutResult(tx -> sincronizarAvisosParaUsuarioInterno(usuarioId));
        }
    }

    private void sincronizarAvisosParaUsuarioInterno(Long usuarioId) {
        User usuario = userRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return;
        }

        List<AvisoOficial> avisosElegiveis = new ArrayList<>(
                avisoOficialRepository.findByEnviarParaTodosTrueOrderByDataCriacaoDesc()
        );

        List<Long> linhasFavoritasIds = usuarioLinhaFavoritaRepository.findByUsuarioIdOrderByLinhaNomeAsc(usuarioId)
                .stream()
                .map(favorito -> favorito.getLinha().getId())
                .toList();

        if (!linhasFavoritasIds.isEmpty()) {
            avisosElegiveis.addAll(
                    avisoOficialRepository.findByEnviarParaTodosFalseAndLinhaIdInAndStatusOrderByDataCriacaoDesc(
                            linhasFavoritasIds,
                            StatusNotificacao.ATIVA
                    )
            );
        }

        if (avisosElegiveis.isEmpty()) {
            return;
        }

        List<Notificacao> existentes = notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId);
        Set<Long> avisoIdsExistentes = new HashSet<>();
        Set<String> gruposExistentes = new HashSet<>();
        List<Notificacao> atualizacoesStatus = new ArrayList<>();

        for (Notificacao notificacao : existentes) {
            if (notificacao.getAvisoOficial() != null && notificacao.getAvisoOficial().getId() != null) {
                avisoIdsExistentes.add(notificacao.getAvisoOficial().getId());
                if (notificacao.getStatus() != notificacao.getAvisoOficial().getStatus()) {
                    notificacao.setStatus(notificacao.getAvisoOficial().getStatus());
                    atualizacoesStatus.add(notificacao);
                }
            }
            if (notificacao.getGrupoNotificacao() != null && !notificacao.getGrupoNotificacao().isBlank()) {
                gruposExistentes.add(notificacao.getGrupoNotificacao());
            }
        }

        if (!atualizacoesStatus.isEmpty()) {
            notificacaoRepository.saveAll(atualizacoesStatus);
        }

        Set<Long> avisoIdsDescartados = new HashSet<>();
        avisoDescartadoUsuarioRepository.findByUsuarioId(usuarioId)
                .forEach(descartado -> avisoIdsDescartados.add(descartado.getAvisoOficial().getId()));

        List<Notificacao> novasNotificacoes = new ArrayList<>();
        Set<Long> avisosProcessados = new HashSet<>();

        for (AvisoOficial aviso : avisosElegiveis) {
            if (!avisosProcessados.add(aviso.getId())) {
                continue;
            }
            if (avisoIdsDescartados.contains(aviso.getId())) {
                continue;
            }
            if (avisoIdsExistentes.contains(aviso.getId()) || gruposExistentes.contains(aviso.getGrupoNotificacao())) {
                continue;
            }

            Notificacao nova = new Notificacao();
            nova.setUsuario(usuario);
            nova.setAvisoOficial(aviso);
            nova.setLinha(aviso.getLinha());
            nova.setOcorrencia(aviso.getOcorrencia());
            nova.setGrupoNotificacao(aviso.getGrupoNotificacao());
            nova.setTipoNotificacao(aviso.getTipoNotificacao());
            nova.setTitulo(aviso.getTitulo());
            nova.setMensagem(aviso.getMensagem());
            nova.setStatus(aviso.getStatus());
            nova.setLida(aviso.getStatus() == StatusNotificacao.SUPERADA);
            nova.setDataCriacao(aviso.getDataCriacao());
            novasNotificacoes.add(nova);
        }

        if (!novasNotificacoes.isEmpty()) {
            notificacaoRepository.saveAll(novasNotificacoes);
        }
    }

    public Notificacao buscarPorId(Long id) {
        return notificacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificacao nao encontrada."));
    }

    public Notificacao salvar(Notificacao notificacao) {
        prepararNotificacao(notificacao);
        return notificacaoRepository.save(notificacao);
    }

    @Transactional
    public NotificacaoLinhaResponse notificarUsuarios(
            Long linhaId,
            Boolean enviarParaTodos,
            String titulo,
            String mensagem,
            Boolean superarNotificacoesAtuaisDaLinha
    ) {
        if (Boolean.TRUE.equals(enviarParaTodos)) {
            return notificarTodosUsuarios(titulo, mensagem);
        }

        if (linhaId == null) {
            throw new IllegalArgumentException("Linha deve ser informada quando o aviso nao for geral.");
        }

        return notificarUsuariosDaLinha(linhaId, titulo, mensagem, superarNotificacoesAtuaisDaLinha);
    }

    public List<AvisoNotificacaoResponse> listarAvisosOficiais() {
        List<AvisoOficial> avisosOficiais = avisoOficialRepository.findAllByOrderByDataCriacaoDesc();
        List<Notificacao> todasNotificacoes = listarTodas();

        Map<Long, List<Notificacao>> notificacoesPorAvisoId = new LinkedHashMap<>();
        Map<String, List<Notificacao>> gruposLegados = new LinkedHashMap<>();

        for (Notificacao notificacao : todasNotificacoes) {
            if (notificacao.getAvisoOficial() != null && notificacao.getAvisoOficial().getId() != null) {
                notificacoesPorAvisoId
                        .computeIfAbsent(notificacao.getAvisoOficial().getId(), id -> new ArrayList<>())
                        .add(notificacao);
            } else {
                gruposLegados
                        .computeIfAbsent(chaveAviso(notificacao), chave -> new ArrayList<>())
                        .add(notificacao);
            }
        }

        List<AvisoNotificacaoResponse> resposta = new ArrayList<>();

        for (AvisoOficial aviso : avisosOficiais) {
            List<Notificacao> copias = notificacoesPorAvisoId.getOrDefault(aviso.getId(), List.of());
            resposta.add(toAvisoResponse(aviso, copias));
        }

        for (List<Notificacao> copiasLegadas : gruposLegados.values()) {
            resposta.add(toAvisoResponseLegado(copiasLegadas));
        }

        resposta.sort(Comparator.comparing(
                AvisoNotificacaoResponse::dataCriacao,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return resposta;
    }

    public List<AvisoPublicoResponse> listarAvisosPublicos(StatusNotificacao status) {
        List<AvisoOficial> avisos = status == null
                ? avisoOficialRepository.findAllByOrderByDataCriacaoDesc()
                : avisoOficialRepository.findByStatusOrderByDataCriacaoDesc(status);

        return avisos.stream()
                .map(this::toAvisoPublicoResponse)
                .toList();
    }

    public Notificacao marcarComoLida(Long id) {
        Notificacao notificacao = buscarPorId(id);
        notificacao.setLida(true);
        return notificacaoRepository.save(notificacao);
    }

    @Transactional
    public void excluir(Long id) {
        Notificacao notificacao = buscarPorId(id);
        notificacaoRepository.delete(notificacao);
    }

    @Transactional
    public void excluirAviso(Long avisoId) {
        AvisoOficial aviso = avisoOficialRepository.findById(avisoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso oficial nao encontrado."));

        avisoDescartadoUsuarioRepository.deleteByAvisoOficialId(aviso.getId());
        notificacaoRepository.deleteByAvisoOficialId(aviso.getId());
        if (aviso.getGrupoNotificacao() != null && !aviso.getGrupoNotificacao().isBlank()) {
            notificacaoRepository.deleteByGrupoNotificacao(aviso.getGrupoNotificacao());
        }
        avisoOficialRepository.delete(aviso);
    }

    @Transactional
    public AvisoNotificacaoResponse marcarAvisoComoSuperado(Long notificacaoReferenciaId) {
        Notificacao referencia = notificacaoRepository.findById(notificacaoReferenciaId).orElse(null);

        if (referencia != null) {
            if (referencia.getAvisoOficial() != null) {
                return marcarAvisoOficialComoSuperado(referencia.getAvisoOficial().getId());
            }

            List<Notificacao> notificacoes = buscarCopiasDoMesmoAviso(referencia);
            notificacoes.forEach(notificacao -> notificacao.setStatus(StatusNotificacao.SUPERADA));
            notificacaoRepository.saveAll(notificacoes);

            if (referencia.getGrupoNotificacao() != null && !referencia.getGrupoNotificacao().isBlank()) {
                avisoOficialRepository.findByGrupoNotificacao(referencia.getGrupoNotificacao())
                        .ifPresent(aviso -> {
                            aviso.setStatus(StatusNotificacao.SUPERADA);
                            avisoOficialRepository.save(aviso);
                        });
            }

            return toAvisoResponseLegado(notificacoes);
        }

        return marcarAvisoOficialComoSuperado(notificacaoReferenciaId);
    }

    @Transactional
    public AvisoNotificacaoResponse marcarAvisoOficialComoSuperado(Long avisoId) {
        AvisoOficial aviso = avisoOficialRepository.findById(avisoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso oficial nao encontrado."));

        aviso.setStatus(StatusNotificacao.SUPERADA);
        avisoOficialRepository.save(aviso);

        List<Notificacao> copias = buscarCopiasDoAvisoOficial(aviso);
        copias.forEach(notificacao -> notificacao.setStatus(StatusNotificacao.SUPERADA));
        if (!copias.isEmpty()) {
            notificacaoRepository.saveAll(copias);
        }

        return toAvisoResponse(aviso, copias);
    }

    @Transactional
    public LimpezaHistoricoNotificacoesResponse limparHistoricoDoUsuarioAutenticado(String emailUsuarioAutenticado) {
        User usuario = buscarUsuarioPorEmail(emailUsuarioAutenticado);
        List<Notificacao> historico = notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
                usuario.getId(),
                StatusNotificacao.SUPERADA
        );

        for (Notificacao notificacao : historico) {
            AvisoOficial aviso = notificacao.getAvisoOficial();
            if (aviso == null && notificacao.getGrupoNotificacao() != null) {
                aviso = avisoOficialRepository.findByGrupoNotificacao(notificacao.getGrupoNotificacao()).orElse(null);
            }
            if (aviso != null && !avisoDescartadoUsuarioRepository.existsByUsuarioIdAndAvisoOficialId(usuario.getId(), aviso.getId())) {
                AvisoDescartadoUsuario descarte = new AvisoDescartadoUsuario();
                descarte.setUsuario(usuario);
                descarte.setAvisoOficial(aviso);
                descarte.setDataDescarte(LocalDateTime.now());
                avisoDescartadoUsuarioRepository.save(descarte);
            }
        }

        notificacaoRepository.deleteAll(historico);

        int total = historico.size();
        String mensagem = total == 0
                ? "Nenhuma notificacao antiga foi encontrada no seu historico."
                : "Historico de notificacoes antigas limpo com sucesso.";

        return new LimpezaHistoricoNotificacoesResponse(total, mensagem);
    }

    @Transactional
    public NotificacaoLinhaResponse notificarUsuariosDaLinha(
            Long linhaId,
            String titulo,
            String mensagem,
            Boolean superarNotificacoesAtuaisDaLinha
    ) {
        Linha linha = linhaRepository.findById(linhaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));

        if (Boolean.TRUE.equals(superarNotificacoesAtuaisDaLinha)) {
            superarNotificacoesManuaisAtivasDaLinha(linha.getId());
        }

        AvisoOficial aviso = criarAvisoOficial(linha, null, TipoNotificacao.AVISO, false, titulo, mensagem);
        int total = criarNotificacoesParaFavoritos(aviso, linha);
        String resposta = total == 0
                ? "Aviso da linha publicado. Usuarios que favoritarem esta linha receberao o comunicado enquanto estiver ativo."
                : "Notificacao enviada com sucesso.";

        return new NotificacaoLinhaResponse(linha.getId(), linha.getCodigo(), linha.getNome(), total, resposta);
    }

    @Transactional
    public NotificacaoLinhaResponse notificarTodosUsuarios(String titulo, String mensagem) {
        AvisoOficial aviso = criarAvisoOficial(null, null, TipoNotificacao.AVISO, true, titulo, mensagem);
        List<User> usuarios = userRepository.findByTipoUsuarioOrderByNomeAsc(TipoUsuario.USER);
        int total = criarNotificacoesParaUsuarios(usuarios, aviso);
        String resposta = total == 0
                ? "Aviso geral publicado com sucesso. Novos usuarios receberao este comunicado automaticamente."
                : "Aviso geral enviado com sucesso.";

        return new NotificacaoLinhaResponse(null, null, null, total, resposta);
    }

    @Transactional
    public int notificarOcorrenciaAtiva(Ocorrencia ocorrencia, String titulo, String mensagem) {
        superarNotificacoesAtivasDaOcorrencia(ocorrencia.getId());
        AvisoOficial aviso = criarAvisoOficial(
                ocorrencia.getLinha(),
                ocorrencia,
                TipoNotificacao.OCORRENCIA,
                false,
                titulo,
                mensagem
        );
        return criarNotificacoesParaFavoritos(aviso, ocorrencia.getLinha());
    }

    @Transactional
    public int notificarOcorrenciaEncerrada(Ocorrencia ocorrencia, String titulo, String mensagem) {
        superarNotificacoesAtivasDaOcorrencia(ocorrencia.getId());
        return 0;
    }

    @Transactional
    public void superarNotificacoesAtivasDaOcorrencia(Long ocorrenciaId) {
        if (ocorrenciaId == null) {
            return;
        }

        List<AvisoOficial> avisos = avisoOficialRepository.findByOcorrenciaIdAndStatus(
                ocorrenciaId,
                StatusNotificacao.ATIVA
        );
        if (!avisos.isEmpty()) {
            avisos.forEach(aviso -> aviso.setStatus(StatusNotificacao.SUPERADA));
            avisoOficialRepository.saveAll(avisos);
        }

        List<Notificacao> notificacoes = notificacaoRepository.findByOcorrenciaIdAndStatus(
                ocorrenciaId,
                StatusNotificacao.ATIVA
        );
        if (!notificacoes.isEmpty()) {
            notificacoes.forEach(notificacao -> notificacao.setStatus(StatusNotificacao.SUPERADA));
            notificacaoRepository.saveAll(notificacoes);
        }
    }

    @Transactional
    public void desvincularOcorrencia(Long ocorrenciaId, boolean superarAtivas) {
        if (ocorrenciaId == null) {
            return;
        }

        List<AvisoOficial> avisos = avisoOficialRepository.findByOcorrenciaId(ocorrenciaId);
        if (!avisos.isEmpty()) {
            avisos.forEach(aviso -> {
                if (superarAtivas && aviso.getStatus() == StatusNotificacao.ATIVA) {
                    aviso.setStatus(StatusNotificacao.SUPERADA);
                }
                aviso.setOcorrencia(null);
            });
            avisoOficialRepository.saveAll(avisos);
        }

        List<Notificacao> notificacoes = notificacaoRepository.findByOcorrenciaId(ocorrenciaId);
        if (!notificacoes.isEmpty()) {
            notificacoes.forEach(notificacao -> {
                if (superarAtivas && notificacao.getStatus() == StatusNotificacao.ATIVA) {
                    notificacao.setStatus(StatusNotificacao.SUPERADA);
                }
                notificacao.setOcorrencia(null);
            });
            notificacaoRepository.saveAll(notificacoes);
        }
    }

    @Transactional
    public void migrarAvisosLegadosSeNecessario() {
        List<Notificacao> legadas = notificacaoRepository.findByAvisoOficialIsNull();
        if (legadas.isEmpty()) {
            return;
        }

        Map<String, List<Notificacao>> grupos = new LinkedHashMap<>();
        for (Notificacao notificacao : legadas) {
            grupos.computeIfAbsent(chaveAviso(notificacao), chave -> new ArrayList<>())
                    .add(notificacao);
        }

        for (List<Notificacao> copias : grupos.values()) {
            Notificacao referencia = copias.get(0);
            String grupo = (referencia.getGrupoNotificacao() != null && !referencia.getGrupoNotificacao().isBlank())
                    ? referencia.getGrupoNotificacao()
                    : UUID.randomUUID().toString();

            AvisoOficial aviso = avisoOficialRepository.findByGrupoNotificacao(grupo).orElse(null);
            if (aviso == null) {
                boolean possuiAtiva = copias.stream().anyMatch(item -> item.getStatus() == StatusNotificacao.ATIVA);
                LocalDateTime dataMaisRecente = copias.stream()
                        .map(Notificacao::getDataCriacao)
                        .filter(java.util.Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());

                aviso = new AvisoOficial();
                aviso.setGrupoNotificacao(grupo);
                aviso.setLinha(referencia.getLinha());
                aviso.setOcorrencia(referencia.getOcorrencia());
                aviso.setTipoNotificacao(tipoNotificacao(referencia));
                aviso.setEnviarParaTodos(referencia.getLinha() == null && referencia.getOcorrencia() == null);
                aviso.setTitulo(referencia.getTitulo());
                aviso.setMensagem(referencia.getMensagem());
                aviso.setStatus(possuiAtiva ? StatusNotificacao.ATIVA : StatusNotificacao.SUPERADA);
                aviso.setDataCriacao(dataMaisRecente);
                aviso = avisoOficialRepository.save(aviso);
            }

            AvisoOficial avisoVinculado = aviso;
            Set<Long> usuariosNoGrupo = new HashSet<>();
            List<Notificacao> paraSalvar = new ArrayList<>();
            for (Notificacao copia : copias) {
                if (usuariosNoGrupo.add(copia.getUsuario().getId())) {
                    copia.setAvisoOficial(avisoVinculado);
                    copia.setGrupoNotificacao(avisoVinculado.getGrupoNotificacao());
                    paraSalvar.add(copia);
                }
            }
            if (!paraSalvar.isEmpty()) {
                notificacaoRepository.saveAll(paraSalvar);
            }
        }
    }

    private AvisoOficial criarAvisoOficial(
            Linha linha,
            Ocorrencia ocorrencia,
            TipoNotificacao tipoNotificacao,
            boolean enviarParaTodos,
            String titulo,
            String mensagem
    ) {
        AvisoOficial aviso = new AvisoOficial();
        aviso.setGrupoNotificacao(UUID.randomUUID().toString());
        aviso.setLinha(linha);
        aviso.setOcorrencia(ocorrencia);
        aviso.setTipoNotificacao(tipoNotificacao);
        aviso.setEnviarParaTodos(enviarParaTodos);
        aviso.setTitulo(titulo == null ? null : titulo.trim());
        aviso.setMensagem(mensagem == null ? null : mensagem.trim());
        aviso.setStatus(StatusNotificacao.ATIVA);
        aviso.setDataCriacao(LocalDateTime.now());
        return avisoOficialRepository.save(aviso);
    }

    private void prepararNotificacao(Notificacao notificacao) {
        if (notificacao.getTitulo() != null) {
            notificacao.setTitulo(notificacao.getTitulo().trim());
        }

        if (notificacao.getMensagem() != null) {
            notificacao.setMensagem(notificacao.getMensagem().trim());
        }

        if (notificacao.getLida() == null) {
            notificacao.setLida(false);
        }

        if (notificacao.getStatus() == null) {
            notificacao.setStatus(StatusNotificacao.ATIVA);
        }

        if (notificacao.getTipoNotificacao() == null) {
            notificacao.setTipoNotificacao(
                    notificacao.getOcorrencia() == null ? TipoNotificacao.AVISO : TipoNotificacao.OCORRENCIA
            );
        }

        if (notificacao.getDataCriacao() == null) {
            notificacao.setDataCriacao(LocalDateTime.now());
        }

        User usuario = buscarUsuarioDaNotificacao(notificacao);
        notificacao.setUsuario(usuario);

        if (notificacao.getLinha() != null && notificacao.getLinha().getId() != null) {
            Linha linha = linhaRepository.findById(notificacao.getLinha().getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));
            notificacao.setLinha(linha);
        }
    }

    private int criarNotificacoesParaFavoritos(AvisoOficial aviso, Linha linha) {
        Map<Long, User> usuarios = new LinkedHashMap<>();
        usuarioLinhaFavoritaRepository.findByLinhaId(linha.getId())
                .forEach(favorito -> usuarios.putIfAbsent(favorito.getUsuario().getId(), favorito.getUsuario()));

        return criarNotificacoesParaUsuarios(usuarios.values().stream().toList(), aviso);
    }

    private int criarNotificacoesParaUsuarios(List<User> usuarios, AvisoOficial aviso) {
        Map<Long, User> usuariosUnicos = new LinkedHashMap<>();
        usuarios.forEach(usuario -> usuariosUnicos.putIfAbsent(usuario.getId(), usuario));

        List<Notificacao> novas = new ArrayList<>();
        for (User usuario : usuariosUnicos.values()) {
            if (notificacaoRepository.existsByUsuarioIdAndAvisoOficialId(usuario.getId(), aviso.getId())) {
                continue;
            }
            Notificacao notificacao = new Notificacao();
            notificacao.setUsuario(usuario);
            notificacao.setAvisoOficial(aviso);
            notificacao.setLinha(aviso.getLinha());
            notificacao.setOcorrencia(aviso.getOcorrencia());
            notificacao.setGrupoNotificacao(aviso.getGrupoNotificacao());
            notificacao.setTipoNotificacao(aviso.getTipoNotificacao());
            notificacao.setTitulo(aviso.getTitulo());
            notificacao.setMensagem(aviso.getMensagem());
            notificacao.setLida(false);
            notificacao.setStatus(aviso.getStatus());
            notificacao.setDataCriacao(aviso.getDataCriacao());
            novas.add(notificacao);
        }

        if (!novas.isEmpty()) {
            notificacaoRepository.saveAll(novas);
        }

        return novas.size();
    }

    private List<Notificacao> buscarCopiasDoAvisoOficial(AvisoOficial aviso) {
        Map<Long, Notificacao> unicas = new LinkedHashMap<>();
        notificacaoRepository.findByAvisoOficialId(aviso.getId())
                .forEach(item -> unicas.put(item.getId(), item));
        if (aviso.getGrupoNotificacao() != null && !aviso.getGrupoNotificacao().isBlank()) {
            notificacaoRepository.findByGrupoNotificacao(aviso.getGrupoNotificacao())
                    .forEach(item -> unicas.putIfAbsent(item.getId(), item));
        }
        return new ArrayList<>(unicas.values());
    }

    private List<Notificacao> buscarCopiasDoMesmoAviso(Notificacao referencia) {
        if (referencia.getGrupoNotificacao() != null && !referencia.getGrupoNotificacao().isBlank()) {
            return notificacaoRepository.findByGrupoNotificacao(referencia.getGrupoNotificacao());
        }

        if (referencia.getOcorrencia() != null && referencia.getOcorrencia().getId() != null) {
            return notificacaoRepository.findByOcorrenciaId(referencia.getOcorrencia().getId());
        }

        if (referencia.getLinha() != null && referencia.getLinha().getId() != null) {
            return notificacaoRepository.findByLinhaIdAndTituloAndMensagemAndOcorrenciaIsNull(
                    referencia.getLinha().getId(),
                    referencia.getTitulo(),
                    referencia.getMensagem()
            );
        }

        return List.of(referencia);
    }

    private String chaveAviso(Notificacao notificacao) {
        if (notificacao.getGrupoNotificacao() != null && !notificacao.getGrupoNotificacao().isBlank()) {
            return "grupo:" + notificacao.getGrupoNotificacao();
        }

        if (notificacao.getOcorrencia() != null && notificacao.getOcorrencia().getId() != null) {
            return "ocorrencia:" + notificacao.getOcorrencia().getId();
        }

        Long linhaId = notificacao.getLinha() == null ? null : notificacao.getLinha().getId();
        return "manual:" + linhaId + "|" + notificacao.getTitulo() + "|" + notificacao.getMensagem();
    }

    private AvisoNotificacaoResponse toAvisoResponse(AvisoOficial aviso, List<Notificacao> copias) {
        Linha linha = aviso.getLinha();
        Ocorrencia ocorrencia = aviso.getOcorrencia();
        Long referenciaNotificacaoId = copias.isEmpty() ? null : copias.get(0).getId();
        int usuariosAlcancados = (int) copias.stream()
                .map(notificacao -> notificacao.getUsuario().getId())
                .distinct()
                .count();

        return new AvisoNotificacaoResponse(
                aviso.getId(),
                referenciaNotificacaoId,
                linha == null ? null : linha.getId(),
                linha == null ? null : linha.getCodigo(),
                linha == null ? null : linha.getNome(),
                ocorrencia == null ? null : ocorrencia.getId(),
                aviso.getTipoNotificacao(),
                ocorrencia == null ? null : ocorrencia.getTipo(),
                ocorrencia == null ? null : ocorrencia.getPrioridade(),
                aviso.getTitulo(),
                aviso.getMensagem(),
                aviso.getStatus(),
                usuariosAlcancados,
                aviso.getDataCriacao()
        );
    }

    private AvisoNotificacaoResponse toAvisoResponseLegado(List<Notificacao> notificacoes) {
        Notificacao referencia = notificacoes.get(0);
        Linha linha = referencia.getLinha();
        boolean possuiAtiva = notificacoes.stream()
                .anyMatch(notificacao -> notificacao.getStatus() == StatusNotificacao.ATIVA);
        int usuariosAlcancados = (int) notificacoes.stream()
                .map(notificacao -> notificacao.getUsuario().getId())
                .distinct()
                .count();

        return new AvisoNotificacaoResponse(
                null,
                referencia.getId(),
                linha == null ? null : linha.getId(),
                linha == null ? null : linha.getCodigo(),
                linha == null ? null : linha.getNome(),
                referencia.getOcorrencia() == null ? null : referencia.getOcorrencia().getId(),
                tipoNotificacao(referencia),
                referencia.getOcorrencia() == null ? null : referencia.getOcorrencia().getTipo(),
                referencia.getOcorrencia() == null ? null : referencia.getOcorrencia().getPrioridade(),
                referencia.getTitulo(),
                referencia.getMensagem(),
                possuiAtiva ? StatusNotificacao.ATIVA : StatusNotificacao.SUPERADA,
                usuariosAlcancados,
                referencia.getDataCriacao()
        );
    }

    private AvisoPublicoResponse toAvisoPublicoResponse(AvisoOficial aviso) {
        Linha linha = aviso.getLinha();
        Ocorrencia ocorrencia = aviso.getOcorrencia();
        return new AvisoPublicoResponse(
                aviso.getId(),
                linha == null ? null : linha.getId(),
                linha == null ? null : linha.getCodigo(),
                linha == null ? null : linha.getNome(),
                ocorrencia == null ? null : ocorrencia.getId(),
                aviso.getTipoNotificacao(),
                ocorrencia == null ? null : ocorrencia.getTipo(),
                ocorrencia == null ? null : ocorrencia.getPrioridade(),
                aviso.getEnviarParaTodos(),
                aviso.getTitulo(),
                aviso.getMensagem(),
                aviso.getStatus(),
                aviso.getDataCriacao()
        );
    }

    private void superarNotificacoesManuaisAtivasDaLinha(Long linhaId) {
        List<AvisoOficial> avisos = avisoOficialRepository.findByLinhaIdAndStatusAndOcorrenciaIsNull(
                linhaId,
                StatusNotificacao.ATIVA
        );
        if (!avisos.isEmpty()) {
            avisos.forEach(aviso -> aviso.setStatus(StatusNotificacao.SUPERADA));
            avisoOficialRepository.saveAll(avisos);
        }

        List<Notificacao> notificacoes = notificacaoRepository.findByLinhaIdAndStatusAndOcorrenciaIsNull(
                linhaId,
                StatusNotificacao.ATIVA
        );
        if (!notificacoes.isEmpty()) {
            notificacoes.forEach(notificacao -> notificacao.setStatus(StatusNotificacao.SUPERADA));
            notificacaoRepository.saveAll(notificacoes);
        }
    }

    private User buscarUsuarioDaNotificacao(Notificacao notificacao) {
        if (notificacao.getUsuario() == null || notificacao.getUsuario().getId() == null) {
            throw new IllegalArgumentException("Usuario deve ser informado.");
        }

        return userRepository.findById(notificacao.getUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado."));
    }

    private User buscarUsuarioPorEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Usuario autenticado deve ser informado.");
        }

        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado."));
    }

    private void validarUsuarioExistente(Long usuarioId) {
        if (!userRepository.existsById(usuarioId)) {
            throw new RecursoNaoEncontradoException("Usuario nao encontrado.");
        }
    }

    private TipoNotificacao tipoNotificacao(Notificacao notificacao) {
        if (notificacao.getTipoNotificacao() != null) {
            return notificacao.getTipoNotificacao();
        }

        return notificacao.getOcorrencia() == null ? TipoNotificacao.AVISO : TipoNotificacao.OCORRENCIA;
    }
}
