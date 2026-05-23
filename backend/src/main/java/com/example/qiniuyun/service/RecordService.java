package com.example.qiniuyun.service;

import com.example.qiniuyun.mapper.VoiceRecordMapper;
import com.example.qiniuyun.model.entity.VoiceRecord;
import com.example.qiniuyun.model.vo.RecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final VoiceRecordMapper voiceRecordMapper;

    @Transactional
    public Long createRecord(VoiceRecord record) {
        voiceRecordMapper.insert(record);
        return record.getId();
    }

    @Transactional
    public void updateResult(Long id, String text, double confidence) {
        VoiceRecord record = new VoiceRecord();
        record.setId(id);
        record.setOriginalText(text);
        record.setConfidence(BigDecimal.valueOf(confidence));
        record.setStatus(1);
        voiceRecordMapper.updateById(record);
    }

    @Transactional
    public void updateError(Long id, String errorMsg) {
        VoiceRecord record = new VoiceRecord();
        record.setId(id);
        record.setStatus(2);
        record.setErrorMsg(errorMsg);
        voiceRecordMapper.updateById(record);
    }

    @Transactional
    public void updateEditedText(Long id, String editedText) {
        VoiceRecord record = new VoiceRecord();
        record.setId(id);
        record.setEditedText(editedText);
        voiceRecordMapper.updateById(record);
    }

    public RecordVO getRecordVO(Long id) {
        VoiceRecord record = voiceRecordMapper.selectById(id);
        return record == null ? null : toVO(record);
    }

    public List<RecordVO> getRecordPage(int page, int size) {
        int offset = Math.max(page - 1, 0) * size;
        return voiceRecordMapper.selectPage(offset, size)
                .stream()
                .map(this::toVO)
                .toList();
    }

    public long getRecordCount() {
        return voiceRecordMapper.selectCount();
    }

    @Transactional
    public boolean deleteRecord(Long id) {
        return voiceRecordMapper.deleteById(id) > 0;
    }

    private RecordVO toVO(VoiceRecord record) {
        RecordVO vo = new RecordVO();
        vo.setId(record.getId());
        vo.setOriginalText(record.getOriginalText());
        vo.setEditedText(record.getEditedText());
        vo.setEngineType(record.getEngineType());
        vo.setDuration(record.getAudioDuration());
        vo.setConfidence(record.getConfidence());
        vo.setStatus(record.getStatus());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
