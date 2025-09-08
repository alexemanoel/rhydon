package com.rhydon.rhydon.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = { "spring.jpa.hibernate.ddl-auto=update" }
)
@AutoConfigureMockMvc
class UserControllerListIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  MockMvc mockMvc;

  @BeforeEach
  void seed() throws Exception {
    // Deixe a base sempre no estado conhecido para este teste:
    // como o container é estático por classe, limpamos criando e-mails únicos
    // ou simplesmente recriamos usuários sem depender do estado anterior.
    // Aqui, vamos "garantir" o conjunto criando 3 usuários com e-mails únicos.
    mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"fullName":"Ana","email":"ana.list@example.com","password":"123456"}
        """))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"fullName":"Bruno","email":"bruno.list@example.com","password":"123456"}
        """))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"fullName":"Carla","email":"carla.list@example.com","password":"123456"}
        """))
      .andExpect(status().isCreated());
  }

  @Test
  void listShouldReturnPage0AndPage1_sortedByFullNameAsc() throws Exception {
    // Página 0, size 2, ordenado por nome ASC: Ana, Bruno
    mockMvc.perform(get("/api/users")
        .queryParam("page", "0")
        .queryParam("size", "2")
        .queryParam("sort", "fullName")
        .queryParam("dir", "asc"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page").value(0))
      .andExpect(jsonPath("$.size").value(2))
      .andExpect(jsonPath("$.totalElements").value(3))
      .andExpect(jsonPath("$.totalPages").value(2))
      .andExpect(jsonPath("$.first").value(true))
      .andExpect(jsonPath("$.last").value(false))
      .andExpect(jsonPath("$.sort").value("fullName"))
      .andExpect(jsonPath("$.dir").value("asc"))
      .andExpect(jsonPath("$.content[0].fullName").value("Ana"))
      .andExpect(jsonPath("$.content[1].fullName").value("Bruno"));

    // Página 1, size 2: sobra só "Carla"
    mockMvc.perform(get("/api/users")
        .queryParam("page", "1")
        .queryParam("size", "2")
        .queryParam("sort", "fullName")
        .queryParam("dir", "asc"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page").value(1))
      .andExpect(jsonPath("$.size").value(2))
      .andExpect(jsonPath("$.totalElements").value(3))
      .andExpect(jsonPath("$.totalPages").value(2))
      .andExpect(jsonPath("$.first").value(false))
      .andExpect(jsonPath("$.last").value(true))
      .andExpect(jsonPath("$.content.length()").value(1))
      .andExpect(jsonPath("$.content[0].fullName").value("Carla"));
  }
}
