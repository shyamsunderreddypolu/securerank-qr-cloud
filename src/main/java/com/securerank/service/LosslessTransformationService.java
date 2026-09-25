package com.securerank.service;

import com.securerank.dto.response.AlgorithmMetricResult;
import com.securerank.dto.response.BenchmarkReportResponse;

public interface LosslessTransformationService {

    double calculateShannonEntropy(String bitStream);

    AlgorithmMetricResult evaluateRLE(String bitStream);

    AlgorithmMetricResult evaluateHuffman(String bitStream);

    AlgorithmMetricResult evaluateLZW(String bitStream);

    AlgorithmMetricResult evaluateBinaryToInteger(String bitStream);

    AlgorithmMetricResult evaluateBase64(String bitStream);

    AlgorithmMetricResult evaluateBwtMtfHuffman(String bitStream);

    BenchmarkReportResponse runFullBenchmark(Long fileId, String filename, String bitStream);
}
