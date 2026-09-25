package com.securerank.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    private String label;

    private String fileType;

    private Long fileSize;

    @Lob
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] encryptedBytes;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String encryptedSummary;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String indexVector;

    private String trapdoorKey;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String qrCodeBase64;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String vcShare1Base64;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String vcShare2Base64;

    private Integer bitStreamLength;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String binaryBitStream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
