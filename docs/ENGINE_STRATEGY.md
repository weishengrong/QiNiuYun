# 识别引擎策略

本 PR 统一项目的识别引擎说明和配置边界，明确当前项目只保留两类识别路径：

- StepFun 实时流式识别
- Vosk 本地离线识别

## 为什么这样设计

语音输入法需要在响应速度、准确度、成本和可用性之间做平衡。

StepFun 实时流式识别负责主输入体验：

- 支持边说边出字。
- 支持服务端 VAD 断句。
- 支持 completed 最终文本回填。
- 更适合真实输入法场景。

Vosk 离线识别负责兜底：

- 本地运行，不依赖云端服务。
- 成本低。
- 适合网络异常或云端 Token 不可用时保留基础能力。

## 当前引擎配置

### StepFun

```yaml
stepfun:
  asr:
    token: ${STEPFUN_API_TOKEN:}
    stream-url: wss://api.stepfun.com/v1/realtime/asr/stream
    model: step-asr-1.1-stream
    language: zh
```

Token 只从环境变量读取，不写入仓库。

### Vosk

```yaml
vosk:
  model-path: ${VOSK_MODEL_PATH:models/vosk-model-small-cn-0.22}
  sample-rate: 16000

asr:
  default-engine: vosk
```

上传识别默认使用 Vosk，实时输入默认使用 StepFun。

## 清理原则

项目中不再保留无用的第三方 ASR 接入代码，避免：

- 多余依赖增加构建和维护成本。
- 多个云端模型选项干扰用户选择。
- 无用密钥配置造成安全风险。
- 历史实现影响后续调试。

后续如果需要新增模型，应以独立 PR 接入，并保证每个 PR 只新增一种识别能力。

## 验证方式

- 前端只展示 `StepFun 实时` 和 `Vosk 离线` 两个选项。
- `application.yml` 中只保留 StepFun 和 Vosk 配置。
- 后端上传识别默认走 Vosk。
- 实时输入通过 `/ws/asr/stepfun` 连接后端代理。
