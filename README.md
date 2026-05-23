# QiNiuYun 语音输入法

这是语音输入法项目的新仓库，用于按 Pull Request 粒度逐步实现功能。

## PR 1 功能范围

本阶段只初始化前后端基础工程：

- 后端：Spring Boot 基础应用和健康检查接口
- 前端：Vue 3 + Vite + TypeScript 基础页面
- 不接入数据库
- 不接入 ASR 模型
- 不实现录音上传

## 启动方式

### 后端

```powershell
cd backend
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
npm run build

cd ..\backend
mvn test
```
