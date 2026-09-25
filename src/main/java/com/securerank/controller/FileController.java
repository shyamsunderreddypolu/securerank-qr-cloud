package com.securerank.controller;

import com.securerank.dto.response.ApiResponse;
import com.securerank.dto.response.FileMetadataResponse;
import com.securerank.dto.response.KeyRequestResponse;
import com.securerank.dto.response.SearchResultResponse;
import com.securerank.entity.UploadedFile;
import com.securerank.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "keywords", required = false) String keywords,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        ApiResponse response = fileService.uploadFile(file, label, keywords, ownerEmail);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping("/my-files")
    @PreAuthorize("hasAuthority('ROLE_OWNER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<FileMetadataResponse>> getMyFiles(Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(fileService.getMyFiles(ownerEmail));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_CONSUMER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SearchResultResponse>> searchFiles(
            @RequestParam("query") String query,
            Authentication authentication) {
        String consumerEmail = authentication.getName();
        return ResponseEntity.ok(fileService.searchFiles(query, consumerEmail));
    }

    @PostMapping("/request-key/{fileId}")
    @PreAuthorize("hasAuthority('ROLE_CONSUMER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse> requestFileKey(
            @PathVariable("fileId") Long fileId,
            @RequestBody(required = false) com.securerank.dto.request.KeyRequestDTO requestDto,
            Authentication authentication) {
        String consumerEmail = authentication.getName();
        String reason = requestDto != null ? requestDto.getReason() : null;
        ApiResponse response = fileService.requestFileKey(fileId, consumerEmail, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAuthority('ROLE_CONSUMER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<KeyRequestResponse>> getMyRequests(Authentication authentication) {
        String consumerEmail = authentication.getName();
        return ResponseEntity.ok(fileService.getMyKeyRequests(consumerEmail));
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable("fileId") Long fileId,
            Authentication authentication) {

        String userEmail = authentication.getName();
        UploadedFile file = fileService.getFileById(fileId);
        byte[] decryptedBytes = fileService.downloadAndDecryptFile(fileId, userEmail);

        String mediaType = file.getFileType() != null ? file.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(decryptedBytes);
    }

    @GetMapping("/{fileId}/qr-vc")
    public ResponseEntity<com.securerank.dto.response.QRVisualCryptoResponse> getQRVisualCryptoDetails(
            @PathVariable("fileId") Long fileId) {
        return ResponseEntity.ok(fileService.getQRVisualCryptoDetails(fileId));
    }

    @GetMapping("/{fileId}/benchmark")
    public ResponseEntity<com.securerank.dto.response.BenchmarkReportResponse> getLosslessBenchmark(
            @PathVariable("fileId") Long fileId) {
        return ResponseEntity.ok(fileService.getLosslessBenchmark(fileId));
    }
}
