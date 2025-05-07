package com.ellu.external;

import com.ellu.looper.dto.TestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FastApiClient {

  private final WebClient fastApiWebClient;

  public Mono<TestResponse[]> getPosts() {
    return fastApiWebClient.get().uri("/posts").retrieve().bodyToMono(TestResponse[].class);
  }
}
