package com.example.qiniuyun.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecordPageVO {
    private long total;
    private int page;
    private int size;
    private List<RecordVO> records;
}
