package com.ellu.looper.config;

import com.ellu.looper.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProfileImageInitializer {

    private final ProfileImageService profileImageService;

    @Value("${cloud.aws.s3.initialize-profile-images:false}")
    private boolean initializeProfileImages;

    @Bean
    public CommandLineRunner initializeProfileImages() {
        return args -> {
            if (!initializeProfileImages) {
                log.info("Profile images initialization is disabled");
                return;
            }

            try {
                log.info("Initializing profile images...");
                profileImageService.uploadDefaultProfileImages();
                log.info("Profile images initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize profile images", e);
            }
        };
    }
} 