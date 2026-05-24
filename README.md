# QiNiuYun 语音输入法

基于 Web 的语音输入产品，支持**云端实时识别**与**本地离线识别**双引擎，帮助用户提升文本输入效率。

demo视频链接：https://t.bilibili.com/1206039783284932614?share_source=pc_native

---

## 功能特性

- **StepFun 实时语音输入** — 边说边出字，前端采集 16kHz PCM 音频分片，通过 WebSocket 实时转发到 StepFun ASR 云端服务，返回增量识别文本
- **Vosk 离线识别** — 作为回退路径，在云端服务不可用时提供本地基础识别能力，支持 WAV 文件上传识别
- **历史记录管理** — 自动保存识别记录，支持查看、编辑识别结果、删除记录
- **录音文件上传** — 前端录制 WAV 音频并上传至后端保存
- **实时状态反馈** — 识别过程中实时展示增量文本、VAD 检测状态、错误提示

---

## 技术栈

### 后端

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 4.0.6 |
| 语言 | Java 17 |
| 数据库 | MySQL 8.0+ |
| ORM | MyBatis 4.0.1 |
| WebSocket | Spring WebSocket + java.net.http.WebSocket |
| 离线 ASR | Vosk 0.3.45 |
| 构建工具 | Maven |

### 前端

| 组件 | 技术 |
|------|------|
| 框架 | Vue 3.5 + TypeScript 6.0 |
| 构建工具 | Vite 8 |
| HTTP 请求 | Axios |

### 识别引擎

| 引擎 | 类型 | 模式 | 说明 |
|------|------|------|------|
| StepFun ASR | 云端实时 | WebSocket 流式 | 主路径，支持增量识别 |
| Vosk | 本地离线 | 文件上传 | 回退路径，无需网络 |

---

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+
- Vosk 中文模型文件（离线识别用）

### 数据库初始化

```sql
CREATE DATABASE voice_input DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

表结构见 [docs/DATABASE.md](docs/DATABASE.md)，启动时如配置 `spring.jpa.hibernate.ddl-auto=update` 或手动执行建表 SQL。

### 后端启动

```powershell
cd backend

# 配置环境变量（可选）
$env:STEPFUN_API_TOKEN="你的 StepFun API Token"
$env:MYSQL_URL="jdbc:mysql://localhost:3306/voice_input?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai"
$env:MYSQL_PASSWORD="你的数据库密码"

# 启动
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。

健康检查：

```text
GET http://localhost:8080/api/health
```

### 前端启动

```powershell
cd frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`，开发模式下自动代理 `/api` 和 `/ws` 到后端。

浏览器访问：

```text
http://localhost:5173
```

---

## 环境变量配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `STEPFUN_API_TOKEN` | StepFun API 访问令牌 | — |
| `MYSQL_URL` | MySQL 连接地址 | `jdbc:mysql://localhost:3306/voice_input?...` |
| `MYSQL_PASSWORD` | MySQL 密码 | 空 |
| `AUDIO_UPLOAD_DIR` | 音频文件上传目录 | `./uploads/audio` |

---

## API 接口

### 健康检查

```text
GET /api/health
```

响应示例：

```json
{ "status": "ok", "service": "qiniuyun-backend", "timestamp": "2026-05-24T12:00:00Z" }
```

### 音频上传

```text
POST /api/audio/upload
Content-Type: multipart/form-data

参数: audio (MultipartFile)
```

### 语音识别（离线）

```text
POST /api/asr/recognize
Content-Type: multipart/form-data

参数:
  audio  (MultipartFile)  - 音频文件
  engine (string, 可选)   - 识别引擎，默认 vosk
```

### 识别记录管理

```text
GET    /api/records          - 分页查询记录（page, size）
GET    /api/records/{id}     - 查询单条记录
PUT    /api/records/{id}/text - 更新编辑文本
DELETE /api/records/{id}     - 删除记录
```

### WebSocket 实时识别

```text
WS /ws/asr/stepfun
```

前端通过该 WebSocket 连接进行实时语音识别，协议说明见 [docs/STEPFUN_WS_ENTRY.md](docs/STEPFUN_WS_ENTRY.md)。

---

## 项目结构

```
QiNiuYun/
├── backend/                          # 后端服务
│   ├── src/main/java/com/example/qiniuyun/
│   │   ├── config/                   # 配置类
│   │   │   ├── StepFunAsrConfig.java # StepFun ASR 配置
│   │   │   ├── VoskConfig.java       # Vosk 配置
│   │   │   └── WebSocketConfig.java  # WebSocket 路由配置
│   │   ├── controller/               # REST 控制器
│   │   │   ├── AudioController.java  # 音频上传与识别
│   │   │   ├── RecordController.java # 记录管理
│   │   │   └── HealthController.java # 健康检查
│   │   ├── mapper/                   # MyBatis Mapper
│   │   ├── model/                    # 数据模型
│   │   │   ├── dto/                  # 数据传输对象
│   │   │   ├── entity/               # 实体类
│   │   │   └── vo/                   # 视图对象
│   │   ├── service/                  # 业务逻辑
│   │   │   ├── asr/                  # ASR 引擎实现
│   │   │   │   ├── AsrEngine.java    # 引擎接口
│   │   │   │   └── VoskAsrEngine.java # Vosk 引擎
│   │   │   ├── AsrService.java       # ASR 路由服务
│   │   │   ├── AudioService.java     # 音频文件服务
│   │   │   └── RecordService.java    # 记录服务
│   │   └── websocket/                # WebSocket 处理
│   │       └── StepFunAsrWebSocketHandler.java
│   ├── src/main/resources/
│   │   ├── mapper/                   # MyBatis XML 映射
│   │   └── application.yml           # 应用配置
│   └── pom.xml
├── frontend/                         # 前端应用
│   ├── src/
│   │   ├── api/                      # API 调用封装
│   │   ├── components/               # Vue 组件
│   │   │   ├── VoiceRecorder.vue     # 录音与识别组件
│   │   │   ├── TextEditor.vue        # 文本编辑组件
│   │   │   └── HistoryRecords.vue    # 历史记录组件
│   │   ├── types/                    # TypeScript 类型定义
│   │   ├── App.vue                   # 主页面
│   │   └── main.ts                   # 入口
│   ├── vite.config.ts                # Vite 配置（含代理）
│   └── package.json
├── docs/                             # 设计文档
│   ├── DATABASE.md                   # 数据库设计
│   ├── AUDIO_UPLOAD.md               # 音频上传流程
│   ├── ENGINE_STRATEGY.md            # 引擎策略
│   ├── VOSK_ASR.md                   # Vosk 离线识别
│   ├── STEPFUN_PROXY.md              # StepFun 代理架构
│   ├── STEPFUN_WS_ENTRY.md           # WebSocket 入口
│   ├── FRONTEND_RECORDING.md         # 前端录音
│   ├── FRONTEND_STEPFUN_STREAM.md    # 前端流式识别
│   ├── WAV_RECORDING.md              # WAV 录音格式
│   ├── REALTIME_STATUS.md            # 实时状态
│   └── UI_HISTORY.md                 # 历史记录 UI
└── README.md
```

---

## 识别模式

### StepFun 实时识别（主路径）

适合日常语音输入场景。工作流程：

1. 前端采集 16kHz / 16bit / 单声道 PCM 音频
2. 通过浏览器 WebSocket 发送至后端 `/ws/asr/stepfun`
3. 后端代理转发至 StepFun 云端 `wss://api.stepfun.com/v1/realtime/asr/stream`
4. StepFun 返回增量识别文本（delta），后端实时转发至前端
5. 语音结束（VAD 检测）后返回最终文本（completed）

### Vosk 离线识别（回退路径）

适合网络不稳定或隐私敏感场景。工作流程：

1. 前端录制 16kHz / 16bit / 单声道 WAV 音频
2. 上传至后端 `POST /api/asr/recognize`
3. 后端调用本地 Vosk 模型进行识别
4. 返回识别结果并保存至数据库

---

## 验证方式

```powershell
# 前端构建验证
cd frontend
npm run build

# 后端测试
cd backend
mvn test
```

---

## 安全说明

- StepFun API Token 不存储在前端，通过后端环境变量 `STEPFUN_API_TOKEN` 注入
- 后端 WebSocket 代理模式确保 API 密钥不暴露给客户端
- 识别记录支持用户编辑和删除，保障数据可控

---

## 文档索引

| 文档 | 说明 |
|------|------|
| [数据库设计](docs/DATABASE.md) | 表结构与字段说明 |
| [音频上传流程](docs/AUDIO_UPLOAD.md) | 上传接口与文件处理 |
| [引擎策略](docs/ENGINE_STRATEGY.md) | 双引擎选择与回退策略 |
| [Vosk 离线识别](docs/VOSK_ASR.md) | Vosk 模型配置与调用 |
| [StepFun 代理架构](docs/STEPFUN_PROXY.md) | 后端 WebSocket 代理实现 |
| [WebSocket 入口](docs/STEPFUN_WS_ENTRY.md) | 实时识别协议说明 |
| [前端录音](docs/FRONTEND_RECORDING.md) | 浏览器录音实现 |
| [前端流式识别](docs/FRONTEND_STEPFUN_STREAM.md) | 前端流式处理逻辑 |
| [WAV 录音格式](docs/WAV_RECORDING.md) | WAV 文件格式说明 |
| [实时状态](docs/REALTIME_STATUS.md) | 识别状态与错误处理 |
| [历史记录 UI](docs/UI_HISTORY.md) | 历史记录界面设计 |
