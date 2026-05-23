# Vosk 离线语音识别

本 PR 新增 Vosk 离线 ASR 能力，作为语音输入法的本地识别回退方案。

## 配置

```yaml
vosk:
  model-path: ${VOSK_MODEL_PATH:models/vosk-model-small-cn-0.22}
  sample-rate: 16000

asr:
  default-engine: vosk
```

## 识别接口

```text
POST /api/asr/recognize
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| audio | File | 是 | 音频文件，当前建议使用 WAV |
| engine | String | 否 | 识别引擎，当前支持 `vosk` |

## 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": 1,
    "originalText": "今天天气不错",
    "engineType": "vosk",
    "duration": 0,
    "confidence": 85.0
  }
}
```

## 注意

浏览器默认录音通常是 WebM/Opus，Java 标准音频库无法直接解码。当前 PR 先完成 Vosk 引擎接入，后续 PR 会把前端录音改为标准 WAV，以兼容离线识别。
