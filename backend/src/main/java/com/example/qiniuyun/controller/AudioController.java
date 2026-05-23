package com.example.qiniuyun.controller;

import com.example.qiniuyun.model.entity.VoiceRecord;
import com.example.qiniuyun.model.vo.ApiResult;
import com.example.qiniuyun.model.vo.AudioUploadVO;
import com.example.qiniuyun.service.AudioService;
import com.example.qiniuyun.service.RecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Slf4j
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;
    private final RecordService recordService;

    @PostMapping("/upload")
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

            VoiceRecord record = new VoiceRecord();
            record.setAudioName(originalName);
            record.setAudioSize(audioFile.getSize());
            record.setAudioFormat(audioFormat);
            record.setEngineType("pending");
            record.setStatus(0);
            record.setClientIp(request.getRemoteAddr());

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
}
