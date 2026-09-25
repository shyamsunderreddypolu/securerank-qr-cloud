package com.securerank.service.impl;

import com.securerank.dto.request.LoginRequest;
import com.securerank.dto.request.RegisterRequest;
import com.securerank.dto.response.ApiResponse;
import com.securerank.dto.response.JwtResponse;
import com.securerank.entity.Role;
import com.securerank.entity.User;
import com.securerank.repository.UserRepository;
import com.securerank.security.jwt.JwtUtils;
import com.securerank.security.service.UserDetailsImpl;
import com.securerank.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public ApiResponse registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ApiResponse.builder()
                    .success(false)
                    .message("Error: Email is already in use!")
                    .build();
        }

        // Create new user account (Admin role is auto-approved, Owner & Consumer require approval)
        boolean isAutoApproved = registerRequest.getRole() == Role.ROLE_ADMIN;

        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .mobile(registerRequest.getMobile())
                .address(registerRequest.getAddress())
                .role(registerRequest.getRole())
                .approved(isAutoApproved)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {} with role: {}", user.getEmail(), user.getRole());

        String message = isAutoApproved
                ? "Admin account created successfully!"
                : "User registered successfully! Please wait for Admin approval.";

        return ApiResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        log.info("User authenticated successfully: {} with role: {}", userDetails.getUsername(), role);

        return JwtResponse.builder()
                .token(jwt)
                .id(userDetails.getId())
                .name(userDetails.getName())
                .email(userDetails.getUsername())
                .role(role)
                .build();
    }
}
