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

        try (
                AudioInputStream pcmStream = openPcm16k16bitMonoStream(audioFile);
                Recognizer recognizer = new Recognizer(model, voskConfig.getSampleRate())
        ) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;
            long sampleCount = 0;
            double squareSum = 0;
            int maxAbs = 0;
            StringBuilder textBuilder = new StringBuilder();

            while ((bytesRead = pcmStream.read(buffer)) != -1) {
                if (bytesRead == 0) {
                    continue;
                }
                totalBytes += bytesRead;
                for (int i = 0; i + 1 < bytesRead; i += 2) {
                    int sample = (short) ((buffer[i] & 0xff) | (buffer[i + 1] << 8));
                    int abs = Math.abs(sample);
                    maxAbs = Math.max(maxAbs, abs);
                    squareSum += (double) sample * sample;
                    sampleCount++;
                }
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    appendRecognizedText(textBuilder, recognizer.getResult());
                }
            }

            appendRecognizedText(textBuilder, recognizer.getFinalResult());
            String text = textBuilder.toString().trim();
            double duration = sampleCount / voskConfig.getSampleRate();
            double rms = sampleCount == 0 ? 0 : Math.sqrt(squareSum / sampleCount);
            log.info("Vosk识别完成: format={}, bytes={}, duration={}s, maxAbs={}, rms={}, text={}",
                    audioFormat, totalBytes, String.format("%.2f", duration), maxAbs, String.format("%.2f", rms), text);
            return new AsrResult(text, text.isBlank() ? 0 : 85.0);
        } catch (Exception e) {
            log.error("Vosk识别失败: {}", e.getMessage(), e);
            return new AsrResult("", 0);
        }
    }

    private AudioInputStream openPcm16k16bitMonoStream(File audioFile) throws Exception {
        AudioInputStream originalStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat targetFormat = new AudioFormat(voskConfig.getSampleRate(), 16, 1, true, false);
        return AudioSystem.getAudioInputStream(targetFormat, originalStream);
    }

    private void appendRecognizedText(StringBuilder textBuilder, String resultJson) {
        JsonObject json = JsonParser.parseString(resultJson).getAsJsonObject();
        String text = json.has("text") ? json.get("text").getAsString().trim() : "";
        if (text.isBlank()) {
            return;
        }
        if (!textBuilder.isEmpty()) {
            textBuilder.append(' ');
        }
        textBuilder.append(text);
    }

    @Override
    public String getEngineType() {
        return "vosk";
    }
}
