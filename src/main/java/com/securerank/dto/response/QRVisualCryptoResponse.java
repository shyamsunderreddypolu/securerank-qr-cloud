package com.securerank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRVisualCryptoResponse {
    private Long fileId;
    private String filename;
    private String label;
    private String ownerEmail;
    private String qrCodeBase64;
    private String vcShare1Base64;
    private String vcShare2Base64;
    private String superimposedQRBase64;
    private int bitStreamLength;
    private String bitStreamPreview;
    private double shannonEntropy;
}
