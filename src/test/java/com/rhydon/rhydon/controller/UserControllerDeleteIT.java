package com.rhydon.rhydon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real: cria um usuário, deleta 2x o mesmo id e garante 204 nas duas vezes (idempotente).
 */
@Testcontainers
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = { "spring.jpa.hibernate.ddl-auto=update" } // schema auto nos testes
)
@AutoConfigureMockMvc
class UserControllerDeleteIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void deleteIsIdempotent_returns204EvenIfAlreadyDeleted() throws Exception {
    // 1) cria um usuário e pega o id do JSON de resposta
    var body = """
      { "fullName": "Temp Del", "email": "temp.del@example.com", "password": "123456" }
      """;

    var createResult = mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
      .andExpect(status().isCreated())
      .andReturn();

    var json = createResult.getResponse().getContentAsString();
    long id = objectMapper.readTree(json).get("id").asLong();

    // 2) DELETE 1ª vez -> 204
    mockMvc.perform(delete("/api/users/{id}", id))
      .andExpect(status().isNoContent());

    // 3) DELETE 2ª vez (mesmo id) -> 204 (idempotente)
    mockMvc.perform(delete("/api/users/{id}", id))
      .andExpect(status().isNoContent());
  }
}
