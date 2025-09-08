package com.rhydon.rhydon.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = { "spring.jpa.hibernate.ddl-auto=update" } // schema auto só nos testes
)
@AutoConfigureMockMvc
class UserControllerNotFoundIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  MockMvc mockMvc;

  @Test
  void getById_inexistente_deveRetornar404_comErroPadronizado() throws Exception {
    long idInexistente = 999_999L;

    mockMvc.perform(get("/api/users/{id}", idInexistente))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.status").value(404))
      .andExpect(jsonPath("$.error").value("Not Found"))
      .andExpect(jsonPath("$.message").value("User not found"))
      .andExpect(jsonPath("$.path").value("/api/users/" + idInexistente))
      .andExpect(jsonPath("$.timestamp").exists());
  }
}
