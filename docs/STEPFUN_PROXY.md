# StepFun 实时 ASR 后端代理

本 PR 将 `/ws/asr/stepfun` 从占位入口升级为完整后端代理。

## 工作流程

1. 浏览器连接后端 `/ws/asr/stepfun`。
2. 后端读取 `STEPFUN_API_TOKEN`。
3. 后端携带 Token 连接 StepFun：

   ```text
   wss://api.stepfun.com/v1/realtime/asr/stream
   ```

4. StepFun 返回 `session.created` 后，后端发送 `session.update`。
5. 浏览器发送音频分片：

   ```json
   {
     "type": "audio.append",
     "audio": "Base64编码后的16k PCM分片"
   }
   ```

6. 后端转发为 StepFun `input_audio_buffer.append`。
7. 后端将 StepFun 的 delta、completed、speech_started、speech_stopped 转成轻量消息返回前端。

## 返回给前端的消息

```json
{ "type": "ready" }
{ "type": "configured" }
{ "type": "speech_started" }
{ "type": "delta", "text": "今天" }
{ "type": "completed", "text": "今天天气真不错。" }
```

## 安全设计

Token 只在后端读取，前端不直接连接 StepFun，也不会暴露密钥。
