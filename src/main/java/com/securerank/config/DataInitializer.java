package com.securerank.config;

import com.securerank.entity.Role;
import com.securerank.entity.User;
import com.securerank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@securerank.com")) {
            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@securerank.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ROLE_ADMIN)
                    .approved(true)
                    .mobile("9876543210")
                    .address("Cloud Headquarters")
                    .build();
            userRepository.save(admin);
            log.info("Default Admin account seeded: admin@securerank.com / admin123");
        }

        if (!userRepository.existsByEmail("owner@securerank.com")) {
            User owner = User.builder()
                    .name("Alice (Data Owner)")
                    .email("owner@securerank.com")
                    .password(passwordEncoder.encode("owner123"))
                    .role(Role.ROLE_OWNER)
                    .approved(true)
                    .mobile("9876500001")
                    .address("Research Lab")
                    .build();
            userRepository.save(owner);
            log.info("Default Data Owner account seeded: owner@securerank.com / owner123");
        }

        if (!userRepository.existsByEmail("consumer@securerank.com")) {
            User consumer = User.builder()
                    .name("Bob (Data Consumer)")
                    .email("consumer@securerank.com")
                    .password(passwordEncoder.encode("consumer123"))
                    .role(Role.ROLE_CONSUMER)
                    .approved(true)
                    .mobile("9876500002")
                    .address("City Hospital")
                    .build();
            userRepository.save(consumer);
            log.info("Default Data Consumer account seeded: consumer@securerank.com / consumer123");
        }
    }
}
