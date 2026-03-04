package br.com.felipebrandao.vidya.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------
    // Validação de campos (@Valid)
    // -------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                fields,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(body);
    }

    // -------------------------
    // Regra de negócio
    // -------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                null,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // -------------------------
    // Credenciais inválidas (login)
    // -------------------------
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Usuário ou senha inválidos",
                null,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // -------------------------
    // Usuário não encontrado
    // -------------------------
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                null,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // -------------------------
    // Recurso não encontrado
    // -------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                null,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // -------------------------
    // Erro de integração externa
    // -------------------------
    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegration(
            IntegrationException ex,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                ex.getMessage(),
                null,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    // -------------------------
    // Fallback genérico
    // -------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno no servidor",
                null,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // -------------------------
    // Record de resposta de erro
    // -------------------------
    public record ErrorResponse(
            int status,
            String message,
            Map<String, String> fields,
            String path,
            LocalDateTime timestamp
    ) {
        public ErrorResponse(int status, String message, Map<String, String> fields, String path) {
            this(status, message, fields, path, LocalDateTime.now());
        }
    }
}

