package com.waypoint.project_tcc_app.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException exception,
            HttpServletRequest request
    ) {
        return criarResposta(HttpStatus.NOT_FOUND, "Recurso nao encontrado.", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> tratarRegraDeNegocio(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return criarResposta(HttpStatus.BAD_REQUEST, "Requisicao invalida.", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TaxaReporteExcedidaException.class)
    public ResponseEntity<ApiErrorResponse> tratarCooldownReporte(
            TaxaReporteExcedidaException exception,
            HttpServletRequest request
    ) {
        return criarResposta(HttpStatus.TOO_MANY_REQUESTS, "Muitos reportes.", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> tratarValidacao(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> campos = new LinkedHashMap<>();

        for (FieldError erro : exception.getBindingResult().getFieldErrors()) {
            campos.put(erro.getField(), erro.getDefaultMessage());
        }

        return criarResposta(
                HttpStatus.BAD_REQUEST,
                "Dados invalidos.",
                "Verifique os campos enviados.",
                request.getRequestURI(),
                campos
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> tratarValidacaoParametro(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> campos = new LinkedHashMap<>();

        exception.getConstraintViolations().forEach(violacao ->
                campos.put(violacao.getPropertyPath().toString(), violacao.getMessage())
        );

        return criarResposta(
                HttpStatus.BAD_REQUEST,
                "Dados invalidos.",
                "Verifique os parametros enviados.",
                request.getRequestURI(),
                campos
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> tratarTipoInvalido(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String mensagem = "O parametro '" + exception.getName() + "' possui um valor invalido.";
        return criarResposta(HttpStatus.BAD_REQUEST, "Parametro invalido.", mensagem, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> tratarJsonInvalido(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return criarResposta(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisicao invalido.",
                "Verifique se o JSON foi enviado corretamente e se os valores informados existem.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> tratarIntegridadeBanco(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return criarResposta(
                HttpStatus.CONFLICT,
                "Conflito de dados.",
                "Nao foi possivel concluir a operacao por conflito com dados ja cadastrados ou relacionados.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> tratarAcessoNegado(AccessDeniedException exception, HttpServletRequest request) {
        return criarResposta(
                HttpStatus.FORBIDDEN,
                "Acesso negado.",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> tratarErroInesperado(Exception exception, HttpServletRequest request) {
        LOGGER.error("evento=ERRO_INTERNO caminho={}", request.getRequestURI(), exception);
        return criarResposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno.",
                "Ocorreu um erro inesperado ao processar a requisicao.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> criarResposta(
            HttpStatus status,
            String erro,
            String mensagem,
            String caminho
    ) {
        return criarResposta(status, erro, mensagem, caminho, null);
    }

    private ResponseEntity<ApiErrorResponse> criarResposta(
            HttpStatus status,
            String erro,
            String mensagem,
            String caminho,
            Map<String, String> campos
    ) {
        ApiErrorResponse resposta = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                erro,
                mensagem,
                caminho,
                campos
        );

        return ResponseEntity.status(status).body(resposta);
    }
}
