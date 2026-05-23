package com.example.qiniuyun.service.asr;

import com.example.qiniuyun.config.VoskConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;
import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoskAsrEngine implements AsrEngine {

    private final VoskConfig voskConfig;
    private Model model;

    @PostConstruct
    public void init() {
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            File modelDir = new File(voskConfig.getModelPath());
            if (!modelDir.exists()) {
                log.warn("Vosk模型目录不存在: {}，离线识别暂不可用", voskConfig.getModelPath());
                return;
            }
            model = new Model(voskConfig.getModelPath());
            log.info("Vosk模型加载成功: {}", voskConfig.getModelPath());
        } catch (Exception e) {
            log.error("Vosk模型加载失败: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (model != null) {
            model.close();
        }
    }

    @Override
    public AsrResult recognize(File audioFile, String audioFormat) {
        if (model == null) {
            log.warn("Vosk模型未加载，无法执行离线识别");
            return new AsrResult("", 0);
        }

        try {
            byte[] pcmData = convertToPcm16k16bitMono(audioFile);
            if (pcmData.length == 0) {
                return new AsrResult("", 0);
            }

            try (Recognizer recognizer = new Recognizer(model, voskConfig.getSampleRate())) {
                recognizer.acceptWaveForm(pcmData, pcmData.length);
                String result = recognizer.getFinalResult();
                JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                String text = json.has("text") ? json.get("text").getAsString() : "";
                log.info("Vosk识别完成: format={}, text={}", audioFormat, text);
                return new AsrResult(text, text.isBlank() ? 0 : 85.0);
            }
        } catch (Exception e) {
            log.error("Vosk识别失败: {}", e.getMessage(), e);
            return new AsrResult("", 0);
        }
    }

    private byte[] convertToPcm16k16bitMono(File audioFile) throws Exception {
        try (AudioInputStream originalStream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat targetFormat = new AudioFormat(16000f, 16, 1, true, false);
            try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream)) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = convertedStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                return output.toByteArray();
            }
        }
    }

    @Override
    public String getEngineType() {
        return "vosk";
    }
}
