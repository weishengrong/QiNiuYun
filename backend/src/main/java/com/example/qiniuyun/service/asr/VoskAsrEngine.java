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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoskAsrEngine implements AsrEngine {

    private static final int BUFFER_SIZE = 16384;
    private static final int WAV_HEADER_SIZE = 44;

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
            byte[] fileBytes = readFileBytes(audioFile);
            return recognize(fileBytes, audioFormat);
        } catch (Exception e) {
            log.error("Vosk识别失败: {}", e.getMessage(), e);
            return new AsrResult("", 0);
        }
    }

    @Override
    public AsrResult recognize(byte[] audioData, String audioFormat) {
        if (model == null) {
            log.warn("Vosk模型未加载，无法执行离线识别");
            return new AsrResult("", 0);
        }

        byte[] pcmData = extractPcmFromWav(audioData);
        if (pcmData.length == 0) {
            log.warn("WAV文件中未提取到PCM数据");
            return new AsrResult("", 0);
        }

        try (Recognizer recognizer = new Recognizer(model, voskConfig.getSampleRate())) {
            int offset = 0;
            long totalBytes = 0;
            long sampleCount = 0;
            double squareSum = 0;
            int maxAbs = 0;
            StringBuilder textBuilder = new StringBuilder();

            while (offset < pcmData.length) {
                int chunkSize = Math.min(BUFFER_SIZE, pcmData.length - offset);
                if (chunkSize == 0) break;

                totalBytes += chunkSize;
                for (int i = 0; i + 1 < chunkSize; i += 2) {
                    int sample = (short) ((pcmData[offset + i] & 0xff) | (pcmData[offset + i + 1] << 8));
                    int abs = Math.abs(sample);
                    maxAbs = Math.max(maxAbs, abs);
                    squareSum += (double) sample * sample;
                    sampleCount++;
                }
                if (recognizer.acceptWaveForm(pcmData, offset, chunkSize)) {
                    appendRecognizedText(textBuilder, recognizer.getResult());
                }
                offset += chunkSize;
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

    private byte[] extractPcmFromWav(byte[] wavData) {
        if (wavData == null || wavData.length < WAV_HEADER_SIZE) {
            return new byte[0];
        }
        if (wavData[0] != 'R' || wavData[1] != 'I' || wavData[2] != 'F' || wavData[3] != 'F') {
            return wavData;
        }
        int dataOffset = WAV_HEADER_SIZE;
        for (int i = WAV_HEADER_SIZE - 8; i < wavData.length - 4; i++) {
            if (wavData[i] == 'd' && wavData[i + 1] == 'a' && wavData[i + 2] == 't' && wavData[i + 3] == 'a') {
                dataOffset = i + 8;
                break;
            }
        }
        int pcmLength = wavData.length - dataOffset;
        if (pcmLength <= 0) {
            return new byte[0];
        }
        byte[] pcm = new byte[pcmLength];
        System.arraycopy(wavData, dataOffset, pcm, 0, pcmLength);
        return pcm;
    }

    private byte[] readFileBytes(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int offset = 0;
            int read;
            while ((read = in.read(buf, offset, buf.length - offset)) != -1) {
                offset += read;
            }
            return buf;
        }
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