package com.securerank.dto.response;

import com.securerank.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeyRequestResponse {

    private Long requestId;
    private Long fileId;
    private String filename;
    private String label;
    private String ownerEmail;
    private String consumerName;
    private String consumerEmail;
    private RequestStatus status;
    private String accessReason;
    private String masterKey; // Included only if status is APPROVED and caller is consumer
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
}
