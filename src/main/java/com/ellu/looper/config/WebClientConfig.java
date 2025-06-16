package com.ellu.looper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Value("${fastapi.summary-url}")
  private String fastApiSummaryBaseUrl;

  @Value("${fastapi.chatbot-url}")
  private String fastApiChatbotBaseUrl;

  @Bean(name = "fastApiSummaryWebClient")
  public WebClient fastApiSummaryWebClient() {
    return WebClient.builder().baseUrl(fastApiSummaryBaseUrl).build();
  }

  @Bean(name = "fastApiChatbotWebClient")
  public WebClient fastApiChatbotWebClient() {
    return WebClient.builder().baseUrl(fastApiChatbotBaseUrl).build();
  }
}
