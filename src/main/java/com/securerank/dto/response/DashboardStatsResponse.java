package com.securerank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    private long totalUsers;
    private long totalOwners;
    private long totalConsumers;
    private long pendingUsers;
    private long totalFiles;
    private long pendingKeyRequests;
    private long approvedKeyRequests;
}
