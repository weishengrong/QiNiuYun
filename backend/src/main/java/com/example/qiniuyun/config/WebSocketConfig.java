package com.example.qiniuyun.config;

import com.example.qiniuyun.websocket.StepFunAsrEntryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final StepFunAsrEntryHandler stepFunAsrEntryHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(stepFunAsrEntryHandler, "/ws/asr/stepfun")
                .setAllowedOriginPatterns("*");
    }
}
