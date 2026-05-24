package com.example.qiniuyun.service;

import com.example.qiniuyun.service.asr.AsrEngine;
import com.example.qiniuyun.service.asr.VoskAsrEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsrService {

    private final VoskAsrEngine voskAsrEngine;

    @Value("${asr.default-engine:vosk}")
    private String defaultEngine;

    public AsrEngine.AsrResult recognize(File audioFile, String audioFormat, String engineType) {
        AsrEngine engine = getEngine(engineType);
        if (engine == null) {
            log.warn("指定引擎 {} 不可用，回退到默认引擎 {}", engineType, defaultEngine);
            engine = getEngine(defaultEngine);
        }
        if (engine == null) {
            engine = voskAsrEngine;
        }

        log.info("使用ASR引擎: {} 识别音频: {}", engine.getEngineType(), audioFile.getName());
        return engine.recognize(audioFile, audioFormat);
    }

    public AsrEngine.AsrResult recognize(byte[] audioData, String audioFormat, String engineType) {
        AsrEngine engine = getEngine(engineType);
        if (engine == null) {
            log.warn("指定引擎 {} 不可用，回退到默认引擎 {}", engineType, defaultEngine);
            engine = getEngine(defaultEngine);
        }
        if (engine == null) {
            engine = voskAsrEngine;
        }

        log.info("使用ASR引擎: {} 识别音频数据: {} bytes", engine.getEngineType(), audioData.length);
        return engine.recognize(audioData, audioFormat);
    }

    private AsrEngine getEngine(String engineType) {
        String type = engineType == null || engineType.isBlank() ? defaultEngine : engineType;
        return "vosk".equals(type) ? voskAsrEngine : null;
    }
}
