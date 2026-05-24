package com.example.qiniuyun.model.dto;

import lombok.Data;

@Data
public class AsrResponse {
    private Long recordId;
    private String originalText;
    private String engineType;
    private double duration;
    private double confidence;
}
