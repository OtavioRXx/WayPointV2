package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.HorarioParada;
import com.waypoint.project_tcc_app.model.Linha;
import com.waypoint.project_tcc_app.model.Notificacao;
import com.waypoint.project_tcc_app.model.Ocorrencia;
import com.waypoint.project_tcc_app.model.Parada;
import com.waypoint.project_tcc_app.model.ReporteUsuario;
import com.waypoint.project_tcc_app.model.TipoNotificacao;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.model.UsuarioLinhaFavorita;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static Linha toLinha(LinhaRequest request) {
        Linha linha = new Linha();
        linha.setCodigo(request.codigo());
        linha.setNome(request.nome());
        linha.setStatus(request.status());
        return linha;
    }

    public static LinhaResponse toLinhaResponse(Linha linha) {
        return new LinhaResponse(
                linha.getId(),
                linha.getCodigo(),
                linha.getNome(),
                linha.getStatus()
        );
    }

    public static Parada toParada(ParadaRequest request) {
        Parada parada = new Parada();
        parada.setLinha(linhaReference(request.linhaId()));
        parada.setNome(request.nome());
        parada.setLatitude(request.latitude());
        parada.setLongitude(request.longitude());
        parada.setOrdem(request.ordem());
        parada.setSentido(request.sentido());
        return parada;
    }

    public static ParadaResponse toParadaResponse(Parada parada) {
        Linha linha = parada.getLinha();
        return new ParadaResponse(
                parada.getId(),
                linha.getId(),
                linha.getCodigo(),
                linha.getNome(),
                parada.getNome(),
                parada.getLatitude(),
                parada.getLongitude(),
                parada.getOrdem(),
                parada.getSentido()
        );
    }

    public static Ocorrencia toOcorrencia(OcorrenciaRequest request) {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setLinha(linhaReference(request.linhaId()));
        ocorrencia.setTipo(request.tipo());
        ocorrencia.setDescricao(request.descricao());
        ocorrencia.setPrioridade(request.prioridade());
        ocorrencia.setAtiva(request.ativa());
        ocorrencia.setDataHora(request.dataHora());
        return ocorrencia;
    }

    public static OcorrenciaResponse toOcorrenciaResponse(Ocorrencia ocorrencia) {
        Linha linha = ocorrencia.getLinha();
        return new OcorrenciaResponse(
                ocorrencia.getId(),
                linha == null ? null : linha.getId(),
                linha == null ? null : linha.getCodigo(),
                linha == null ? null : linha.getNome(),
                ocorrencia.getTipo(),
                ocorrencia.getDescricao(),
                ocorrencia.getPrioridade(),
                ocorrencia.getAtiva(),
                ocorrencia.getDataHora(),
                ocorrencia.getDataEncerramento()
        );
    }

    public static ReporteUsuario toReporteUsuario(ReporteUsuarioRequest request) {
        ReporteUsuario reporteUsuario = new ReporteUsuario();
        reporteUsuario.setUsuario(userReference(request.usuarioId()));
        reporteUsuario.setLinha(linhaReference(request.linhaId()));
        if (request.paradaId() != null) {
            reporteUsuario.setParada(paradaReference(request.paradaId()));
        }
        reporteUsuario.setTipoReporte(request.tipoReporte());
        reporteUsuario.setDataHora(request.dataHora());
        return reporteUsuario;
    }

    public static ReporteUsuarioResponse toReporteUsuarioResponse(ReporteUsuario reporteUsuario) {
        User usuario = reporteUsuario.getUsuario();
        Linha linha = reporteUsuario.getLinha();
        Parada parada = reporteUsuario.getParada();
        return new ReporteUsuarioResponse(
                reporteUsuario.getId(),
                usuario.getId(),
                usuario.getNome(),
                linha.getId(),
                linha.getCodigo(),
                linha.getNome(),
                parada == null ? null : parada.getId(),
                parada == null ? null : parada.getNome(),
                parada == null ? null : parada.getSentido().name(),
                reporteUsuario.getTipoReporte(),
                reporteUsuario.getDataHora()
        );
    }

    public static HorarioParada toHorarioParada(HorarioParadaRequest request, Long paradaId) {
        HorarioParada horarioParada = new HorarioParada();
        horarioParada.setParada(paradaReference(paradaId));
        horarioParada.setHorarioPrevisto(request.horarioPrevisto());
        return horarioParada;
    }

    public static HorarioParadaResponse toHorarioParadaResponse(HorarioParada horarioParada) {
        Parada parada = horarioParada.getParada();
        return new HorarioParadaResponse(
                horarioParada.getId(),
                parada.getId(),
                parada.getNome(),
                horarioParada.getHorarioPrevisto()
        );
    }

    public static UsuarioLinhaFavoritaResponse toUsuarioLinhaFavoritaResponse(UsuarioLinhaFavorita favorito) {
        User usuario = favorito.getUsuario();
        Linha linha = favorito.getLinha();
        return new UsuarioLinhaFavoritaResponse(
                usuario.getId(),
                usuario.getNome(),
                linha.getId(),
                linha.getCodigo(),
                linha.getNome()
        );
    }

    public static Notificacao toNotificacao(NotificacaoRequest request) {
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(userReference(request.usuarioId()));
        if (request.linhaId() != null) {
            notificacao.setLinha(linhaReference(request.linhaId()));
        }
        notificacao.setTitulo(request.titulo());
        notificacao.setMensagem(request.mensagem());
        notificacao.setLida(request.lida());
        notificacao.setDataCriacao(request.dataCriacao());
        return notificacao;
    }

    public static NotificacaoResponse toNotificacaoResponse(Notificacao notificacao) {
        User usuario = notificacao.getUsuario();
        Linha linha = notificacao.getLinha();
        return new NotificacaoResponse(
                notificacao.getId(),
                usuario.getId(),
                usuario.getNome(),
                linha == null ? null : linha.getId(),
                linha == null ? null : linha.getCodigo(),
                linha == null ? null : linha.getNome(),
                notificacao.getOcorrencia() == null ? null : notificacao.getOcorrencia().getId(),
                tipoNotificacao(notificacao),
                notificacao.getOcorrencia() == null ? null : notificacao.getOcorrencia().getTipo(),
                notificacao.getOcorrencia() == null ? null : notificacao.getOcorrencia().getPrioridade(),
                notificacao.getTitulo(),
                notificacao.getMensagem(),
                notificacao.getLida(),
                notificacao.getStatus(),
                notificacao.getDataCriacao()
        );
    }

    public static UsuarioResumoResponse toUsuarioResumoResponse(User usuario) {
        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTipoUsuario()
        );
    }

    private static Linha linhaReference(Long id) {
        Linha linha = new Linha();
        linha.setId(id);
        return linha;
    }

    private static Parada paradaReference(Long id) {
        Parada parada = new Parada();
        parada.setId(id);
        return parada;
    }

    private static User userReference(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static TipoNotificacao tipoNotificacao(Notificacao notificacao) {
        if (notificacao.getTipoNotificacao() != null) {
            return notificacao.getTipoNotificacao();
        }

        return notificacao.getOcorrencia() == null ? TipoNotificacao.AVISO : TipoNotificacao.OCORRENCIA;
    }
}
