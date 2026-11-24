package com.waypoint.project_tcc_app.controller;

import com.jayway.jsonpath.JsonPath;
import com.waypoint.project_tcc_app.model.Linha;
import com.waypoint.project_tcc_app.model.Notificacao;
import com.waypoint.project_tcc_app.model.Ocorrencia;
import com.waypoint.project_tcc_app.model.Parada;
import com.waypoint.project_tcc_app.model.Prioridade;
import com.waypoint.project_tcc_app.model.ReporteUsuario;
import com.waypoint.project_tcc_app.model.Sentido;
import com.waypoint.project_tcc_app.model.StatusLinha;
import com.waypoint.project_tcc_app.model.StatusNotificacao;
import com.waypoint.project_tcc_app.model.TipoNotificacao;
import com.waypoint.project_tcc_app.model.TipoReporte;
import com.waypoint.project_tcc_app.model.TipoUsuario;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.model.ConfirmacaoReporte;
import com.waypoint.project_tcc_app.repository.AvisoOficialRepository;
import com.waypoint.project_tcc_app.repository.ConfirmacaoReporteRepository;
import com.waypoint.project_tcc_app.repository.LinhaRepository;
import com.waypoint.project_tcc_app.repository.NotificacaoRepository;
import com.waypoint.project_tcc_app.repository.OcorrenciaRepository;
import com.waypoint.project_tcc_app.repository.ParadaRepository;
import com.waypoint.project_tcc_app.repository.ReporteUsuarioRepository;
import com.waypoint.project_tcc_app.repository.UserRepository;
import com.waypoint.project_tcc_app.service.DadosTemporariosCleanupService;
import com.waypoint.project_tcc_app.service.NotificacaoService;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiFluxosControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LinhaRepository linhaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private OcorrenciaRepository ocorrenciaRepository;

    @Autowired
    private ParadaRepository paradaRepository;

    @Autowired
    private ReporteUsuarioRepository reporteUsuarioRepository;

    @Autowired
    private ConfirmacaoReporteRepository confirmacaoReporteRepository;

    @Autowired
    private DadosTemporariosCleanupService dadosTemporariosCleanupService;

    @Autowired
    private AvisoOficialRepository avisoOficialRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void requisicaoModificadoraSemCsrfFicaBloqueada() throws Exception {
        String novaLinha = """
                {
                  "codigo": "TST-CSRF",
                  "nome": "Linha Sem Csrf",
                  "status": "NORMAL"
                }
                """;

        mockMvc.perform(post("/api/linhas")
                        .with(user("admin@teste.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novaLinha))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioPodeListarLinhasEParadas() throws Exception {
        Linha linha = criarLinha("TST-101", "Linha Teste Usuario");
        criarParada(linha, "Terminal Teste", 1, Sentido.IDA);

        mockMvc.perform(get("/api/linhas")
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/linhas/{linhaId}/paradas", linha.getId())
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].linhaId").value(linha.getId()))
                .andExpect(jsonPath("$[0].nome").value("Terminal Teste"));
    }

    @Test
    void paradasPorLinhaESentidoSaoRetornadasNaOrdemCadastrada() throws Exception {
        Linha linha = criarLinha("TST-102", "Linha Ordem Paradas");
        criarParada(linha, "Segunda IDA", 2, Sentido.IDA);
        criarParada(linha, "Primeira IDA", 1, Sentido.IDA);
        criarParada(linha, "Primeira VOLTA", 1, Sentido.VOLTA);
        criarParada(linha, "Segunda VOLTA", 2, Sentido.VOLTA);

        mockMvc.perform(get("/api/linhas/{linhaId}/paradas", linha.getId())
                        .param("sentido", "IDA")
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Primeira IDA"))
                .andExpect(jsonPath("$[0].ordem").value(1))
                .andExpect(jsonPath("$[1].nome").value("Segunda IDA"))
                .andExpect(jsonPath("$[1].ordem").value(2));

        mockMvc.perform(get("/api/linhas/{linhaId}/paradas", linha.getId())
                        .param("sentido", "VOLTA")
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sentido").value("VOLTA"))
                .andExpect(jsonPath("$[1].sentido").value("VOLTA"));
    }

    @Test
    void horariosDaParadaSaoRetornadosOrdenados() throws Exception {
        Linha linha = criarLinha("TST-103", "Linha Horarios");
        Parada parada = criarParada(linha, "Terminal Horario", 1, Sentido.IDA);

        cadastrarHorario(parada, "08:30:00");
        cadastrarHorario(parada, "07:00:00");
        cadastrarHorario(parada, "08:00:00");

        mockMvc.perform(get("/api/paradas/{paradaId}/horarios", parada.getId())
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].horarioPrevisto").value("07:00:00"))
                .andExpect(jsonPath("$[1].horarioPrevisto").value("08:00:00"))
                .andExpect(jsonPath("$[2].horarioPrevisto").value("08:30:00"));
    }

    @Test
    void usuarioNaoPodeCadastrarLinha() throws Exception {
        String novaLinha = """
                {
                  "codigo": "TST-403",
                  "nome": "Linha Bloqueada",
                  "status": "NORMAL"
                }
                """;

        mockMvc.perform(post("/api/linhas")
                        .with(user("usuario@teste.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novaLinha))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPodeCadastrarLinha() throws Exception {
        String novaLinha = """
                {
                  "codigo": "TST-201",
                  "nome": "Linha Criada Pelo Teste",
                  "status": "NORMAL"
                }
                """;

        mockMvc.perform(post("/api/linhas")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novaLinha))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("TST-201"))
                .andExpect(jsonPath("$.nome").value("Linha Criada Pelo Teste"))
                .andExpect(jsonPath("$.status").value("NORMAL"));
    }

    @Test
    void usuarioPodeReportarEFavoritarLinhaSemExporSenha() throws Exception {
        Linha linha = criarLinha("TST-202", "Linha Reporte Teste");
        User usuario = criarUsuario("reportador@teste.com", "12345678901");

        String reporte = """
                {
                  "usuarioId": %d,
                  "linhaId": %d,
                  "tipoReporte": "LOTADO"
                }
                """.formatted(usuario.getId(), linha.getId());

        mockMvc.perform(post("/api/reportes")
                        .with(user("reportador@teste.com").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.usuarioNome").value(usuario.getNome()))
                .andExpect(jsonPath("$.linhaId").value(linha.getId()))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.usuario.senha").doesNotExist());

        mockMvc.perform(post("/api/usuarios/{usuarioId}/linhas-favoritas/{linhaId}", usuario.getId(), linha.getId())
                        .with(user("reportador@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.linhaId").value(linha.getId()))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.usuario.senha").doesNotExist());
    }

    @Test
    void endpointLegadoDeReporteUsaUsuarioAutenticadoEIgnoraUsuarioIdEnviado() throws Exception {
        Linha linha = criarLinha("TST-239", "Linha Reporte Usuario Autenticado");
        User usuarioAutenticado = criarUsuario("reportador.autenticado@teste.com", "52345678913");
        User outroUsuario = criarUsuario("reportador.outro@teste.com", "52345678914");

        String reporte = """
                {
                  "usuarioId": %d,
                  "linhaId": %d,
                  "tipoReporte": "LOTADO"
                }
                """.formatted(outroUsuario.getId(), linha.getId());

        mockMvc.perform(post("/api/reportes")
                        .with(user(usuarioAutenticado.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuarioAutenticado.getId()))
                .andExpect(jsonPath("$.linhaId").value(linha.getId()));

        assertThat(reporteUsuarioRepository.findByUsuarioIdOrderByDataHoraDesc(outroUsuario.getId())).isEmpty();
    }

    @Test
    void endpointLegadoDeReporteTambemRespeitaCooldown() throws Exception {
        Linha linha = criarLinha("TST-240", "Linha Cooldown Legado");
        User usuario = criarUsuario("cooldown.legado@teste.com", "52345678915");

        String primeiroReporte = """
                {
                  "usuarioId": %d,
                  "linhaId": %d,
                  "tipoReporte": "LOTADO"
                }
                """.formatted(usuario.getId(), linha.getId());

        String segundoReporteComDataAntiga = """
                {
                  "usuarioId": %d,
                  "linhaId": %d,
                  "tipoReporte": "ATRASADO",
                  "dataHora": "2000-01-01T10:00:00"
                }
                """.formatted(usuario.getId(), linha.getId());

        mockMvc.perform(post("/api/reportes")
                        .with(user(usuario.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(primeiroReporte))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reportes")
                        .with(user(usuario.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(segundoReporteComDataAntiga))
                .andExpect(status().isTooManyRequests());

        assertThat(reporteUsuarioRepository.findByUsuarioIdOrderByDataHoraDesc(usuario.getId())).hasSize(1);
    }

    @Test
    void usuarioCriaReporteAPartirDeParadaComLinhaInferida() throws Exception {
        Linha linha = criarLinha("TST-226", "Linha Reporte Parada");
        Parada parada = criarParada(linha, "Parada Reporte", 1, Sentido.IDA);
        User usuario = criarUsuario("reportador.parada@teste.com", "12345678939");

        String reporte = """
                {
                  "paradaId": %d,
                  "tipoReporte": "PASSOU"
                }
                """.formatted(parada.getId());

        mockMvc.perform(post("/api/reportes/minhas/paradas")
                        .with(user(usuario.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.linhaId").value(linha.getId()))
                .andExpect(jsonPath("$.paradaId").value(parada.getId()))
                .andExpect(jsonPath("$.paradaNome").value(parada.getNome()))
                .andExpect(jsonPath("$.tipoReporte").value("PASSOU"))
                .andExpect(jsonPath("$.dataHora").exists());

        ReporteUsuario reporteSalvo = reporteUsuarioRepository.findByUsuarioIdOrderByDataHoraDesc(usuario.getId()).get(0);
        assertThat(reporteUsuarioRepository.findByUsuarioIdOrderByDataHoraDesc(usuario.getId())).hasSize(1);
        assertThat(reporteSalvo.getLinha().getId()).isEqualTo(linha.getId());
        assertThat(reporteSalvo.getParada().getId()).isEqualTo(parada.getId());
        assertThat(reporteSalvo.getDataHora()).isNotNull();
    }

    @Test
    void reportePorParadaInexistenteRetornaNaoEncontrado() throws Exception {
        User usuario = criarUsuario("reportador.parada.inexistente@teste.com", "12345678943");

        String reporte = """
                {
                  "paradaId": 999999,
                  "tipoReporte": "PASSOU"
                }
                """;

        mockMvc.perform(post("/api/reportes/minhas/paradas")
                        .with(user(usuario.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().isNotFound());
    }

    @Test
    void reportePorParadaComTipoInvalidoRetornaBadRequest() throws Exception {
        Linha linha = criarLinha("TST-230", "Linha Tipo Reporte Invalido");
        Parada parada = criarParada(linha, "Parada Tipo Invalido", 1, Sentido.IDA);
        User usuario = criarUsuario("reportador.tipo.invalido@teste.com", "12345678944");

        String reporte = """
                {
                  "paradaId": %d,
                  "tipoReporte": "QUEBRADO"
                }
                """.formatted(parada.getId());

        mockMvc.perform(post("/api/reportes/minhas/paradas")
                        .with(user(usuario.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportePorParadaSemUsuarioAutenticadoFicaBloqueado() throws Exception {
        Linha linha = criarLinha("TST-231", "Linha Reporte Sem Login");
        Parada parada = criarParada(linha, "Parada Sem Login", 1, Sentido.IDA);

        String reporte = """
                {
                  "paradaId": %d,
                  "tipoReporte": "PASSOU"
                }
                """.formatted(parada.getId());

        mockMvc.perform(post("/api/reportes/minhas/paradas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void reporteNaoAceitaParadaDeOutraLinhaQuandoLinhaTambemFoiInformada() throws Exception {
        Linha linhaA = criarLinha("TST-227", "Linha A Parada Invalida");
        Linha linhaB = criarLinha("TST-228", "Linha B Parada Invalida");
        Parada paradaDaLinhaB = criarParada(linhaB, "Parada Outra Linha", 1, Sentido.IDA);
        User usuario = criarUsuario("reportador.invalido@teste.com", "12345678940");

        String reporte = """
                {
                  "usuarioId": %d,
                  "linhaId": %d,
                  "paradaId": %d,
                  "tipoReporte": "ATRASADO"
                }
                """.formatted(usuario.getId(), linhaA.getId(), paradaDaLinhaB.getId());

        mockMvc.perform(post("/api/reportes")
                        .with(user(usuario.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cooldownBloqueiaMesmoUsuarioNaMesmaParadaAntesDeQuinzeMinutos() throws Exception {
        Linha linha = criarLinha("TST-232", "Linha Cooldown Bloqueado");
        Parada parada = criarParada(linha, "Parada Cooldown", 1, Sentido.IDA);
        User usuario = criarUsuario("cooldown.bloqueado@teste.com", "42345678901");

        reportarParada(parada, usuario, "PASSOU")
                .andExpect(status().isCreated());

        reportarParada(parada, usuario, "ATRASADO")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("Aguarde aproximadamente")));
    }

    @Test
    void cooldownPermiteMesmoUsuarioNaMesmaParadaAposQuinzeMinutos() throws Exception {
        Linha linha = criarLinha("TST-233", "Linha Cooldown Liberado");
        Parada parada = criarParada(linha, "Parada Liberada", 1, Sentido.IDA);
        User usuario = criarUsuario("cooldown.liberado@teste.com", "42345678902");

        criarReporteEntidade(usuario, linha, parada, TipoReporte.PASSOU, LocalDateTime.now().minusMinutes(16));

        reportarParada(parada, usuario, "ATRASADO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paradaId").value(parada.getId()))
                .andExpect(jsonPath("$.linhaId").value(linha.getId()));

        assertThat(reporteUsuarioRepository.findByUsuarioIdOrderByDataHoraDesc(usuario.getId())).hasSize(2);
    }

    @Test
    void cooldownNaoBloqueiaOutraParadaDoMesmoUsuario() throws Exception {
        Linha linha = criarLinha("TST-234", "Linha Cooldown Outra Parada");
        Parada paradaA = criarParada(linha, "Parada A", 1, Sentido.IDA);
        Parada paradaB = criarParada(linha, "Parada B", 2, Sentido.IDA);
        User usuario = criarUsuario("cooldown.outra.parada@teste.com", "42345678903");

        reportarParada(paradaA, usuario, "PASSOU")
                .andExpect(status().isCreated());

        reportarParada(paradaB, usuario, "NAO_PASSOU")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paradaId").value(paradaB.getId()));
    }

    @Test
    void cooldownNaoBloqueiaOutroUsuarioNaMesmaParada() throws Exception {
        Linha linha = criarLinha("TST-235", "Linha Cooldown Outro Usuario");
        Parada parada = criarParada(linha, "Parada Compartilhada", 1, Sentido.IDA);
        User usuarioA = criarUsuario("cooldown.usuario.a@teste.com", "42345678904");
        User usuarioB = criarUsuario("cooldown.usuario.b@teste.com", "42345678905");

        reportarParada(parada, usuarioA, "PASSOU")
                .andExpect(status().isCreated());

        reportarParada(parada, usuarioB, "PASSOU")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuarioB.getId()));
    }

    @Test
    void ocorrenciaAtivaGeraNotificacaoParaUsuarioComLinhaFavorita() throws Exception {
        Linha linha = criarLinha("TST-203", "Linha Notificacao Teste");
        User usuario = criarUsuario("favorito@teste.com", "12345678902");
        User usuarioSemFavorito = criarUsuario("sem.favorito.ocorrencia@teste.com", "52345678902");

        mockMvc.perform(post("/api/usuarios/{usuarioId}/linhas-favoritas/{linhaId}", usuario.getId(), linha.getId())
                        .with(user("favorito@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isCreated());

        String ocorrencia = """
                {
                  "linhaId": %d,
                  "tipo": "ATRASO",
                  "descricao": "Veiculo com atraso no trajeto",
                  "prioridade": "MEDIA",
                  "ativa": true
                }
                """.formatted(linha.getId());

        mockMvc.perform(post("/api/ocorrencias")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ocorrencia))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linhaId").value(linha.getId()))
                .andExpect(jsonPath("$.ativa").value(true));

        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", usuario.getId())
                        .with(user("favorito@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$[0].linhaId").value(linha.getId()))
                .andExpect(jsonPath("$[0].tipoNotificacao").value("OCORRENCIA"))
                .andExpect(jsonPath("$[0].tipoOcorrencia").value("ATRASO"))
                .andExpect(jsonPath("$[0].prioridadeOcorrencia").value("MEDIA"))
                .andExpect(jsonPath("$[0].titulo", not(nullValue())))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].usuario.senha").doesNotExist());

        assertThat(notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioSemFavorito.getId())).isEmpty();
    }

    @Test
    void usuarioNaoPodeAcessarDadosDeOutroUsuario() throws Exception {
        Linha linha = criarLinha("TST-204", "Linha Acesso Teste");
        User usuarioLogado = criarUsuario("usuario.logado@teste.com", "12345678903");
        User outroUsuario = criarUsuario("outro.usuario@teste.com", "12345678904");

        String reporteDeOutroUsuario = """
                {
                  "usuarioId": %d,
                  "linhaId": %d,
                  "tipoReporte": "ATRASADO"
                }
                """.formatted(outroUsuario.getId(), linha.getId());

        mockMvc.perform(get("/api/usuarios/{usuarioId}/linhas-favoritas", outroUsuario.getId())
                        .with(user(usuarioLogado.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/usuarios/{usuarioId}/reportes", outroUsuario.getId())
                        .with(user(usuarioLogado.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", outroUsuario.getId())
                        .with(user(usuarioLogado.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reportes")
                        .with(user(usuarioLogado.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporteDeOutroUsuario))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuarioLogado.getId()));

        assertThat(reporteUsuarioRepository.findByUsuarioIdOrderByDataHoraDesc(outroUsuario.getId())).isEmpty();
    }

    @Test
    void adminPodeAcessarDadosDeQualquerUsuario() throws Exception {
        User usuario = criarUsuario("usuario.admin.consulta@teste.com", "12345678905");

        mockMvc.perform(get("/api/usuarios/{usuarioId}/linhas-favoritas", usuario.getId())
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioNaoPodeMarcarNotificacaoDeOutroUsuarioComoLida() throws Exception {
        User usuarioLogado = criarUsuario("usuario.notificacao@teste.com", "12345678906");
        User outroUsuario = criarUsuario("dono.notificacao@teste.com", "12345678907");
        Notificacao notificacao = criarNotificacao(outroUsuario);

        mockMvc.perform(patch("/api/notificacoes/{id}/lida", notificacao.getId())
                        .with(user(usuarioLogado.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());

        Notificacao notificacaoAtualizada = notificacaoRepository.findById(notificacao.getId()).orElseThrow();
        assertThat(notificacaoAtualizada.getLida()).isFalse();
    }

    @Test
    void adminEnviaNotificacaoParaTodosUsuariosQueFavoritaramLinha() throws Exception {
        Linha linha = criarLinha("TST-207", "Linha Favoritos Notificados");
        User usuarioA = criarUsuario("favorito.a@teste.com", "12345678909");
        User usuarioB = criarUsuario("favorito.b@teste.com", "12345678910");
        User usuarioC = criarUsuario("favorito.c@teste.com", "12345678911");

        favoritar(usuarioA, linha);
        favoritar(usuarioB, linha);
        favoritar(usuarioC, linha);

        enviarNotificacaoDaLinha(linha, "Aviso operacional", "Linha com operacao especial", false)
                .andExpect(jsonPath("$.usuariosNotificados").value(3));

        List<Notificacao> notificacoes = notificacaoRepository.findByLinhaIdAndStatusAndOcorrenciaIsNull(
                linha.getId(),
                StatusNotificacao.ATIVA
        );

        assertThat(notificacoes).hasSize(3);
        assertThat(notificacoes).extracting(notificacao -> notificacao.getUsuario().getId())
                .containsExactlyInAnyOrder(usuarioA.getId(), usuarioB.getId(), usuarioC.getId());
        assertThat(notificacoes).extracting(Notificacao::getTipoNotificacao)
                .containsOnly(TipoNotificacao.AVISO);
    }

    @Test
    void adminEnviaAvisoGeralParaTodosUsuariosSemLinhaENaoCriaOcorrencia() throws Exception {
        User usuarioA = criarUsuario("aviso.geral.a@teste.com", "52345678903");
        User usuarioB = criarUsuario("aviso.geral.b@teste.com", "52345678904");
        User usuarioC = criarUsuario("aviso.geral.c@teste.com", "52345678905");
        long ocorrenciasAntes = ocorrenciaRepository.count();

        String aviso = """
                {
                  "enviarParaTodos": true,
                  "titulo": "Manutencao programada",
                  "mensagem": "O sistema ficara indisponivel temporariamente."
                }
                """;

        mockMvc.perform(post("/api/notificacoes")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aviso))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linhaId").value(nullValue()))
                .andExpect(jsonPath("$.usuariosNotificados").value(greaterThanOrEqualTo(3)));

        assertThat(ocorrenciaRepository.count()).isEqualTo(ocorrenciasAntes);
        assertThat(notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioA.getId())).hasSize(1);
        assertThat(notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioB.getId())).hasSize(1);
        assertThat(notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioC.getId())).hasSize(1);

        Notificacao notificacao = notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioA.getId()).get(0);
        assertThat(notificacao.getLinha()).isNull();
        assertThat(notificacao.getOcorrencia()).isNull();
        assertThat(notificacao.getTipoNotificacao()).isEqualTo(TipoNotificacao.AVISO);
        assertThat(notificacao.getStatus()).isEqualTo(StatusNotificacao.ATIVA);
    }

    @Test
    void usuarioQueNaoFavoritouLinhaNaoRecebeNotificacaoDaLinha() throws Exception {
        Linha linha = criarLinha("TST-208", "Linha Apenas Um Favorito");
        User usuarioFavoritou = criarUsuario("recebe.notificacao@teste.com", "12345678912");
        User usuarioNaoFavoritou = criarUsuario("nao.recebe.notificacao@teste.com", "12345678913");

        favoritar(usuarioFavoritou, linha);

        enviarNotificacaoDaLinha(linha, "Aviso da linha", "Somente favoritos recebem", false)
                .andExpect(jsonPath("$.usuariosNotificados").value(1));

        assertThat(linhaRepository.findById(linha.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusLinha.NORMAL);
        assertThat(notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioFavoritou.getId())).hasSize(1);
        assertThat(notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioNaoFavoritou.getId())).isEmpty();
    }

    @Test
    void marcarNotificacaoComoLidaNaoAlteraNotificacaoDeOutroUsuario() throws Exception {
        Linha linha = criarLinha("TST-209", "Linha Leitura Individual");
        User usuarioA = criarUsuario("leitor.a@teste.com", "12345678914");
        User usuarioB = criarUsuario("leitor.b@teste.com", "12345678915");

        favoritar(usuarioA, linha);
        favoritar(usuarioB, linha);
        enviarNotificacaoDaLinha(linha, "Aviso compartilhado", "Cada usuario possui sua leitura", false);

        Notificacao notificacaoA = notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioA.getId()).get(0);
        Notificacao notificacaoB = notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioB.getId()).get(0);

        mockMvc.perform(patch("/api/notificacoes/{id}/lida", notificacaoA.getId())
                        .with(user(usuarioA.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lida").value(true));

        assertThat(notificacaoRepository.findById(notificacaoA.getId()).orElseThrow().getLida()).isTrue();
        assertThat(notificacaoRepository.findById(notificacaoB.getId()).orElseThrow().getLida()).isFalse();
    }

    @Test
    void usuarioLimpaApenasSeuHistoricoDeNotificacoesSuperadas() throws Exception {
        User usuario = criarUsuario("limpa.historico@teste.com", "12345678936");
        User outroUsuario = criarUsuario("historico.outro@teste.com", "12345678937");
        Notificacao atual = criarNotificacao(usuario, StatusNotificacao.ATIVA);
        criarNotificacao(usuario, StatusNotificacao.SUPERADA);
        criarNotificacao(usuario, StatusNotificacao.SUPERADA);
        criarNotificacao(usuario, StatusNotificacao.SUPERADA);
        Notificacao superadaOutroUsuario = criarNotificacao(outroUsuario, StatusNotificacao.SUPERADA);

        mockMvc.perform(delete("/api/notificacoes/minhas/historico")
                        .with(user(usuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacoesRemovidas").value(3));

        assertThat(notificacaoRepository.findById(atual.getId())).isPresent();
        assertThat(notificacaoRepository.findById(superadaOutroUsuario.getId())).isPresent();
        assertThat(notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
                usuario.getId(),
                StatusNotificacao.SUPERADA
        )).isEmpty();
        assertThat(notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
                usuario.getId(),
                StatusNotificacao.ATIVA
        )).hasSize(1);
    }

    @Test
    void adminMarcaAvisoOficialComoSuperadoEUsuarioLimpaApenasSeuHistorico() throws Exception {
        Linha linha = criarLinha("TST-225", "Linha Aviso Oficial");
        List<User> usuarios = new java.util.ArrayList<>();

        for (int indice = 1; indice <= 5; indice++) {
            User usuario = criarUsuario(
                    "favorito.aviso." + indice + "@teste.com",
                    "42345678" + String.format("%03d", indice)
            );
            usuarios.add(usuario);
            favoritar(usuario, linha);
        }

        enviarNotificacaoDaLinha(linha, "Circular interrompida", "Vila Brasil esta interrompida.", false)
                .andExpect(jsonPath("$.usuariosNotificados").value(5));

        List<Notificacao> ativas = notificacaoRepository.findByLinhaIdAndStatusAndOcorrenciaIsNull(
                linha.getId(),
                StatusNotificacao.ATIVA
        );
        assertThat(ativas).hasSize(5);

        mockMvc.perform(get("/api/notificacoes/avisos")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Circular interrompida"))
                .andExpect(jsonPath("$[0].status").value("ATIVA"))
                .andExpect(jsonPath("$[0].usuariosAlcancados").value(5));

        mockMvc.perform(patch("/api/notificacoes/avisos/{id}/superada", ativas.get(0).getId())
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPERADA"))
                .andExpect(jsonPath("$.usuariosAlcancados").value(5));

        assertThat(notificacaoRepository.findByLinhaIdAndStatusAndOcorrenciaIsNull(
                linha.getId(),
                StatusNotificacao.ATIVA
        )).isEmpty();
        assertThat(notificacaoRepository.findByLinhaIdAndStatusAndOcorrenciaIsNull(
                linha.getId(),
                StatusNotificacao.SUPERADA
        )).hasSize(5);

        User primeiroUsuario = usuarios.get(0);
        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", primeiroUsuario.getId())
                        .param("status", "ATIVA")
                        .with(user(primeiroUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", primeiroUsuario.getId())
                        .param("status", "SUPERADA")
                        .with(user(primeiroUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/notificacoes/minhas/historico")
                        .with(user(primeiroUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacoesRemovidas").value(1));

        assertThat(notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
                primeiroUsuario.getId(),
                StatusNotificacao.SUPERADA
        )).isEmpty();

        for (int indice = 1; indice < usuarios.size(); indice++) {
            assertThat(notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
                    usuarios.get(indice).getId(),
                    StatusNotificacao.SUPERADA
            )).hasSize(1);
        }
    }

    @Test
    void encerrarOcorrenciaSuperaNotificacoesVinculadasSemCriarNovoAvisoAtivo() throws Exception {
        Linha linha = criarLinha("TST-210", "Linha Ocorrencia Historico");
        User usuario = criarUsuario("historico.ocorrencia@teste.com", "12345678916");
        favoritar(usuario, linha);

        criarOcorrencia(linha, "VEICULO_QUEBRADO", "Onibus quebrado no terminal", "ALTA");
        Notificacao avisoAntigo = notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
                usuario.getId(),
                StatusNotificacao.ATIVA
        ).get(0);

        Ocorrencia ocorrencia = ocorrenciaRepository.findByLinhaIdAndAtivaOrderByDataHoraDesc(linha.getId(), true).get(0);
        mockMvc.perform(patch("/api/ocorrencias/{id}/encerrar", ocorrencia.getId())
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());

        assertThat(notificacaoRepository.findById(avisoAntigo.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusNotificacao.SUPERADA);

        List<Notificacao> atuais = notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
                usuario.getId(),
                StatusNotificacao.ATIVA
        );
        assertThat(atuais).isEmpty();

        List<Notificacao> historico = notificacaoRepository.findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
                usuario.getId(),
                StatusNotificacao.SUPERADA
        );
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getLida()).isFalse();
        assertThat(historico.get(0).getOcorrencia().getId()).isEqualTo(ocorrencia.getId());
    }

    @Test
    void notificacaoDeUmaLinhaNaoSuperaAvisoManualDeOutraLinha() throws Exception {
        Linha vilaBrasil = criarLinha("TST-211", "Vila Brasil");
        Linha fatec = criarLinha("TST-212", "FATEC AME");
        User usuario = criarUsuario("linhas.diferentes@teste.com", "12345678917");

        favoritar(usuario, vilaBrasil);
        favoritar(usuario, fatec);

        enviarNotificacaoDaLinha(fatec, "Aviso FATEC", "Aviso ainda valido", false);
        enviarNotificacaoDaLinha(vilaBrasil, "Aviso Vila Brasil", "Substitui apenas avisos da Vila Brasil", true);

        List<Notificacao> notificacoesFatec = notificacaoRepository.findByLinhaIdAndStatusAndOcorrenciaIsNull(
                fatec.getId(),
                StatusNotificacao.ATIVA
        );

        assertThat(notificacoesFatec).hasSize(1);
        assertThat(notificacoesFatec.get(0).getTitulo()).isEqualTo("Aviso FATEC");
    }

    @Test
    void usuarioNaoPodeListarReportesPorLinha() throws Exception {
        Linha linha = criarLinha("TST-205", "Linha Reporte Restrito");

        mockMvc.perform(get("/api/linhas/{linhaId}/reportes", linha.getId())
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListaUsuariosSemExporDadosSensiveis() throws Exception {
        User usuario = criarUsuario("usuario.resumo@teste.com", "12345678908");

        mockMvc.perform(get("/api/usuarios")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(usuario.getEmail())))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].cpf").doesNotExist());
    }

    @Test
    void linhaPermaneceAlteradaQuandoAindaExisteOutraOcorrenciaAtiva() throws Exception {
        Linha linha = criarLinha("TST-206", "Linha Multiplas Ocorrencias");

        criarOcorrencia(linha, "ATRASO", "Primeiro atraso ativo", "MEDIA");
        criarOcorrencia(linha, "VEICULO_QUEBRADO", "Veiculo parado no terminal", "ALTA");

        List<Ocorrencia> ocorrenciasAtivas = ocorrenciaRepository.findByLinhaIdAndAtivaOrderByDataHoraDesc(linha.getId(), true);
        assertThat(ocorrenciasAtivas).hasSize(2);

        mockMvc.perform(patch("/api/ocorrencias/{id}/encerrar", ocorrenciasAtivas.get(0).getId())
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        Linha linhaAtualizada = linhaRepository.findById(linha.getId()).orElseThrow();
        assertThat(linhaAtualizada.getStatus()).isNotEqualTo(StatusLinha.NORMAL);
    }

    @Test
    void usuarioListaSomenteOcorrenciasAtivasQuandoUsaFiltroDeAtivas() throws Exception {
        Linha linha = criarLinha("TST-223", "Linha Ocorrencia Atual");
        criarOcorrencia(linha, "VEICULO_QUEBRADO", "Problema ainda ativo", "ALTA");
        Ocorrencia ocorrencia = ocorrenciaRepository.findByLinhaIdAndAtivaOrderByDataHoraDesc(linha.getId(), true).get(0);

        mockMvc.perform(get("/api/ocorrencias")
                        .param("ativa", "true")
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(ocorrencia.getId().intValue())));

        mockMvc.perform(patch("/api/ocorrencias/{id}/encerrar", ocorrencia.getId())
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        mockMvc.perform(get("/api/ocorrencias")
                        .param("ativa", "true")
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(ocorrencia.getId().intValue()))));

        assertThat(ocorrenciaRepository.findById(ocorrencia.getId())).isPresent();
        assertThat(ocorrenciaRepository.findById(ocorrencia.getId()).orElseThrow().getAtiva()).isFalse();
    }

    @Test
    void usuarioListaSomenteOcorrenciasAtivasMesmoSemFiltro() throws Exception {
        Linha linha = criarLinha("TST-241", "Linha Historico Ocorrencia");
        criarOcorrencia(linha, "ATRASO", "Problema ativo", "MEDIA");
        criarOcorrencia(linha, "DESVIO", "Problema encerrado", "MEDIA");

        List<Ocorrencia> ocorrenciasAtivas = ocorrenciaRepository.findByLinhaIdAndAtivaOrderByDataHoraDesc(linha.getId(), true);
        Ocorrencia encerrada = ocorrenciasAtivas.get(0);

        mockMvc.perform(patch("/api/ocorrencias/{id}/encerrar", encerrada.getId())
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ocorrencias")
                        .with(user("usuario@teste.com").roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(encerrada.getId().intValue()))));

        mockMvc.perform(get("/api/ocorrencias")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(encerrada.getId().intValue())));
    }

    @Test
    void feedColaborativoNaoExpoeDadosSensiveisDoAutor() throws Exception {
        Linha linha = criarLinha("TST-213", "Linha Feed Publico");
        User autor = criarUsuario("autor.feed@teste.com", "12345678918");
        User leitor = criarUsuario("leitor.feed@teste.com", "12345678919");
        ReporteUsuario reporte = criarReporte(autor, linha, "LOTADO");

        mockMvc.perform(get("/api/reportes/feed")
                        .with(user(leitor.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reporteId").value(reporte.getId()))
                .andExpect(jsonPath("$[0].linhaId").value(linha.getId()))
                .andExpect(jsonPath("$[0].autorNome").value(autor.getNome()))
                .andExpect(jsonPath("$[0].autorConfiabilidade").value(1.0))
                .andExpect(jsonPath("$[0].quantidadeConfirmacoes").value(0))
                .andExpect(jsonPath("$[0].confirmadoPeloUsuarioAutenticado").value(false))
                .andExpect(jsonPath("$[0].autorDoUsuarioAutenticado").value(false))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].cpf").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].dataNascimento").doesNotExist());
    }

    @Test
    void feedColaborativoMostraParadaQuandoReporteTemParada() throws Exception {
        Linha linha = criarLinha("TST-229", "Linha Feed Parada");
        Parada parada = criarParada(linha, "Rodoviaria Teste", 1, Sentido.VOLTA);
        User autor = criarUsuario("autor.feed.parada@teste.com", "12345678941");
        User leitor = criarUsuario("leitor.feed.parada@teste.com", "12345678942");

        String reporte = """
                {
                  "paradaId": %d,
                  "tipoReporte": "NAO_PASSOU"
                }
                """.formatted(parada.getId());

        mockMvc.perform(post("/api/reportes/minhas/paradas")
                        .with(user(autor.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reportes/feed")
                        .with(user(leitor.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].linhaId").value(linha.getId()))
                .andExpect(jsonPath("$[0].paradaId").value(parada.getId()))
                .andExpect(jsonPath("$[0].paradaNome").value(parada.getNome()))
                .andExpect(jsonPath("$[0].paradaSentido").value("VOLTA"));
    }

    @Test
    void usuarioConfirmaReporteSemAumentarConfiabilidadeAntesDeCincoConfirmacoes() throws Exception {
        Linha linha = criarLinha("TST-214", "Linha Confirmacao Valida");
        User autor = criarUsuario("autor.confirmacao@teste.com", "12345678920");
        User confirmador = criarUsuario("confirmador@teste.com", "12345678921");
        ReporteUsuario reporte = criarReporte(autor, linha, "ATRASADO");

        mockMvc.perform(post("/api/reportes/{reporteId}/confirmacoes", reporte.getId())
                        .with(user(confirmador.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reporteId").value(reporte.getId()))
                .andExpect(jsonPath("$.usuarioConfirmacaoId").value(confirmador.getId()))
                .andExpect(jsonPath("$.quantidadeConfirmacoes").value(1))
                .andExpect(jsonPath("$.autorConfiabilidade").value(1.00));

        assertThat(confirmacaoReporteRepository.countByReporteId(reporte.getId())).isEqualTo(1);
        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(userRepository.findById(confirmador.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void autorNaoPodeConfirmarProprioReporte() throws Exception {
        Linha linha = criarLinha("TST-215", "Linha Confirmacao Propria");
        User autor = criarUsuario("autor.proprio@teste.com", "12345678922");
        ReporteUsuario reporte = criarReporte(autor, linha, "NAO_PASSOU");

        mockMvc.perform(post("/api/reportes/{reporteId}/confirmacoes", reporte.getId())
                        .with(user(autor.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isBadRequest());

        assertThat(confirmacaoReporteRepository.countByReporteId(reporte.getId())).isZero();
        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void usuarioNaoPodeConfirmarMesmoReporteDuasVezes() throws Exception {
        Linha linha = criarLinha("TST-216", "Linha Confirmacao Duplicada");
        User autor = criarUsuario("autor.duplicado@teste.com", "12345678923");
        User confirmador = criarUsuario("confirmador.duplicado@teste.com", "12345678924");
        ReporteUsuario reporte = criarReporte(autor, linha, "PASSOU");

        mockMvc.perform(post("/api/reportes/{reporteId}/confirmacoes", reporte.getId())
                        .with(user(confirmador.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reportes/{reporteId}/confirmacoes", reporte.getId())
                        .with(user(confirmador.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isBadRequest());

        assertThat(confirmacaoReporteRepository.countByReporteId(reporte.getId())).isEqualTo(1);
        assertThat(reporteUsuarioRepository.findAll()).hasSize(1);
        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void confirmacoesDeUsuariosDiferentesSomamNoMesmoReporte() throws Exception {
        Linha linha = criarLinha("TST-217", "Linha Duas Confirmacoes");
        User autor = criarUsuario("autor.duas@teste.com", "12345678925");
        User usuarioA = criarUsuario("confirmador.a@teste.com", "12345678926");
        User usuarioB = criarUsuario("confirmador.b@teste.com", "12345678927");
        ReporteUsuario reporte = criarReporte(autor, linha, "UNIFICADA");

        mockMvc.perform(post("/api/reportes/{reporteId}/confirmacoes", reporte.getId())
                        .with(user(usuarioA.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reportes/{reporteId}/confirmacoes", reporte.getId())
                        .with(user(usuarioB.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeConfirmacoes").value(2))
                .andExpect(jsonPath("$.autorConfiabilidade").value(1.00));

        assertThat(confirmacaoReporteRepository.countByReporteId(reporte.getId())).isEqualTo(2);
        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void confiabilidadeAumentaSomenteEmBlocosDeCincoConfirmacoes() throws Exception {
        Linha linha = criarLinha("TST-224", "Linha Bonus Confiabilidade");
        User autor = criarUsuario("autor.bonus@teste.com", "12345678938");
        ReporteUsuario reporte = criarReporte(autor, linha, "ATRASADO");
        List<User> confirmadores = new java.util.ArrayList<>();

        for (int indice = 1; indice <= 10; indice++) {
            confirmadores.add(criarUsuario(
                    "confirmador.bonus." + indice + "@teste.com",
                    "22345678" + String.format("%03d", indice)
            ));
        }

        for (int indice = 0; indice < 4; indice++) {
            confirmarReporte(reporte, confirmadores.get(indice))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.autorConfiabilidade").value(1.00));
        }

        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("1.00"));

        confirmarReporte(reporte, confirmadores.get(4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeConfirmacoes").value(5))
                .andExpect(jsonPath("$.autorConfiabilidade").value(1.10));

        assertThat(reporteUsuarioRepository.findById(reporte.getId()).orElseThrow()
                .getBlocosConfiabilidadeRecompensados()).isEqualTo(1);

        for (int indice = 5; indice < 9; indice++) {
            confirmarReporte(reporte, confirmadores.get(indice))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.autorConfiabilidade").value(1.10));
        }

        confirmarReporte(reporte, confirmadores.get(9))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeConfirmacoes").value(10))
                .andExpect(jsonPath("$.autorConfiabilidade").value(1.20));

        assertThat(confirmacaoReporteRepository.countByReporteId(reporte.getId())).isEqualTo(10);
        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("1.20"));
        assertThat(reporteUsuarioRepository.findById(reporte.getId()).orElseThrow()
                .getBlocosConfiabilidadeRecompensados()).isEqualTo(2);

        confirmarReporte(reporte, confirmadores.get(9))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("1.20"));
    }

    @Test
    void confirmacaoDeUmReporteNaoAlteraOutroReporte() throws Exception {
        Linha linhaA = criarLinha("TST-218", "Linha Reporte A");
        Linha linhaB = criarLinha("TST-219", "Linha Reporte B");
        User autorA = criarUsuario("autor.a@teste.com", "12345678928");
        User autorB = criarUsuario("autor.b@teste.com", "12345678929");
        User confirmador = criarUsuario("confirmador.independente@teste.com", "12345678930");
        ReporteUsuario reporteA = criarReporte(autorA, linhaA, "LOTADO");
        ReporteUsuario reporteB = criarReporte(autorB, linhaB, "ATRASADO");

        mockMvc.perform(post("/api/reportes/{reporteId}/confirmacoes", reporteA.getId())
                        .with(user(confirmador.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isCreated());

        assertThat(confirmacaoReporteRepository.countByReporteId(reporteA.getId())).isEqualTo(1);
        assertThat(confirmacaoReporteRepository.countByReporteId(reporteB.getId())).isZero();
        assertThat(userRepository.findById(autorA.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(userRepository.findById(autorB.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void feedColaborativoPodeSerFiltradoPorLinha() throws Exception {
        Linha linhaA = criarLinha("TST-220", "Linha Feed A");
        Linha linhaB = criarLinha("TST-221", "Linha Feed B");
        User autor = criarUsuario("autor.filtro@teste.com", "12345678931");
        User leitor = criarUsuario("leitor.filtro@teste.com", "12345678932");
        ReporteUsuario reporteA = criarReporte(autor, linhaA, "LOTADO");
        criarReporte(autor, linhaB, "PASSOU");

        mockMvc.perform(get("/api/reportes/feed")
                        .param("linhaId", linhaA.getId().toString())
                        .with(user(leitor.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reporteId").value(reporteA.getId()))
                .andExpect(jsonPath("$[0].linhaId").value(linhaA.getId()));
    }

    @Test
    void feedColaborativoMostraSomenteReportesRecentesEmOrdemDecrescente() throws Exception {
        Linha linha = criarLinha("TST-236", "Linha Feed Recentes");
        Parada parada = criarParada(linha, "Parada Feed Recente", 1, Sentido.IDA);
        User autor = criarUsuario("autor.feed.recente@teste.com", "42345678906");
        User leitor = criarUsuario("leitor.feed.recente@teste.com", "42345678907");

        criarReporteEntidade(autor, linha, parada, TipoReporte.PASSOU, LocalDateTime.now().minusMinutes(21));
        ReporteUsuario reporteAntigoRecente = criarReporteEntidade(
                autor,
                linha,
                parada,
                TipoReporte.ATRASADO,
                LocalDateTime.now().minusMinutes(10)
        );
        ReporteUsuario reporteNovo = criarReporteEntidade(
                autor,
                linha,
                parada,
                TipoReporte.NAO_PASSOU,
                LocalDateTime.now().minusMinutes(1)
        );

        MvcResult result = mockMvc.perform(get("/api/reportes/feed")
                        .param("linhaId", linha.getId().toString())
                        .with(user(leitor.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reporteId").value(reporteNovo.getId()))
                .andExpect(jsonPath("$[0].paradaId").value(parada.getId()))
                .andExpect(jsonPath("$[0].paradaNome").value(parada.getNome()))
                .andExpect(jsonPath("$[1].reporteId").value(reporteAntigoRecente.getId()))
                .andReturn();

        List<Integer> reporteIds = JsonPath.read(result.getResponse().getContentAsString(), "$[*].reporteId");
        assertThat(reporteIds).doesNotHaveDuplicates();
    }

    @Test
    void limpezaRemoveReporteExpiradoEConfirmacoesSemAlterarConfiabilidade() throws Exception {
        Linha linha = criarLinha("TST-237", "Linha Cleanup Reporte");
        Parada parada = criarParada(linha, "Parada Cleanup Reporte", 1, Sentido.IDA);
        User autor = criarUsuario("autor.cleanup.reporte@teste.com", "42345678908");
        User confirmadorA = criarUsuario("confirmador.cleanup.a@teste.com", "42345678909");
        User confirmadorB = criarUsuario("confirmador.cleanup.b@teste.com", "42345678910");
        User confirmadorC = criarUsuario("confirmador.cleanup.c@teste.com", "42345678911");
        autor.setConfiabilidade(new BigDecimal("1.20"));
        userRepository.saveAndFlush(autor);

        ReporteUsuario reporteExpirado = criarReporteEntidade(
                autor,
                linha,
                parada,
                TipoReporte.PASSOU,
                LocalDateTime.now().minusMinutes(21)
        );
        criarConfirmacao(reporteExpirado, confirmadorA);
        criarConfirmacao(reporteExpirado, confirmadorB);
        criarConfirmacao(reporteExpirado, confirmadorC);

        assertThat(confirmacaoReporteRepository.countByReporteId(reporteExpirado.getId())).isEqualTo(3);

        int removidos = dadosTemporariosCleanupService.limparReportesExpirados();

        assertThat(removidos).isGreaterThanOrEqualTo(1);
        assertThat(reporteUsuarioRepository.findById(reporteExpirado.getId())).isEmpty();
        assertThat(confirmacaoReporteRepository.countByReporteId(reporteExpirado.getId())).isZero();
        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("1.20"));
    }

    @Test
    void limpezaDeOcorrenciasRemoveSomenteEncerradasAntigas() {
        Linha linha = criarLinha("TST-238", "Linha Cleanup Ocorrencia");
        Ocorrencia ocorrenciaAtiva = criarOcorrenciaEntidade(
                linha,
                true,
                null,
                "ATIVA"
        );
        Ocorrencia ocorrenciaEncerradaRecente = criarOcorrenciaEntidade(
                linha,
                false,
                LocalDateTime.now().minusMinutes(30),
                "ENCERRADA_RECENTE"
        );
        Ocorrencia ocorrenciaEncerradaAntiga = criarOcorrenciaEntidade(
                linha,
                false,
                LocalDateTime.now().minusHours(2),
                "ENCERRADA_ANTIGA"
        );
        User usuario = criarUsuario("notificacao.cleanup.ocorrencia@teste.com", "42345678912");
        Notificacao notificacao = criarNotificacao(usuario);
        notificacao.setOcorrencia(ocorrenciaEncerradaAntiga);
        notificacaoRepository.saveAndFlush(notificacao);

        int removidas = dadosTemporariosCleanupService.limparOcorrenciasEncerradasAntigas();

        assertThat(removidas).isGreaterThanOrEqualTo(1);
        assertThat(ocorrenciaRepository.findById(ocorrenciaAtiva.getId())).isPresent();
        assertThat(ocorrenciaRepository.findById(ocorrenciaEncerradaRecente.getId())).isPresent();
        assertThat(ocorrenciaRepository.findById(ocorrenciaEncerradaAntiga.getId())).isEmpty();
        assertThat(notificacaoRepository.findById(notificacao.getId())).isPresent();
        assertThat(notificacaoRepository.findById(notificacao.getId()).orElseThrow().getOcorrencia()).isNull();
    }

    @Test
    void confiabilidadeDoAutorNaoUltrapassaLimiteMaximo() throws Exception {
        Linha linha = criarLinha("TST-222", "Linha Confiabilidade Maxima");
        User autor = criarUsuario("autor.maximo@teste.com", "12345678933");
        autor.setConfiabilidade(new BigDecimal("1.95"));
        userRepository.saveAndFlush(autor);
        ReporteUsuario reporte = criarReporte(autor, linha, "NAO_PASSOU");

        for (int indice = 1; indice <= 4; indice++) {
            User confirmador = criarUsuario(
                    "confirmador.max." + indice + "@teste.com",
                    "32345678" + String.format("%03d", indice)
            );
            confirmarReporte(reporte, confirmador)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.autorConfiabilidade").value(1.95));
        }

        User quintoConfirmador = criarUsuario("confirmador.max.5@teste.com", "32345678005");
        confirmarReporte(reporte, quintoConfirmador)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorConfiabilidade").value(2.00));

        assertThat(userRepository.findById(autor.getId()).orElseThrow().getConfiabilidade())
                .isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    void adminNaoPodeListarTodasNotificacoesIndividuais() throws Exception {
        mockMvc.perform(get("/api/notificacoes")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void novoUsuarioCadastradoDepoisRecebeAvisoGeralAtivoEHistoricoSemDuplicar() throws Exception {
        String avisoAtivo = """
                {
                  "enviarParaTodos": true,
                  "titulo": "Feriado Municipal",
                  "mensagem": "Horarios reduzidos no feriado municipal."
                }
                """;
        String avisoEncerrado = """
                {
                  "enviarParaTodos": true,
                  "titulo": "Comunicado Anterior",
                  "mensagem": "Comunicado antigo que deve aparecer no historico."
                }
                """;

        mockMvc.perform(post("/api/notificacoes")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(avisoAtivo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/notificacoes")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(avisoEncerrado))
                .andExpect(status().isCreated());

        MvcResult avisosResult = mockMvc.perform(get("/api/notificacoes/avisos")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        List<Number> avisoIdsEncerrado = JsonPath.read(
                avisosResult.getResponse().getContentAsString(),
                "$[?(@.titulo == 'Comunicado Anterior')].avisoId"
        );
        Long avisoEncerradoId = avisoIdsEncerrado.get(0).longValue();

        mockMvc.perform(patch("/api/notificacoes/avisos/oficial/{avisoId}/superada", avisoEncerradoId)
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPERADA"));

        // Novo usuario cadastrado APOS a criacao dos avisos
        mockMvc.perform(post("/cadastrar")
                        .with(csrf())
                        .param("nome", "Novo Passageiro")
                        .param("cpf", "611.222.333-44")
                        .param("dataNascimento", "1999-03-20")
                        .param("email", "novo.passageiro.notif@teste.com")
                        .param("senha", "SenhaSegura99"))
                .andExpect(status().is3xxRedirection());

        User novoUsuario = userRepository.findByEmail("novo.passageiro.notif@teste.com").orElseThrow();

        // Deve receber o aviso ativo como nao lido (indicador da Home)
        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", novoUsuario.getId())
                        .param("status", "ATIVA")
                        .param("lida", "false")
                        .with(user(novoUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Feriado Municipal"))
                .andExpect(jsonPath("$[0].lida").value(false));

        // Consultar novamente nao deve duplicar registros
        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", novoUsuario.getId())
                        .param("status", "ATIVA")
                        .with(user(novoUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Deve enxergar o aviso superado no historico
        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", novoUsuario.getId())
                        .param("status", "SUPERADA")
                        .with(user(novoUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Comunicado Anterior"));

        // Ao limpar o historico, o aviso superado nao deve voltar nas consultas seguintes
        mockMvc.perform(delete("/api/notificacoes/minhas/historico")
                        .with(user(novoUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacoesRemovidas").value(1));

        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", novoUsuario.getId())
                        .param("status", "SUPERADA")
                        .with(user(novoUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void usuarioQueFavoritaLinhaDepoisRecebeAvisoAtivoDaLinha() throws Exception {
        Linha linha = criarLinha("TST-301", "Linha Aviso Futuro");

        enviarNotificacaoDaLinha(linha, "Desvio temporario", "Desvio na av principal.", false)
                .andExpect(jsonPath("$.usuariosNotificados").value(0));

        User novoUsuario = criarUsuario("favorito.tardio@teste.com", "62233344455");

        // Antes de favoritar, nao recebe aviso especifico da linha
        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", novoUsuario.getId())
                        .param("status", "ATIVA")
                        .with(user(novoUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Ao favoritar a linha, passa a receber o aviso ativo dela
        favoritar(novoUsuario, linha);

        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", novoUsuario.getId())
                        .param("status", "ATIVA")
                        .param("lida", "false")
                        .with(user(novoUsuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Desvio temporario"));
    }

    @Test
    void migracaoLegadaPermiteQueNovosUsuariosRecebamNotificacoesAntigas() throws Exception {
        User usuarioAntigo = criarUsuario("legado.antigo@teste.com", "63344455566");
        Notificacao legada = criarNotificacao(usuarioAntigo, StatusNotificacao.ATIVA);
        legada.setTitulo("Circular Legada");
        legada.setMensagem("Aviso ja existente no banco antes da atualizacao.");
        notificacaoRepository.saveAndFlush(legada);

        notificacaoService.migrarAvisosLegadosSeNecessario();

        User usuarioNovo = criarUsuario("legado.novo@teste.com", "63344455577");
        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", usuarioNovo.getId())
                        .param("status", "ATIVA")
                        .param("lida", "false")
                        .with(user(usuarioNovo.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", hasItem("Circular Legada")));
    }

    @Test
    void adminExcluiAvisoOficialERemoveDeTodosUsuariosEIndicadores() throws Exception {
        User usuarioA = criarUsuario("exclui.aviso.a@teste.com", "64455566677");

        String aviso = """
                {
                  "enviarParaTodos": true,
                  "titulo": "Feriado Expirado",
                  "mensagem": "Aviso que sera removido pelo admin."
                }
                """;

        mockMvc.perform(post("/api/notificacoes")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aviso))
                .andExpect(status().isCreated());

        MvcResult avisosResult = mockMvc.perform(get("/api/notificacoes/avisos")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        List<Number> ids = JsonPath.read(
                avisosResult.getResponse().getContentAsString(),
                "$[?(@.titulo == 'Feriado Expirado')].avisoId"
        );
        Long avisoId = ids.get(0).longValue();

        // USER e visitante nao podem excluir
        mockMvc.perform(delete("/api/notificacoes/avisos/{avisoId}", avisoId)
                        .with(user(usuarioA.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/notificacoes/avisos/{avisoId}", avisoId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // ADMIN exclui definitivamente
        mockMvc.perform(delete("/api/notificacoes/avisos/{avisoId}", avisoId)
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(avisoOficialRepository.findById(avisoId)).isEmpty();

        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", usuarioA.getId())
                        .with(user(usuarioA.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", not(hasItem("Feriado Expirado"))));

        mockMvc.perform(get("/api/notificacoes/publicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", not(hasItem("Feriado Expirado"))));
    }

    @Test
    void visitanteConsultaApisPublicasMasFicaBloqueadoEmAcoesRestritas() throws Exception {
        Linha linha = criarLinha("TST-302", "Linha Publica Visitante");
        Parada parada = criarParada(linha, "Terminal Publico", 1, Sentido.IDA);
        cadastrarHorario(parada, "07:30:00");
        User autor = criarUsuario("autor.publico@teste.com", "65566677788");
        criarReporte(autor, linha, "PASSOU");

        criarOcorrencia(linha, "ATRASO", "Ocorrencia ativa publica", "MEDIA");
        criarOcorrencia(linha, "DESVIO", "Ocorrencia encerrada privada", "BAIXA");
        List<Ocorrencia> ativas = ocorrenciaRepository.findByLinhaIdAndAtivaOrderByDataHoraDesc(linha.getId(), true);
        Ocorrencia encerrada = ativas.get(0);
        mockMvc.perform(patch("/api/ocorrencias/{id}/encerrar", encerrada.getId())
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());

        String avisoGeral = """
                {
                  "enviarParaTodos": true,
                  "titulo": "Comunicado Publico",
                  "mensagem": "Informacao visivel a visitantes."
                }
                """;
        mockMvc.perform(post("/api/notificacoes")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(avisoGeral))
                .andExpect(status().isCreated());

        // Consultas publicas liberadas para visitantes
        mockMvc.perform(get("/api/linhas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("TST-302")));

        mockMvc.perform(get("/api/linhas/{linhaId}/paradas", linha.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Terminal Publico"));

        mockMvc.perform(get("/api/paradas/{paradaId}/horarios", parada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].horarioPrevisto").value("07:30:00"));

        mockMvc.perform(get("/api/ocorrencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(encerrada.getId().intValue()))));

        mockMvc.perform(get("/api/reportes/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].confirmadoPeloUsuarioAutenticado").value(false))
                .andExpect(jsonPath("$[0].autorDoUsuarioAutenticado").value(false))
                .andExpect(jsonPath("$[0].email").doesNotExist());

        mockMvc.perform(get("/api/notificacoes/publicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", hasItem("Comunicado Publico")));

        // Endpoints restritos continuam bloqueados para visitantes
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/notificacoes/avisos"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/usuarios/{usuarioId}/notificacoes", autor.getId()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/api/usuarios/{usuarioId}/linhas-favoritas/{linhaId}", autor.getId(), linha.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/api/linhas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PUB-1\",\"nome\":\"Bloqueada\",\"status\":\"NORMAL\"}"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/api/usuarios/admins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Fake\",\"cpf\":\"11122233344\",\"dataNascimento\":\"1995-01-01\",\"email\":\"fake@teste.com\",\"senha\":\"SenhaForte123\"}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminCadastraNovoAdministradorComSegurancaEUserNaoPode() throws Exception {
        User usuarioComum = criarUsuario("comum.admin.teste@teste.com", "66677788899");
        String novoAdminPayload = """
                {
                  "nome": "Novo Administrador",
                  "cpf": "555.666.777-88",
                  "dataNascimento": "1992-08-10",
                  "email": "novo.admin@waypoint.com",
                  "senha": "AdminSeguro2026"
                }
                """;

        // USER comum nao pode criar admin
        mockMvc.perform(post("/api/usuarios/admins")
                        .with(user(usuarioComum.getEmail()).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novoAdminPayload))
                .andExpect(status().isForbidden());

        // ADMIN cria novo admin com sucesso sem expor senha ou CPF
        mockMvc.perform(post("/api/usuarios/admins")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novoAdminPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Novo Administrador"))
                .andExpect(jsonPath("$.email").value("novo.admin@waypoint.com"))
                .andExpect(jsonPath("$.tipoUsuario").value("ADMIN"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.cpf").doesNotExist());

        User adminSalvo = userRepository.findByEmail("novo.admin@waypoint.com").orElseThrow();
        assertThat(adminSalvo.getTipoUsuario()).isEqualTo(TipoUsuario.ADMIN);
        assertThat(adminSalvo.getCpf()).isEqualTo("55566677788");
        assertThat(passwordEncoder.matches("AdminSeguro2026", adminSalvo.getSenha())).isTrue();

        // Senha fraca rejeitada
        String senhaFracaPayload = """
                {
                  "nome": "Admin Fraco",
                  "cpf": "555.666.777-99",
                  "dataNascimento": "1992-08-10",
                  "email": "admin.fraco@waypoint.com",
                  "senha": "12345678"
                }
                """;
        mockMvc.perform(post("/api/usuarios/admins")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(senhaFracaPayload))
                .andExpect(status().isBadRequest());
    }

    private Linha criarLinha(String codigo, String nome) {
        Linha linha = new Linha();
        linha.setCodigo(codigo);
        linha.setNome(nome);
        linha.setStatus(StatusLinha.NORMAL);
        return linhaRepository.save(linha);
    }

    private Parada criarParada(Linha linha, String nome, int ordem, Sentido sentido) throws Exception {
        String parada = """
                {
                  "linhaId": %d,
                  "nome": "%s",
                  "latitude": -22.12345678,
                  "longitude": -51.12345678,
                  "ordem": %d,
                  "sentido": "%s"
                }
                """.formatted(linha.getId(), nome, ordem, sentido.name());

        mockMvc.perform(post("/api/paradas")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(parada))
                .andExpect(status().isCreated());

        return paradaRepository.findByLinhaIdAndSentidoOrderByOrdemAsc(linha.getId(), sentido)
                .stream()
                .filter(item -> item.getNome().equals(nome))
                .findFirst()
                .orElseThrow();
    }

    private void cadastrarHorario(Parada parada, String horarioPrevisto) throws Exception {
        String horario = """
                {
                  "horarioPrevisto": "%s"
                }
                """.formatted(horarioPrevisto);

        mockMvc.perform(post("/api/paradas/{paradaId}/horarios", parada.getId())
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horario))
                .andExpect(status().isCreated());
    }

    private User criarUsuario(String email, String cpf) {
        User usuario = new User();
        usuario.setNome("Usuario Teste");
        usuario.setCpf(cpf);
        usuario.setDataNascimento(LocalDate.of(2000, 1, 1));
        usuario.setEmail(email);
        usuario.setSenha("senha-teste");
        usuario.setTipoUsuario(TipoUsuario.USER);
        usuario.setConfiabilidade(BigDecimal.ONE);
        return userRepository.save(usuario);
    }

    private Notificacao criarNotificacao(User usuario) {
        return criarNotificacao(usuario, StatusNotificacao.ATIVA);
    }

    private Notificacao criarNotificacao(User usuario, StatusNotificacao status) {
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(usuario);
        notificacao.setTitulo("Aviso de teste");
        notificacao.setMensagem("Mensagem de teste");
        notificacao.setLida(false);
        notificacao.setStatus(status);
        return notificacaoRepository.save(notificacao);
    }

    private ReporteUsuario criarReporte(User usuario, Linha linha, String tipoReporte) throws Exception {
        String reporte = """
                {
                  "usuarioId": %d,
                  "linhaId": %d,
                  "tipoReporte": "%s"
                }
                """.formatted(usuario.getId(), linha.getId(), tipoReporte);

        mockMvc.perform(post("/api/reportes")
                        .with(user(usuario.getEmail()).roles(usuario.getTipoUsuario().name())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reporte))
                .andExpect(status().isCreated());

        return reporteUsuarioRepository.findByUsuarioIdOrderByDataHoraDesc(usuario.getId()).get(0);
    }

    private void favoritar(User usuario, Linha linha) throws Exception {
        mockMvc.perform(post("/api/usuarios/{usuarioId}/linhas-favoritas/{linhaId}", usuario.getId(), linha.getId())
                        .with(user(usuario.getEmail()).roles("USER")).with(csrf()))
                .andExpect(status().isCreated());
    }

    private ResultActions enviarNotificacaoDaLinha(
            Linha linha,
            String titulo,
            String mensagem,
            boolean superarNotificacoesAtuaisDaLinha
    ) throws Exception {
        String notificacao = """
                {
                  "linhaId": %d,
                  "titulo": "%s",
                  "mensagem": "%s",
                  "superarNotificacoesAtuaisDaLinha": %s
                }
                """.formatted(linha.getId(), titulo, mensagem, superarNotificacoesAtuaisDaLinha);

        return mockMvc.perform(post("/api/notificacoes")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificacao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linhaId").value(linha.getId()));
    }

    private ResultActions confirmarReporte(ReporteUsuario reporte, User confirmador) throws Exception {
        return mockMvc.perform(post("/api/reportes/{reporteId}/confirmacoes", reporte.getId())
                .with(user(confirmador.getEmail()).roles("USER")).with(csrf()));
    }

    private ResultActions reportarParada(Parada parada, User usuario, String tipoReporte) throws Exception {
        String reporte = """
                {
                  "paradaId": %d,
                  "tipoReporte": "%s"
                }
                """.formatted(parada.getId(), tipoReporte);

        return mockMvc.perform(post("/api/reportes/minhas/paradas")
                .with(user(usuario.getEmail()).roles("USER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reporte));
    }

    private ReporteUsuario criarReporteEntidade(
            User usuario,
            Linha linha,
            Parada parada,
            TipoReporte tipoReporte,
            LocalDateTime dataHora
    ) {
        ReporteUsuario reporte = new ReporteUsuario();
        reporte.setUsuario(usuario);
        reporte.setLinha(linha);
        reporte.setParada(parada);
        reporte.setTipoReporte(tipoReporte);
        reporte.setDataHora(dataHora);
        return reporteUsuarioRepository.saveAndFlush(reporte);
    }

    private ConfirmacaoReporte criarConfirmacao(ReporteUsuario reporte, User usuario) {
        ConfirmacaoReporte confirmacao = new ConfirmacaoReporte();
        confirmacao.setReporte(reporte);
        confirmacao.setUsuario(usuario);
        confirmacao.setDataHora(LocalDateTime.now());
        return confirmacaoReporteRepository.saveAndFlush(confirmacao);
    }

    private Ocorrencia criarOcorrenciaEntidade(
            Linha linha,
            boolean ativa,
            LocalDateTime dataEncerramento,
            String tipo
    ) {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setLinha(linha);
        ocorrencia.setTipo(tipo);
        ocorrencia.setDescricao("Ocorrencia criada para teste de limpeza.");
        ocorrencia.setPrioridade(Prioridade.MEDIA);
        ocorrencia.setAtiva(ativa);
        ocorrencia.setDataHora(LocalDateTime.now().minusHours(3));
        ocorrencia.setDataEncerramento(dataEncerramento);
        return ocorrenciaRepository.saveAndFlush(ocorrencia);
    }

    private void criarOcorrencia(Linha linha, String tipo, String descricao, String prioridade) throws Exception {
        String ocorrencia = """
                {
                  "linhaId": %d,
                  "tipo": "%s",
                  "descricao": "%s",
                  "prioridade": "%s",
                  "ativa": true
                }
                """.formatted(linha.getId(), tipo, descricao, prioridade);

        mockMvc.perform(post("/api/ocorrencias")
                        .with(user("admin@teste.com").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ocorrencia))
                .andExpect(status().isCreated());
    }
}

