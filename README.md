# QiNiuYun 语音输入法

QiNiuYun 是一个基于 Web 的语音输入法产品，目标是帮助用户提升文本输入效率。

当前项目采用两条识别路径：

- StepFun 实时识别：主路径，支持边说边出字。
- Vosk 离线识别：回退路径，在云端服务不可用时提供基础识别能力。

项目不在前端保存任何模型密钥。StepFun Token 通过后端环境变量 `STEPFUN_API_TOKEN` 注入，由后端代理连接云端实时 ASR。

## 当前能力

- 前后端基础工程
- 语音识别历史记录存储
- 录音文件上传与本地保存
- 前端浏览器录音
- 前端标准 WAV 录音输出
- Vosk 离线识别
- StepFun 后端 WebSocket 代理
- StepFun 前端实时语音输入
- 实时识别状态与错误提示

## 启动方式

### 后端

```powershell
cd backend
$env:STEPFUN_API_TOKEN="你的 StepFun Token"
mvn spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/api/health
```

### 前端

```powershell
cd frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

## 验证方式

```powershell
cd frontend
npm.cmd run build

cd ..\backend
mvn test
```

## 识别模式

### StepFun 实时

适合日常输入场景。前端采集 16k PCM 音频分片，通过后端 WebSocket 代理发送给 StepFun，返回 delta 增量文本和 completed 最终文本。

### Vosk 离线

适合离线回退场景。前端生成 16kHz / 16bit / 单声道 WAV，后端上传后调用本地 Vosk 模型识别。
