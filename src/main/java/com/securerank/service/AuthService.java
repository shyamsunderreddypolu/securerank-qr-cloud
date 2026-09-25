package com.securerank.service;

import com.securerank.dto.request.LoginRequest;
import com.securerank.dto.request.RegisterRequest;
import com.securerank.dto.response.ApiResponse;
import com.securerank.dto.response.JwtResponse;

public interface AuthService {

    ApiResponse registerUser(RegisterRequest registerRequest);

    JwtResponse authenticateUser(LoginRequest loginRequest);
}
