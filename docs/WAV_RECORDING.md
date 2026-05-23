# 前端标准 WAV 录音

本 PR 将浏览器录音从默认 WebM/Opus 改为标准 WAV，用于兼容 Vosk 离线识别。

## 为什么需要修改

浏览器 `MediaRecorder` 默认输出通常是 WebM/Opus。Java 标准音频库和 Vosk 离线识别流程无法稳定直接处理该格式，容易出现：

```text
UnsupportedAudioFileException: File of unsupported format
```

## 实现方式

- 使用 Web Audio API 获取麦克风 PCM 数据。
- 将音频重采样为 16kHz。
- 将 Float32 音频样本转换为 16bit PCM。
- 在前端写入 WAV 文件头。
- 上传 `audio/wav` Blob 到后端。

## 输出格式

```text
采样率：16000 Hz
位深：16 bit
声道：单声道
编码：PCM little-endian
容器：WAV
```

## 验证方式

1. 前端录音并上传。
2. 后端保存文件扩展名为 `.wav`。
3. 调用 Vosk 离线识别时不再出现格式不支持错误。
