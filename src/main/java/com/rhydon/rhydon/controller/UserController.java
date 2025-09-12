package com.rhydon.rhydon.controller;

import com.rhydon.rhydon.dto.PageResponse;
import com.rhydon.rhydon.dto.UserCreateRequest;
import com.rhydon.rhydon.dto.UserResponse;
import com.rhydon.rhydon.dto.UserUpdateRequest;
import com.rhydon.rhydon.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "CRUD de usuários (paginado e com ordenação)")
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService service;

  public UserController(UserService service) { this.service = service; }

  @Operation(summary = "Cria um novo usuário")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@RequestBody @Valid UserCreateRequest req) {
    return service.create(req);
  }

  @Operation(summary = "Busca um usuário por ID")
  @GetMapping("/{id}")
  public UserResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @Operation(summary = "Lista usuários paginados e ordenados")
  @Parameters({
      @Parameter(name = "page", description = "Página (0..N)", example = "0"),
      @Parameter(name = "size", description = "Tamanho da página", example = "10"),
      @Parameter(name = "sort", description = "Campo (ex.: fullName, email, createdAt)", example = "createdAt"),
      @Parameter(name = "dir", description = "Direção (asc|desc)", example = "desc")
  })
  @GetMapping
  public PageResponse<UserResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String dir
  ) {
    return service.list(page, size, sort, dir);
  }

  @Operation(summary = "Atualiza parcialmente um usuário")
  @PutMapping("/{id}")
  public UserResponse update(@PathVariable Long id, @RequestBody @Valid UserUpdateRequest req) {
    return service.update(id, req);
  }

  @Operation(summary = "Remove um usuário (idempotente)")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
