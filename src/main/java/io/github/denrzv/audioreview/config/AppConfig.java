package io.github.denrzv.audioreview.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Slf4j
@Configuration
public class AppConfig {
    @Value("${app.file-server-url}")
    private String fileServerUrl;
    @Value("${app.user-lock-minutes}")
    private String userLockMinutes;
    @Value("${app.allowed-origin}")
    private String allowedOrigin;
}
