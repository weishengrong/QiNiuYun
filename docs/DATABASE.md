# 数据库设计

## voice_record 表

本 PR 新增语音识别历史记录表，用于保存每次识别任务的音频信息、识别文本、状态和错误信息。

```sql
CREATE TABLE voice_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    audio_name VARCHAR(255) NULL COMMENT '原始音频文件名',
    audio_size BIGINT NULL COMMENT '音频文件大小(字节)',
    audio_duration INT NULL COMMENT '音频时长(秒)',
    audio_format VARCHAR(20) NULL COMMENT '音频格式',
    original_text TEXT NULL COMMENT '原始识别文本',
    edited_text TEXT NULL COMMENT '用户编辑后的文本',
    engine_type VARCHAR(20) NULL COMMENT '识别引擎',
    confidence DECIMAL(5,2) NULL COMMENT '识别置信度(0-100)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=处理中, 1=成功, 2=失败',
    error_msg VARCHAR(500) NULL COMMENT '失败原因',
    client_ip VARCHAR(50) NULL COMMENT '客户端IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_created_at (created_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音识别记录表';
```
