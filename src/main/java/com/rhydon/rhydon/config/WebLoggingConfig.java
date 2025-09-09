package com.rhydon.rhydon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class WebLoggingConfig {

  @Bean
  public CommonsRequestLoggingFilter requestLoggingFilter() {
    CommonsRequestLoggingFilter f = new CommonsRequestLoggingFilter();
    f.setIncludeClientInfo(true);   
    f.setIncludeQueryString(true);  
    f.setIncludeHeaders(false);     
    f.setIncludePayload(false);     
    f.setBeforeMessagePrefix(">> ");
    f.setAfterMessagePrefix("<< ");
    return f;
  }
}
