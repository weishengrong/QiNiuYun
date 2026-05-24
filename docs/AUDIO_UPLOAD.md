# 音频上传接口

本 PR 新增录音文件上传与本地保存能力，暂不接入 ASR 识别。

## 上传录音

```text
POST /api/audio/upload
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| audio | File | 是 | 音频文件，Content-Type 需要以 `audio/` 开头 |

## 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": 1,
    "audioName": "0f3b4e6f.wav",
    "audioSize": 12345,
    "audioFormat": "wav",
    "status": "uploaded"
  }
}
```

## 行为说明

- 后端会把音频保存到 `audio.upload-dir` 配置目录。
- 上传成功后会创建一条 `voice_record` 记录。
- 当前记录状态为 `0=处理中`，后续 PR 会接入 ASR 后更新为成功或失败。
