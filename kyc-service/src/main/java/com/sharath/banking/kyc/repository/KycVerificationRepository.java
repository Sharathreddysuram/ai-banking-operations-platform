package com.sharath.banking.kyc.repository;

import com.sharath.banking.kyc.entity.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {
    List<KycVerification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<KycVerification> findFirstByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
