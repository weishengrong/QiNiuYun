package com.example.qiniuyun.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AudioUploadVO {
    private Long recordId;
    private String audioName;
    private Long audioSize;
    private String audioFormat;
    private String status;
}
