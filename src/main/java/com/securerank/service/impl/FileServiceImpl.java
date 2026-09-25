package com.securerank.service.impl;

import com.securerank.dto.response.*;
import com.securerank.entity.*;
import com.securerank.repository.FileKeyRepository;
import com.securerank.repository.KeyRequestRepository;
import com.securerank.repository.UploadedFileRepository;
import com.securerank.repository.UserRepository;
import com.securerank.service.FileService;
import com.securerank.service.LosslessTransformationService;
import com.securerank.service.QRVisualCryptoService;
import com.securerank.util.AESCryptoUtils;
import com.securerank.util.TFIDFUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final UploadedFileRepository uploadedFileRepository;
    private final FileKeyRepository fileKeyRepository;
    private final KeyRequestRepository keyRequestRepository;
    private final UserRepository userRepository;
    private final AESCryptoUtils aesCryptoUtils;
    private final TFIDFUtils tfidfUtils;
    private final QRVisualCryptoService qrVisualCryptoService;
    private final LosslessTransformationService losslessTransformationService;

    @Override
    @Transactional
    public ApiResponse uploadFile(MultipartFile file, String label, String keywords, String ownerEmail) {
        try {
            User owner = userRepository.findByEmail(ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Owner not found: " + ownerEmail));

            byte[] originalBytes = file.getBytes();
            String masterKey = aesCryptoUtils.generateMasterKey();
            byte[] encryptedBytes = aesCryptoUtils.encrypt(originalBytes, masterKey);

            String textContent = "";
            try {
                textContent = new String(originalBytes, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }

            String indexVector = tfidfUtils.buildIndexVector(textContent, keywords);
            String trapdoorKey = aesCryptoUtils.generateTrapdoor(keywords != null && !keywords.isEmpty() ? keywords : file.getOriginalFilename());

            // 1. Generate QR Code containing secure file metadata
            String qrPayload = String.format("{\"file\":\"%s\",\"label\":\"%s\",\"owner\":\"%s\",\"trapdoor\":\"%s\"}",
                    file.getOriginalFilename(), label != null ? label : "", ownerEmail, trapdoorKey);
            BufferedImage qrImage = qrVisualCryptoService.generateQRCode(qrPayload, 128, 128);
            String qrCodeBase64 = qrVisualCryptoService.imageToBase64DataUri(qrImage);

            // 2. Generate 2-out-of-2 Visual Cryptography Shares
            Map<String, Object> vcShares = qrVisualCryptoService.generateVisualCryptoShares(qrImage);
            String share1Base64 = (String) vcShares.get("share1Base64");
            String share2Base64 = (String) vcShares.get("share2Base64");
            String bitStream = (String) vcShares.get("bitStream");
            int bitStreamLen = (Integer) vcShares.get("bitStreamLength");

            UploadedFile uploadedFile = UploadedFile.builder()
                    .filename(file.getOriginalFilename())
                    .label(label)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .encryptedBytes(encryptedBytes)
                    .encryptedSummary(keywords)
                    .indexVector(indexVector)
                    .trapdoorKey(trapdoorKey)
                    .qrCodeBase64(qrCodeBase64)
                    .vcShare1Base64(share1Base64)
                    .vcShare2Base64(share2Base64)
                    .bitStreamLength(bitStreamLen)
                    .binaryBitStream(bitStream)
                    .owner(owner)
                    .build();

            UploadedFile savedFile = uploadedFileRepository.save(uploadedFile);

            FileKey fileKey = FileKey.builder()
                    .file(savedFile)
                    .masterKey(masterKey)
                    .build();
            fileKeyRepository.save(fileKey);

            log.info("Uploaded and encrypted file: {} (ID: {}) with QR and (2,2) Visual Cryptography for owner: {}",
                    savedFile.getFilename(), savedFile.getId(), ownerEmail);

            return ApiResponse.builder()
                    .success(true)
                    .message("File uploaded, encrypted with AES-256, QR code & Visual Cryptography shares generated successfully!")
                    .build();
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return ApiResponse.builder()
                    .success(false)
                    .message("Upload failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getMyFiles(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found: " + ownerEmail));

        return uploadedFileRepository.findByOwnerOrderByUploadedAtDesc(owner)
                .stream()
                .map(f -> FileMetadataResponse.builder()
                        .id(f.getId())
                        .filename(f.getFilename())
                        .label(f.getLabel())
                        .fileType(f.getFileType())
                        .fileSize(f.getFileSize())
                        .ownerName(f.getOwner().getName())
                        .ownerEmail(f.getOwner().getEmail())
                        .uploadedAt(f.getUploadedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchResultResponse> searchFiles(String query, String consumerEmail) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keywords = Arrays.stream(query.toLowerCase().split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<UploadedFile> allFiles = uploadedFileRepository.findAll();
        long totalDocs = allFiles.size();

        User consumer = consumerEmail != null ? userRepository.findByEmail(consumerEmail).orElse(null) : null;

        List<SearchResultResponse> results = new ArrayList<>();

        for (UploadedFile file : allFiles) {
            double score = tfidfUtils.calculateRelevanceScore(file.getIndexVector(), keywords, totalDocs);
            if (score > 0.0) {
                String requestStatus = null;
                if (consumer != null) {
                    Optional<KeyRequest> reqOpt = keyRequestRepository.findByFileAndConsumer(file, consumer);
                    if (reqOpt.isPresent()) {
                        requestStatus = reqOpt.get().getStatus().name();
                    }
                }

                results.add(SearchResultResponse.builder()
                        .id(file.getId())
                        .filename(file.getFilename())
                        .label(file.getLabel())
                        .fileType(file.getFileType())
                        .fileSize(file.getFileSize())
                        .ownerEmail(file.getOwner().getEmail())
                        .score(score)
                        .keyRequestStatus(requestStatus)
                        .uploadedAt(file.getUploadedAt())
                        .build());
            }
        }

        // Rank by score descending
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        return results;
    }

    @Override
    @Transactional
    public ApiResponse requestFileKey(Long fileId, String consumerEmail, String accessReason) {
        User consumer = userRepository.findByEmail(consumerEmail)
                .orElseThrow(() -> new RuntimeException("Consumer not found: " + consumerEmail));

        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        Optional<KeyRequest> existing = keyRequestRepository.findByFileAndConsumer(file, consumer);
        if (existing.isPresent()) {
            RequestStatus status = existing.get().getStatus();
            if (status == RequestStatus.PENDING) {
                return ApiResponse.builder().success(false).message("Key request already submitted and is pending approval.").build();
            } else if (status == RequestStatus.APPROVED) {
                return ApiResponse.builder().success(true).message("Key request was already approved! You can download the file.").build();
            }
        }

        KeyRequest request = existing.orElseGet(() -> KeyRequest.builder()
                .file(file)
                .consumer(consumer)
                .status(RequestStatus.PENDING)
                .build());

        request.setStatus(RequestStatus.PENDING);
        request.setAccessReason(accessReason != null && !accessReason.trim().isEmpty()
                ? accessReason.trim()
                : "Secure document review & evaluation");
        keyRequestRepository.save(request);

        log.info("Consumer {} requested key for file ID {} with reason: {}", consumerEmail, fileId, request.getAccessReason());

        return ApiResponse.builder()
                .success(true)
                .message("Key request submitted to Admin successfully with access reason!")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeyRequestResponse> getMyKeyRequests(String consumerEmail) {
        User consumer = userRepository.findByEmail(consumerEmail)
                .orElseThrow(() -> new RuntimeException("Consumer not found: " + consumerEmail));

        return keyRequestRepository.findByConsumerOrderByRequestedAtDesc(consumer)
                .stream()
                .map(req -> {
                    String masterKey = null;
                    if (req.getStatus() == RequestStatus.APPROVED) {
                        masterKey = fileKeyRepository.findByFile(req.getFile())
                                .map(FileKey::getMasterKey)
                                .orElse(null);
                    }

                    return KeyRequestResponse.builder()
                            .requestId(req.getId())
                            .fileId(req.getFile().getId())
                            .filename(req.getFile().getFilename())
                            .label(req.getFile().getLabel())
                            .ownerEmail(req.getFile().getOwner().getEmail())
                            .consumerName(req.getConsumer().getName())
                            .consumerEmail(req.getConsumer().getEmail())
                            .status(req.getStatus())
                            .accessReason(req.getAccessReason())
                            .masterKey(masterKey)
                            .requestedAt(req.getRequestedAt())
                            .approvedAt(req.getApprovedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadAndDecryptFile(Long fileId, String userEmail) {
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        boolean isOwner = file.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_PKG;

        if (!isOwner && !isAdmin) {
            KeyRequest request = keyRequestRepository.findByFileAndConsumer(file, user)
                    .orElseThrow(() -> new AccessDeniedException("Access Denied: You have not requested access to this file."));

            if (request.getStatus() != RequestStatus.APPROVED) {
                throw new AccessDeniedException("Access Denied: Key request is not approved yet.");
            }
        }

        FileKey fileKey = fileKeyRepository.findByFile(file)
                .orElseThrow(() -> new RuntimeException("Master decryption key not found for file ID: " + fileId));

        log.info("Decrypting and serving file: {} for user: {}", file.getFilename(), userEmail);
        return aesCryptoUtils.decrypt(file.getEncryptedBytes(), fileKey.getMasterKey());
    }

    @Override
    @Transactional(readOnly = true)
    public UploadedFile getFileById(Long fileId) {
        return uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
    }

    @Override
    @Transactional
    public QRVisualCryptoResponse getQRVisualCryptoDetails(Long fileId) {
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        String qrCode = file.getQrCodeBase64();
        String share1 = file.getVcShare1Base64();
        String share2 = file.getVcShare2Base64();
        String bitStream = file.getBinaryBitStream();

        // If previously uploaded file without QR/VC, generate on-the-fly and save
        if (qrCode == null || share1 == null || share2 == null || bitStream == null) {
            String qrPayload = String.format("{\"fileId\":%d,\"name\":\"%s\",\"owner\":\"%s\"}",
                    file.getId(), file.getFilename(), file.getOwner().getEmail());
            BufferedImage qrImg = qrVisualCryptoService.generateQRCode(qrPayload, 128, 128);
            qrCode = qrVisualCryptoService.imageToBase64DataUri(qrImg);

            Map<String, Object> vcShares = qrVisualCryptoService.generateVisualCryptoShares(qrImg);
            share1 = (String) vcShares.get("share1Base64");
            share2 = (String) vcShares.get("share2Base64");
            bitStream = (String) vcShares.get("bitStream");
            int bitLen = (Integer) vcShares.get("bitStreamLength");

            file.setQrCodeBase64(qrCode);
            file.setVcShare1Base64(share1);
            file.setVcShare2Base64(share2);
            file.setBinaryBitStream(bitStream);
            file.setBitStreamLength(bitLen);
            uploadedFileRepository.save(file);
        }

        // Superimpose Share 1 and Share 2 to generate reconstructed image
        String superimposedBase64 = "";
        try {
            byte[] s1Bytes = Base64.getDecoder().decode(share1.replace("data:image/png;base64,", ""));
            byte[] s2Bytes = Base64.getDecoder().decode(share2.replace("data:image/png;base64,", ""));
            BufferedImage s1Img = ImageIO.read(new ByteArrayInputStream(s1Bytes));
            BufferedImage s2Img = ImageIO.read(new ByteArrayInputStream(s2Bytes));
            BufferedImage superimposed = qrVisualCryptoService.superimposeShares(s1Img, s2Img);
            superimposedBase64 = qrVisualCryptoService.imageToBase64DataUri(superimposed);
        } catch (Exception e) {
            log.warn("Could not superimpose shares: {}", e.getMessage());
            superimposedBase64 = qrCode;
        }

        double entropy = losslessTransformationService.calculateShannonEntropy(bitStream);
        String preview = bitStream.length() > 100 ? bitStream.substring(0, 100) + "..." : bitStream;

        return QRVisualCryptoResponse.builder()
                .fileId(file.getId())
                .filename(file.getFilename())
                .label(file.getLabel())
                .ownerEmail(file.getOwner().getEmail())
                .qrCodeBase64(qrCode)
                .vcShare1Base64(share1)
                .vcShare2Base64(share2)
                .superimposedQRBase64(superimposedBase64)
                .bitStreamLength(bitStream.length())
                .bitStreamPreview(preview)
                .shannonEntropy(entropy)
                .build();
    }

    @Override
    @Transactional
    public BenchmarkReportResponse getLosslessBenchmark(Long fileId) {
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        String bitStream = file.getBinaryBitStream();
        if (bitStream == null || bitStream.isEmpty()) {
            getQRVisualCryptoDetails(fileId);
            file = uploadedFileRepository.findById(fileId).orElse(file);
            bitStream = file.getBinaryBitStream();
        }

        return losslessTransformationService.runFullBenchmark(file.getId(), file.getFilename(), bitStream);
    }
}
