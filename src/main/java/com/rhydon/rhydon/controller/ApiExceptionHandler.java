package com.rhydon.rhydon.controller;

import com.rhydon.rhydon.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

  private ErrorResponse body(HttpStatus status, String message, String path, Object details) {
    return new ErrorResponse(
      Instant.now().toString(),
      status.value(),
      status.getReasonPhrase(),
      message,
      path,
      details
    );
  }

  
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                        HttpServletRequest req) {
    Map<String, String> fields = ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .collect(Collectors.toMap(
        fe -> fe.getField(),
        DefaultMessageSourceResolvable::getDefaultMessage,
        (a, b) -> a 
      ));
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(body(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), fields));
  }

  
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                 HttpServletRequest req) {
    Map<String, String> errors = ex.getConstraintViolations()
      .stream()
      .collect(Collectors.toMap(
        v -> v.getPropertyPath().toString(),
        v -> v.getMessage(),
        (a,b)->a
      ));
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(body(HttpStatus.BAD_REQUEST, "Constraint violation", req.getRequestURI(), errors));
  }

  
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex,
                                                      HttpServletRequest req) {
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(body(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), null));
  }

  
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                           HttpServletRequest req) {
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(body(HttpStatus.CONFLICT, "Data integrity violation", req.getRequestURI(), null));
  }

  
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                             HttpServletRequest req) {
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(body(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI(), null));
  }

  
  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleBadJson(org.springframework.http.converter.HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(body(HttpStatus.BAD_REQUEST, "Malformed JSON request", req.getRequestURI(), null));
  }

  
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req.getRequestURI(), null));
  }
}
