package com.rhydon.rhydon.controller;

import com.rhydon.rhydon.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  /** Monta o corpo padronizado do erro. */
  private ErrorResponse body(HttpStatus status, String message, String path, Object details) {
    return new ErrorResponse(
        Instant.now().toString(),      // timestamp ISO
        status.value(),                // status code
        status.getReasonPhrase(),      // status text
        message,                       // mensagem principal
        path,                          // path solicitado
        details                        // detalhes opcionais
    );
  }

  /** Bean Validation em @RequestBody (ex.: @NotBlank, @Email em DTO). -> 400 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                        HttpServletRequest req) {
    Map<String, String> fields = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            fe -> fe.getField(),
            DefaultMessageSourceResolvable::getDefaultMessage,
            (a, b) -> a   // em caso de chave repetida, fica o primeiro
        ));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(body(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), fields));
  }

  /** Constraint em parâmetros de path/query (@Validated no controller). -> 400 */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                 HttpServletRequest req) {
    Map<String, String> details = ex.getConstraintViolations()
        .stream()
        .collect(Collectors.toMap(
            v -> v.getPropertyPath().toString(),
            v -> v.getMessage(),
            (a, b) -> a
        ));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(body(HttpStatus.BAD_REQUEST, "Constraint violation", req.getRequestURI(), details));
  }

  /** Recurso não encontrado (usado no service: new NoSuchElementException("User not found")). -> 404 */
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex,
                                                      HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(body(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), null));
  }

  /** Método HTTP não suportado para o endpoint (ex.: PUT ausente). -> 405 */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                              HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(body(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req.getRequestURI(), null));
  }

  /** Violação de integridade (ex.: unique key) vinda do banco. -> 409 */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                           HttpServletRequest req) {
    // Mostra a “root cause” em details (ajuda a depurar unique constraint, FK, etc.)
    String root = Optional.ofNullable(ex.getMostSpecificCause())
        .map(Throwable::getMessage)
        .orElse(ex.getMessage());

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(body(HttpStatus.CONFLICT, "Data integrity violation", req.getRequestURI(), root));
  }

  /** Conflitos de negócio (ex.: e-mail já em uso lançado no service). -> 409 */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                             HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(body(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI(), null));
  }

  /** JSON malformado no corpo da requisição. -> 400 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleBadJson(HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(body(HttpStatus.BAD_REQUEST, "Malformed JSON request", req.getRequestURI(), null));
  }

  /** Fallback genérico (erros não previstos). -> 500 + log com stacktrace */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
    // Log mínimo (somente 500), com stacktrace para facilitar debug agora
    log.error("Unhandled error at {} -> {}", req.getRequestURI(), ex.toString(), ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req.getRequestURI(), null));
  }
}
