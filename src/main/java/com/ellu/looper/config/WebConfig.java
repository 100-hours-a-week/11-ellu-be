package com.ellu.looper.config;

import com.ellu.looper.commons.CurrentUserArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final CurrentUserArgumentResolver currentUserArgumentResolver;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserArgumentResolver);
  }
}

// 아래처럼 사용 가능
// @RestController
// @RequestMapping("/api/users")
// public class UserController {
//
//    @GetMapping("/me")
//    public ResponseEntity<String> getCurrentUserId(@CurrentUser Long userId) {
//        return ResponseEntity.ok("현재 로그인한 유저 ID: " + userId);
//    }
// }
