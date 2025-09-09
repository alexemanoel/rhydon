package com.rhydon.rhydon.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = { "spring.jpa.hibernate.ddl-auto=update" } // só nos testes
)
@AutoConfigureMockMvc
class UserControllerUpdateIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  Long targetId;
  String targetEmail;
  String otherEmail;

  @BeforeEach
  void setUp() throws Exception {
    // e-mails únicos para evitar conflitos entre execuções
    String suffix = String.valueOf(System.nanoTime());
    targetEmail = "update_target_" + suffix + "@rhydon.dev";
    otherEmail  = "update_other_"  + suffix + "@rhydon.dev";

    // cria usuário "alvo" (que será atualizado)
    var createTarget = mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"fullName":"Alvo Update","email":"%s","password":"123456"}
        """.formatted(targetEmail)))
      .andExpect(status().isCreated())
      .andReturn();

    JsonNode target = objectMapper.readTree(createTarget.getResponse().getContentAsString());
    targetId = target.get("id").asLong();

    // cria "outro" usuário (para testar conflito de e-mail)
    mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"fullName":"Outro User","email":"%s","password":"123456"}
        """.formatted(otherEmail)))
      .andExpect(status().isCreated());
  }

  @Test
  void updateOnlyFullName_shouldReturn200_andKeepEmail() throws Exception {
    mockMvc.perform(put("/api/users/{id}", targetId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          { "fullName": "Nome Atualizado" }
        """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(targetId))
      .andExpect(jsonPath("$.fullName").value("Nome Atualizado"))
      .andExpect(jsonPath("$.email").value(targetEmail)); // e-mail não muda
  }

  @Test
  void updateEmail_toExisting_shouldReturn409() throws Exception {
    mockMvc.perform(put("/api/users/{id}", targetId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(("""
          { "email": "%s" }
        """).formatted(otherEmail)))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.status").value(409))
      .andExpect(jsonPath("$.error").value("Conflict"));
  }

  @Test
  void updateNonExistingId_shouldReturn404() throws Exception {
    long missingId = 9_999_999L;

    mockMvc.perform(put("/api/users/{id}", missingId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          { "fullName": "Qualquer" }
        """))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.status").value(404))
      .andExpect(jsonPath("$.message").value("User not found"));
  }
}
