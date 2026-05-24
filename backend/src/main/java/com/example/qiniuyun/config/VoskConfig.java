package com.example.qiniuyun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "vosk")
public class VoskConfig {
    private String modelPath;
    private float sampleRate = 16000f;
}
