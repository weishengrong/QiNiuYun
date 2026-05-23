package com.example.qiniuyun.websocket;

import com.example.qiniuyun.config.StepFunAsrConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class StepFunAsrEntryHandler extends TextWebSocketHandler {

    private final StepFunAsrConfig config;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("StepFun实时识别入口连接建立: {}", session.getId());
        if (!StringUtils.hasText(config.getToken())) {
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"StepFun API Token 未配置，请设置 STEPFUN_API_TOKEN\"}"));
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        session.sendMessage(new TextMessage("{\"type\":\"ready\",\"message\":\"StepFun实时识别入口已就绪，完整代理将在后续PR实现\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        session.sendMessage(new TextMessage("{\"type\":\"info\",\"message\":\"已收到消息，音频转发将在后续PR实现\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("StepFun实时识别入口连接关闭: {}, status={}", session.getId(), status);
    }
}
