package com.securerank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultResponse {

    private Long id;
    private String filename;
    private String label;
    private String fileType;
    private Long fileSize;
    private String ownerEmail;
    private double score;
    private int rank;
    private String keyRequestStatus; // null, PENDING, APPROVED, REJECTED
    private LocalDateTime uploadedAt;
}
