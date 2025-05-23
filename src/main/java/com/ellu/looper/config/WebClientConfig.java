package com.ellu.looper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Value("${fastapi.base-url}")
  private String fastApiBaseUrl;

  @Bean(name = "fastApiWebClient")
  public WebClient fastApiWebClient() {
    return WebClient.builder().baseUrl(fastApiBaseUrl).build();
  }
}
