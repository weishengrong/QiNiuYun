# StepFun 实时识别 WebSocket 入口

本 PR 新增 StepFun 实时识别的配置和后端 WebSocket 入口，为后续实现完整流式 ASR 代理做准备。

## 配置

```yaml
stepfun:
  asr:
    token: ${STEPFUN_API_TOKEN:}
    stream-url: wss://api.stepfun.com/v1/realtime/asr/stream
    model: step-asr-1.1-stream
    language: zh
    sample-rate: 16000
```

Token 从环境变量读取，避免提交到代码仓库：

```powershell
$env:STEPFUN_API_TOKEN="你的 StepFun Token"
```

## WebSocket 入口

```text
/ws/asr/stepfun
```

当前 PR 只验证入口可连接和 Token 校验。完整音频转发、session.update、delta/completed 事件处理会在后续 PR 实现。

## 验证点

- 未配置 Token 时，连接后返回错误提示。
- 配置 Token 后，连接后返回 ready 消息。
- Vite 已配置 `/ws` 代理，前端后续可通过同源地址连接 WebSocket。
