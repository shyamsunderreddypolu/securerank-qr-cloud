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
public class FileMetadataResponse {

    private Long id;
    private String filename;
    private String label;
    private String fileType;
    private Long fileSize;
    private String ownerName;
    private String ownerEmail;
    private LocalDateTime uploadedAt;
}
