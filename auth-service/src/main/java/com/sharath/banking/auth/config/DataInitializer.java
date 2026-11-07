package com.sharath.banking.auth.config;

import com.sharath.banking.auth.entity.Role;
import com.sharath.banking.auth.entity.User;
import com.sharath.banking.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        String encoded = passwordEncoder.encode("password123");
        userRepository.save(User.builder()
                .username("admin")
                .email("admin@bank.local")
                .passwordHash(encoded)
                .fullName("Admin User")
                .role(Role.ADMIN)
                .enabled(true)
                .build());
        userRepository.save(User.builder()
                .username("ops.manager")
                .email("ops@bank.local")
                .passwordHash(encoded)
                .fullName("Operations Manager")
                .role(Role.OPS_MANAGER)
                .enabled(true)
                .build());
        userRepository.save(User.builder()
                .username("fraud.analyst")
                .email("fraud@bank.local")
                .passwordHash(encoded)
                .fullName("Fraud Analyst")
                .role(Role.FRAUD_ANALYST)
                .enabled(true)
                .build());
        userRepository.save(User.builder()
                .username("loan.officer")
                .email("loan@bank.local")
                .passwordHash(encoded)
                .fullName("Loan Officer")
                .role(Role.LOAN_OFFICER)
                .enabled(true)
                .build());
        userRepository.save(User.builder()
                .username("compliance.officer")
                .email("compliance@bank.local")
                .passwordHash(encoded)
                .fullName("Compliance Officer")
                .role(Role.COMPLIANCE)
                .enabled(true)
                .build());
    }
}
