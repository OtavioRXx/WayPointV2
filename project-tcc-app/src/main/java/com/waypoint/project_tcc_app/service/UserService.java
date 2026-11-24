package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.dto.CadastroAdminRequest;
import com.waypoint.project_tcc_app.dto.CadastroUsuarioRequest;
import com.waypoint.project_tcc_app.model.TipoUsuario;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.repository.UserRepository;
import com.waypoint.project_tcc_app.security.SenhaPolicyService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SenhaPolicyService senhaPolicyService;
    private final NotificacaoService notificacaoService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SenhaPolicyService senhaPolicyService,
            NotificacaoService notificacaoService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.senhaPolicyService = senhaPolicyService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public User cadastrar(CadastroUsuarioRequest request) {
        User user = new User();
        user.setNome(request.getNome());
        user.setCpf(request.getCpf());
        user.setDataNascimento(request.getDataNascimento());
        user.setEmail(request.getEmail());
        user.setSenha(request.getSenha());
        user.setTipoUsuario(TipoUsuario.USER);
        user.setConfiabilidade(BigDecimal.ONE);

        prepararCadastro(user);
        validarCadastro(user);
        senhaPolicyService.validar(user.getSenha());
        user.setSenha(passwordEncoder.encode(user.getSenha()));
        User salvo = userRepository.save(user);
        notificacaoService.sincronizarAvisosParaUsuario(salvo.getId());
        return salvo;
    }

    @Transactional
    public User cadastrarAdmin(CadastroAdminRequest request) {
        User user = new User();
        user.setNome(request.nome());
        user.setCpf(request.cpf());
        user.setDataNascimento(request.dataNascimento());
        user.setEmail(request.email());
        user.setSenha(request.senha());
        user.setTipoUsuario(TipoUsuario.ADMIN);
        user.setConfiabilidade(BigDecimal.ONE);

        prepararCadastro(user);
        validarCadastro(user);
        senhaPolicyService.validar(user.getSenha());
        user.setSenha(passwordEncoder.encode(user.getSenha()));
        User salvo = userRepository.save(user);
        notificacaoService.sincronizarAvisosParaUsuario(salvo.getId());
        return salvo;
    }

    public User salvar(User user) {
        prepararCadastro(user);
        validarCadastro(user);
        senhaPolicyService.validar(user.getSenha());
        user.setSenha(passwordEncoder.encode(user.getSenha()));
        return userRepository.save(user);
    }

    public List<User> listarTodos() {
        return userRepository.findAll();
    }

    public Optional<User> buscarPorId(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public User atualizar(User user) {
        return userRepository.save(user);
    }

    public void excluir(Long id) {
        userRepository.deleteById(id);
    }

    private void prepararCadastro(User user) {
        if (user.getCpf() != null) {
            user.setCpf(user.getCpf().replaceAll("\\D", ""));
        }

        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }

        if (user.getTipoUsuario() == null) {
            user.setTipoUsuario(TipoUsuario.USER);
        }

        if (user.getConfiabilidade() == null) {
            user.setConfiabilidade(BigDecimal.ONE);
        }
    }

    private void validarCadastro(User user) {
        if (user.getCpf() == null || user.getCpf().length() != 11) {
            throw new IllegalArgumentException("CPF deve conter 11 numeros.");
        }

        if (userRepository.existsByCpf(user.getCpf())) {
            throw new IllegalArgumentException("CPF ja cadastrado.");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email ja cadastrado.");
        }
    }
}
