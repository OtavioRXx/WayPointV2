package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.dto.CadastroUsuarioRequest;
import com.waypoint.project_tcc_app.model.TipoUsuario;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserService userService;
    private final String googleMapsApiKey;
    private final String googleMapsMapId;

    public UserController(
            UserService userService,
            @Value("${waypoint.google-maps.api-key:}") String googleMapsApiKey,
            @Value("${waypoint.google-maps.map-id:DEMO_MAP_ID}") String googleMapsMapId
    ) {
        this.userService = userService;
        this.googleMapsApiKey = googleMapsApiKey;
        this.googleMapsMapId = googleMapsMapId;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String login() {
        return "index";
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication) {
        adicionarContextoPublicoOuAutenticado(model, authentication);
        return "home";
    }

    @GetMapping("/linhas")
    public String linhas(Model model, Authentication authentication) {
        adicionarContextoPublicoOuAutenticado(model, authentication);
        return "user/linhas";
    }

    @GetMapping("/paradas")
    public String paradas(Model model, Authentication authentication) {
        adicionarContextoPublicoOuAutenticado(model, authentication);
        return "user/paradas";
    }

    @GetMapping("/ocorrencias")
    public String ocorrencias(Model model, Authentication authentication) {
        adicionarContextoPublicoOuAutenticado(model, authentication);
        return "user/ocorrencias";
    }

    @GetMapping("/reportes")
    public String reportes(Model model, Authentication authentication) {
        adicionarContextoPublicoOuAutenticado(model, authentication);
        return "user/reportes";
    }

    @GetMapping("/notificacoes")
    public String notificacoes(Model model, Authentication authentication) {
        adicionarContextoPublicoOuAutenticado(model, authentication);
        return "user/notificacoes";
    }

    @GetMapping("/painel-testes")
    public String painelTestes(Model model, Authentication authentication) {
        adicionarDadosUsuario(model, authentication);
        return "painel-testes";
    }

    private void adicionarContextoPublicoOuAutenticado(Model model, Authentication authentication) {
        if (!estaAutenticado(authentication)) {
            adicionarDadosVisitante(model);
            return;
        }

        try {
            adicionarDadosUsuario(model, authentication);
        } catch (UsernameNotFoundException exception) {
            adicionarDadosUsuarioAutenticado(model, authentication);
        }
    }

    private boolean estaAutenticado(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private void adicionarDadosVisitante(Model model) {
        model.addAttribute("usuarioId", null);
        model.addAttribute("usuarioNome", "Visitante");
        model.addAttribute("usuarioEmail", "");
        model.addAttribute("usuarioTipo", "VISITANTE");
        model.addAttribute("admin", false);
        model.addAttribute("autenticado", false);
        model.addAttribute("visitante", true);
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("googleMapsMapId", googleMapsMapId);
    }

    private void adicionarDadosUsuario(Model model, Authentication authentication) {
        User usuario = userService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario autenticado nao encontrado."));

        model.addAttribute("usuarioId", usuario.getId());
        model.addAttribute("usuarioNome", usuario.getNome());
        model.addAttribute("usuarioEmail", usuario.getEmail());
        model.addAttribute("usuarioTipo", usuario.getTipoUsuario().name());
        model.addAttribute("admin", usuario.getTipoUsuario() == TipoUsuario.ADMIN);
        model.addAttribute("autenticado", true);
        model.addAttribute("visitante", false);
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("googleMapsMapId", googleMapsMapId);
    }

    private void adicionarDadosUsuarioAutenticado(Model model, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(autoridade -> "ROLE_ADMIN".equals(autoridade.getAuthority()));

        model.addAttribute("usuarioId", null);
        model.addAttribute("usuarioNome", authentication.getName());
        model.addAttribute("usuarioEmail", authentication.getName());
        model.addAttribute("usuarioTipo", admin ? "ADMIN" : "USER");
        model.addAttribute("admin", admin);
        model.addAttribute("autenticado", true);
        model.addAttribute("visitante", false);
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("googleMapsMapId", googleMapsMapId);
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("cadastroUsuarioRequest", new CadastroUsuarioRequest());
        return "cadastro";
    }

    @PostMapping("/cadastrar")
    public String cadastrar(
            @Valid CadastroUsuarioRequest cadastroUsuarioRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("erroCadastro", "Verifique os dados informados e tente novamente.");
            return "cadastro";
        }

        try {
            userService.cadastrar(cadastroUsuarioRequest);
            redirectAttributes.addFlashAttribute(
                    "sucessoCadastro",
                    "Cadastro realizado com sucesso. Agora voce ja pode fazer login."
            );
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("erroCadastro", mensagemCadastro(exception));
            return "cadastro";
        }
    }

    private String mensagemCadastro(IllegalArgumentException exception) {
        String mensagem = exception.getMessage();
        if (mensagem != null && mensagem.toLowerCase().contains("senha")) {
            return mensagem;
        }

        return "Nao foi possivel concluir o cadastro com os dados informados.";
    }
}
