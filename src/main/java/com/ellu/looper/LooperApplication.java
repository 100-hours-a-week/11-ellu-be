package com.ellu.looper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LooperApplication {

  public static void main(String[] args) {
    SpringApplication.run(LooperApplication.class, args);
  }
}
