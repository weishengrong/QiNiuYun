package com.example.qiniuyun.controller;

import com.example.qiniuyun.model.vo.ApiResult;
import com.example.qiniuyun.model.vo.RecordPageVO;
import com.example.qiniuyun.model.vo.RecordVO;
import com.example.qiniuyun.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping
    public ApiResult<RecordPageVO> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<RecordVO> records = recordService.getRecordPage(page, size);
        long total = recordService.getRecordCount();
        return ApiResult.success(new RecordPageVO(total, page, size, records));
    }

    @GetMapping("/{id}")
    public ApiResult<RecordVO> detail(@PathVariable Long id) {
        RecordVO record = recordService.getRecordVO(id);
        if (record == null) {
            return ApiResult.error(40400, "记录不存在");
        }
        return ApiResult.success(record);
    }

    @PutMapping("/{id}/text")
    public ApiResult<Void> updateText(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String editedText = body.get("editedText");
        if (editedText == null || editedText.isBlank()) {
            return ApiResult.error(40001, "编辑文本不能为空");
        }
        recordService.updateEditedText(id, editedText);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        boolean deleted = recordService.deleteRecord(id);
        if (!deleted) {
            return ApiResult.error(40400, "记录不存在");
        }
        return ApiResult.success();
    }
}
