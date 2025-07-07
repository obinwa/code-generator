package com.idea2code.code_generator.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UploadState {
    private String bucket;
    private String fileKey;
    private String uploadId;
    public int partCounter;
    public Map<Integer, CompletedPart> completedParts = new HashMap<>();
    private int buffered;

    public UploadState(String bucket, String fileKey){
        this.bucket = bucket;
        this.fileKey = fileKey;
    }
}
