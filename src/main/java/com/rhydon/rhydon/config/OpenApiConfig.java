package com.rhydon.rhydon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI apiInfo() {
    return new OpenAPI().info(new Info()
        .title("Rhydon API")
        .version("v1")
        .description("Backend em Spring Boot para cadastro de usuários"));
  }

  @Bean
  public OpenApiCustomizer defaultErrors() {
    return openApi -> {
      if (openApi.getPaths() == null) return;

      openApi.getPaths().values().forEach(item -> {
        if (item == null) return;

        item.readOperations().forEach(op -> {
          ApiResponses responses = op.getResponses();
          if (responses == null) {
            responses = new ApiResponses();
            op.setResponses(responses);
          }
          // adiciona somente se ainda não existir
          responses.addApiResponse("400",
              responses.get("400") != null ? responses.get("400")
                                           : new ApiResponse().description("Bad Request"));
          responses.addApiResponse("404",
              responses.get("404") != null ? responses.get("404")
                                           : new ApiResponse().description("Not Found"));
          responses.addApiResponse("409",
              responses.get("409") != null ? responses.get("409")
                                           : new ApiResponse().description("Conflict"));
        });
      });
    };
  }
}
