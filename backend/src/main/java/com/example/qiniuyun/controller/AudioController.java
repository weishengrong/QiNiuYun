package com.example.qiniuyun.controller;

import com.example.qiniuyun.model.dto.AsrResponse;
import com.example.qiniuyun.model.entity.VoiceRecord;
import com.example.qiniuyun.model.vo.ApiResult;
import com.example.qiniuyun.model.vo.AudioUploadVO;
import com.example.qiniuyun.service.AsrService;
import com.example.qiniuyun.service.AudioService;
import com.example.qiniuyun.service.RecordService;
import com.example.qiniuyun.service.asr.AsrEngine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;
    private final RecordService recordService;
    private final AsrService asrService;

    @PostMapping("/audio/upload")
    public ApiResult<AudioUploadVO> upload(
            @RequestParam("audio") MultipartFile audioFile,
            HttpServletRequest request) {
        if (audioFile.isEmpty()) {
            return ApiResult.error(40001, "音频文件为空");
        }
        if (!audioService.isAudioFile(audioFile)) {
            return ApiResult.error(40002, "音频格式不支持: " + audioFile.getContentType());
        }

        try {
            String originalName = audioFile.getOriginalFilename();
            String audioFormat = audioService.getAudioFormat(originalName);

            VoiceRecord record = createPendingRecord(audioFile, audioFormat, request);
            Long recordId = recordService.createRecord(record);
            File savedFile = audioService.saveAudio(audioFile);
            log.info("录音上传完成: recordId={}, file={}", recordId, savedFile.getName());

            return ApiResult.success(new AudioUploadVO(
                    recordId,
                    savedFile.getName(),
                    audioFile.getSize(),
                    audioFormat,
                    "uploaded"
            ));
        } catch (Exception e) {
            log.error("录音上传失败", e);
            return ApiResult.error(50003, "音频保存失败: " + e.getMessage());
        }
    }

    @PostMapping("/asr/recognize")
    public ApiResult<AsrResponse> recognize(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "engine", required = false) String engine,
            HttpServletRequest request) {
        if (audioFile.isEmpty()) {
            return ApiResult.error(40001, "音频文件为空");
        }
        if (!audioService.isAudioFile(audioFile)) {
            return ApiResult.error(40002, "音频格式不支持: " + audioFile.getContentType());
        }

        Long recordId = null;
        try {
            String originalName = audioFile.getOriginalFilename();
            String audioFormat = audioService.getAudioFormat(originalName);
            VoiceRecord record = createPendingRecord(audioFile, audioFormat, request);
            record.setEngineType(engine == null || engine.isBlank() ? "vosk" : engine);
            recordId = recordService.createRecord(record);

            byte[] audioBytes = audioFile.getBytes();
            AsrEngine.AsrResult result = asrService.recognize(audioBytes, audioFormat, engine);
            if (result.text() == null || result.text().isBlank()) {
                recordService.updateError(recordId, "识别结果为空");
                return ApiResult.error(50001, "语音识别失败，请检查音频质量或模型配置");
            }

            audioService.saveAudio(audioFile);
            recordService.updateResult(recordId, result.text(), result.confidence());

            AsrResponse response = new AsrResponse();
            response.setRecordId(recordId);
            response.setOriginalText(result.text());
            response.setEngineType("vosk");
            response.setDuration(0);
            response.setConfidence(result.confidence());
            return ApiResult.success(response);
        } catch (IOException e) {
            log.error("音频数据读取失败", e);
            if (recordId != null) {
                recordService.updateError(recordId, e.getMessage());
            }
            return ApiResult.error(50002, "音频数据读取失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("语音识别处理失败", e);
            if (recordId != null) {
                recordService.updateError(recordId, e.getMessage());
            }
            return ApiResult.error(50002, "语音识别服务异常: " + e.getMessage());
        }
    }

    private VoiceRecord createPendingRecord(MultipartFile audioFile, String audioFormat, HttpServletRequest request) {
        VoiceRecord record = new VoiceRecord();
        record.setAudioName(audioFile.getOriginalFilename());
        record.setAudioSize(audioFile.getSize());
        record.setAudioFormat(audioFormat);
        record.setEngineType("pending");
        record.setStatus(0);
        record.setClientIp(request.getRemoteAddr());
        return record;
    }
}
