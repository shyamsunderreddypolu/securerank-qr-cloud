package com.securerank.service;

import com.securerank.dto.response.ApiResponse;
import com.securerank.dto.response.FileMetadataResponse;
import com.securerank.dto.response.KeyRequestResponse;
import com.securerank.dto.response.SearchResultResponse;
import com.securerank.entity.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    ApiResponse uploadFile(MultipartFile file, String label, String keywords, String ownerEmail);

    List<FileMetadataResponse> getMyFiles(String ownerEmail);

    List<SearchResultResponse> searchFiles(String query, String consumerEmail);

    ApiResponse requestFileKey(Long fileId, String consumerEmail, String accessReason);

    List<KeyRequestResponse> getMyKeyRequests(String consumerEmail);

    byte[] downloadAndDecryptFile(Long fileId, String userEmail);

    UploadedFile getFileById(Long fileId);

    com.securerank.dto.response.QRVisualCryptoResponse getQRVisualCryptoDetails(Long fileId);

    com.securerank.dto.response.BenchmarkReportResponse getLosslessBenchmark(Long fileId);
}
