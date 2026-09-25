package com.securerank.service;

import com.securerank.dto.response.ApiResponse;
import com.securerank.dto.response.DashboardStatsResponse;
import com.securerank.dto.response.KeyRequestResponse;
import com.securerank.entity.User;

import java.util.List;

public interface AdminService {

    List<User> getPendingUsers();

    ApiResponse approveUser(Long userId);

    List<KeyRequestResponse> getPendingKeyRequests();

    ApiResponse approveKeyRequest(Long requestId);

    DashboardStatsResponse getDashboardStats();
}
