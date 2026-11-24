package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.model.TipoUsuario;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.repository.UserRepository;
import com.waypoint.project_tcc_app.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CadastroLoginControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void paginaInicialRedirecionaParaHomePublica() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void visitanteAcessaPaginasPublicasSemLoginENaoAcessaPainelAdmin() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Acesso visitante")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entrar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Criar conta")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Painel Admin"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/ocorrencias\""))));

        mockMvc.perform(get("/linhas"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/linhas"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Modo visitante")));

        mockMvc.perform(get("/paradas"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/paradas"));

        mockMvc.perform(get("/reportes"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/reportes"));

        mockMvc.perform(get("/notificacoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/notificacoes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Comunicados da administração")));

        mockMvc.perform(get("/ocorrencias"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/ocorrencias"));

        mockMvc.perform(get("/painel-testes"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void cadastroComCpfFormatadoSalvaSomenteDigitosETelaUsaMascara() throws Exception {
        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-cpf-mask")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Continuar sem login")));

        mockMvc.perform(post("/cadastrar")
                        .with(csrf())
                        .param("nome", "Usuario Cpf Formatado")
                        .param("cpf", "777.888.999-11")
                        .param("dataNascimento", "1998-05-15")
                        .param("email", "cpf.formatado@teste.com")
                        .param("senha", "SenhaValida123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User usuario = userRepository.findByEmail("cpf.formatado@teste.com").orElseThrow();
        assertThat(usuario.getCpf()).isEqualTo("77788899911");
    }

    @Test
    void cadastroSalvaUsuarioComoUserERedirecionaComMensagemDeSucesso() throws Exception {
        mockMvc.perform(post("/cadastrar")
                        .with(csrf())
                        .param("nome", "Usuario Cadastro Teste")
                        .param("cpf", "98765432100")
                        .param("dataNascimento", "2000-01-01")
                        .param("email", "cadastro.web@teste.com")
                        .param("senha", "SenhaValida123")
                        .param("tipoUsuario", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("sucessoCadastro"));

        User usuario = userRepository.findByEmail("cadastro.web@teste.com").orElseThrow();

        assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.USER);
        assertThat(usuario.getSenha()).isNotEqualTo("SenhaValida123");
        assertThat(passwordEncoder.matches("SenhaValida123", usuario.getSenha())).isTrue();
    }

    @Test
    void cadastroInvalidoExibeMensagemDeErroNaTelaDeCadastro() throws Exception {
        mockMvc.perform(post("/cadastrar")
                        .with(csrf())
                        .param("nome", "Usuario Cadastro Invalido")
                        .param("cpf", "123")
                        .param("dataNascimento", "2000-01-01")
                        .param("email", "cadastro.invalido@teste.com")
                        .param("senha", "SenhaValida123"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeExists("erroCadastro"));
    }

    @Test
    void cadastroIgnoraCamposInternosManipuladosNaRequisicao() throws Exception {
        User usuarioExistente = criarUsuario("existente.mass@teste.com", "11223344561", "SenhaValida123");
        usuarioExistente.setConfiabilidade(new BigDecimal("1.00"));
        userRepository.saveAndFlush(usuarioExistente);

        mockMvc.perform(post("/cadastrar")
                        .with(csrf())
                        .param("id", usuarioExistente.getId().toString())
                        .param("nome", "Usuario Manipulado")
                        .param("cpf", "98765432101")
                        .param("dataNascimento", "2000-01-01")
                        .param("email", "manipulado.mass@teste.com")
                        .param("senha", "SenhaValida123")
                        .param("tipoUsuario", "ADMIN")
                        .param("confiabilidade", "2.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User usuarioCriado = userRepository.findByEmail("manipulado.mass@teste.com").orElseThrow();
        User usuarioOriginal = userRepository.findById(usuarioExistente.getId()).orElseThrow();

        assertThat(usuarioCriado.getId()).isNotEqualTo(usuarioExistente.getId());
        assertThat(usuarioCriado.getTipoUsuario()).isEqualTo(TipoUsuario.USER);
        assertThat(usuarioCriado.getConfiabilidade()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(usuarioOriginal.getEmail()).isEqualTo("existente.mass@teste.com");
        assertThat(usuarioOriginal.getTipoUsuario()).isEqualTo(TipoUsuario.USER);
    }

    @Test
    void cadastroRejeitaSenhaFraca() throws Exception {
        mockMvc.perform(post("/cadastrar")
                        .with(csrf())
                        .param("nome", "Usuario Senha Fraca")
                        .param("cpf", "98765432102")
                        .param("dataNascimento", "2000-01-01")
                        .param("email", "senha.fraca@teste.com")
                        .param("senha", "12345678"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeExists("erroCadastro"));

        assertThat(userRepository.findByEmail("senha.fraca@teste.com")).isEmpty();
    }

    @Test
    void loginValidoRedirecionaParaHome() throws Exception {
        criarUsuario("login.valido@teste.com", "11223344556", "SenhaValida123");

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "login.valido@teste.com")
                        .param("password", "SenhaValida123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void loginInvalidoRedirecionaParaLoginComErro() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "nao.existe@teste.com")
                        .param("password", "senha-errada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    void tentativasRepetidasDeLoginBloqueiamTemporariamenteAConta() throws Exception {
        criarUsuario("login.bloqueado@teste.com", "11223344562", "SenhaValida123");

        for (int tentativa = 0; tentativa < 5; tentativa++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", "login.bloqueado@teste.com")
                            .param("password", "senha-incorreta"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?erro"));
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "login.bloqueado@teste.com")
                        .param("password", "SenhaValida123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    void homeAutenticadaExibePainelDeTestes() throws Exception {
        mockMvc.perform(get("/home")
                .with(user("usuario@teste.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Linhas")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Parada")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reporte")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Notifica")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/ocorrencias\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Operação"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Painel Admin"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Area tecnica e documentacao"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sistema colaborativo de transporte")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trabalho de Conclus")));
    }

    @Test
    void arquivosEstaticosDoPainelSaoPublicos() throws Exception {
        mockMvc.perform(get("/css/painel-testes.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".map-canvas")));

        mockMvc.perform(get("/js/modules/mapa.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.WayPointMapa")));

        mockMvc.perform(get("/js/painel-testes.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("document.body.dataset")));

        mockMvc.perform(get("/js/core/api.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.WayPoint")));
    }

    @Test
    void painelTestesAdminRenderizaInterfaceAdministrativa() throws Exception {
        User usuario = criarUsuario("painel.admin@teste.com", "11223344557", "SenhaValida123", TipoUsuario.ADMIN);

        mockMvc.perform(get("/painel-testes")
                .with(user(usuario.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("painel-testes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Painel administrativo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/waypoint.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/modules/mapa.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/painel-testes.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-user-type=\"ADMIN\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Linha ID"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Usuario ID"))));
    }

    @Test
    void painelTestesBloqueiaUserComPagina403Personalizada() throws Exception {
        User usuario = criarUsuario("painel.user@teste.com", "11223344559", "SenhaValida123");

        mockMvc.perform(get("/painel-testes")
                .with(user(usuario.getEmail()).roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));

        mockMvc.perform(get("/error/403")
                .with(user(usuario.getEmail()).roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Acesso não permitido")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Voltar para Home")));
    }

    @Test
    void swaggerFicaProtegidoParaAdmin() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/swagger-ui/index.html")
                        .with(user("usuario@teste.com").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v3/api-docs")
                        .with(user("admin@teste.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void paginasUserSaoRenderizadasSeparadamente() throws Exception {
        User usuario = criarUsuario("paginas.user@teste.com", "11223344558", "SenhaValida123");

        mockMvc.perform(get("/linhas").with(user(usuario.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/linhas"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Selecionar"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Painel Admin"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/ocorrencias\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/modules/linhas.js")));

        mockMvc.perform(get("/paradas").with(user(usuario.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/paradas"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Painel Admin"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/ocorrencias\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/modules/paradas.js")));

        mockMvc.perform(get("/reportes").with(user(usuario.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/reportes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Feed colaborativo")));
    }

    @Test
    void paginasComunsDoAdminUsamMesmaInterfaceDoUser() throws Exception {
        User usuario = criarUsuario("paginas.admin@teste.com", "11223344560", "SenhaValida123", TipoUsuario.ADMIN);

        mockMvc.perform(get("/home").with(user(usuario.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Painel Admin")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Operacao"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/ocorrencias\""))));

        mockMvc.perform(get("/linhas").with(user(usuario.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/linhas"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Painel Admin")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/ocorrencias\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/modules/linhas.js")));

        mockMvc.perform(get("/paradas").with(user(usuario.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/paradas"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Painel Admin")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/ocorrencias\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/modules/paradas.js")));
    }

    private User criarUsuario(String email, String cpf, String senha) {
        return criarUsuario(email, cpf, senha, TipoUsuario.USER);
    }

    private User criarUsuario(String email, String cpf, String senha, TipoUsuario tipoUsuario) {
        User usuario = new User();
        usuario.setNome("Usuario Login Teste");
        usuario.setCpf(cpf);
        usuario.setDataNascimento(LocalDate.of(2000, 1, 1));
        usuario.setEmail(email);
        usuario.setSenha(senha);
        usuario.setTipoUsuario(tipoUsuario);

        return userService.salvar(usuario);
    }
}
