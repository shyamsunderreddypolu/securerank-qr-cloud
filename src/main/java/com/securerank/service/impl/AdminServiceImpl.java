package com.securerank.service.impl;

import com.securerank.dto.response.ApiResponse;
import com.securerank.dto.response.DashboardStatsResponse;
import com.securerank.dto.response.KeyRequestResponse;
import com.securerank.entity.FileKey;
import com.securerank.entity.KeyRequest;
import com.securerank.entity.RequestStatus;
import com.securerank.entity.Role;
import com.securerank.entity.User;
import com.securerank.repository.FileKeyRepository;
import com.securerank.repository.KeyRequestRepository;
import com.securerank.repository.UploadedFileRepository;
import com.securerank.repository.UserRepository;
import com.securerank.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final KeyRequestRepository keyRequestRepository;
    private final FileKeyRepository fileKeyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<User> getPendingUsers() {
        return userRepository.findByApprovedFalse();
    }

    @Override
    @Transactional
    public ApiResponse approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setApproved(true);
        userRepository.save(user);

        log.info("Admin approved user: {}", user.getEmail());

        return ApiResponse.builder()
                .success(true)
                .message("User " + user.getEmail() + " approved successfully.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeyRequestResponse> getPendingKeyRequests() {
        return keyRequestRepository.findByStatusOrderByRequestedAtDesc(RequestStatus.PENDING)
                .stream()
                .map(req -> KeyRequestResponse.builder()
                        .requestId(req.getId())
                        .fileId(req.getFile().getId())
                        .filename(req.getFile().getFilename())
                        .label(req.getFile().getLabel())
                        .ownerEmail(req.getFile().getOwner().getEmail())
                        .consumerName(req.getConsumer().getName())
                        .consumerEmail(req.getConsumer().getEmail())
                        .status(req.getStatus())
                        .accessReason(req.getAccessReason())
                        .requestedAt(req.getRequestedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApiResponse approveKeyRequest(Long requestId) {
        KeyRequest request = keyRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Key request not found with ID: " + requestId));

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedAt(LocalDateTime.now());
        keyRequestRepository.save(request);

        log.info("Admin approved key request ID: {} for consumer: {}", requestId, request.getConsumer().getEmail());

        return ApiResponse.builder()
                .success(true)
                .message("Key request approved. Master key distributed to consumer.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalOwners = userRepository.findByRole(Role.ROLE_OWNER).size();
        long totalConsumers = userRepository.findByRole(Role.ROLE_CONSUMER).size();
        long pendingUsers = userRepository.findByApprovedFalse().size();
        long totalFiles = uploadedFileRepository.count();
        long pendingKeys = keyRequestRepository.findByStatusOrderByRequestedAtDesc(RequestStatus.PENDING).size();
        long approvedKeys = keyRequestRepository.findByStatusOrderByRequestedAtDesc(RequestStatus.APPROVED).size();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalConsumers(totalConsumers)
                .pendingUsers(pendingUsers)
                .totalFiles(totalFiles)
                .pendingKeyRequests(pendingKeys)
                .approvedKeyRequests(approvedKeys)
                .build();
    }
}
