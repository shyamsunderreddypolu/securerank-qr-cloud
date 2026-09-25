package com.securerank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlgorithmMetricResult {
    private String algorithmName;
    private String algorithmCategory; // "Traditional Compression", "Transformation", "Hybrid Pipeline"
    private int originalSizeChars;
    private int compressedSizeChars;
    private double compressionRatioPercent; // (compressed / original) * 100
    private double spaceSavingsPercent; // 100 - compressionRatio
    private double compressionTimeMs;
    private double decompressionTimeMs;
    private double memoryUtilizedKb;
    private boolean losslessFidelity;
    private String sampleCompressedOutput;
    private String notes;
}
