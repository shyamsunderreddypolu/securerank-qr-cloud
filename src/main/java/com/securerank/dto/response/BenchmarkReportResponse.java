package com.securerank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkReportResponse {
    private Long fileId;
    private String filename;
    private int bitStreamLength;
    private double shannonEntropy;
    private String optimalAlgorithm;
    private String conclusion;
    private List<AlgorithmMetricResult> results;
}
