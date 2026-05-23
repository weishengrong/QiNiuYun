# 前端 StepFun 实时语音输入

本 PR 在前端接入 StepFun 实时语音输入体验。

## 功能

- 支持选择 `StepFun 实时` 或 `Vosk 离线`。
- StepFun 实时模式下，前端采集麦克风 PCM 分片并通过 WebSocket 发送给后端代理。
- 接收后端返回的 `delta` 增量文本并实时展示。
- 接收 `completed` 最终文本后回填到编辑器。
- Vosk 离线模式继续上传标准 WAV 到后端识别。

## 前端发送给后端的实时音频消息

```json
{
  "type": "audio.append",
  "audio": "Base64编码后的16k PCM分片"
}
```

## 前端消费的后端消息

```json
{ "type": "configured" }
{ "type": "speech_started" }
{ "type": "delta", "text": "今天" }
{ "type": "completed", "text": "今天天气真不错。" }
```
