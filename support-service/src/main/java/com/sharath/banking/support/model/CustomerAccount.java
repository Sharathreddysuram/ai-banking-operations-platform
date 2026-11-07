package com.sharath.banking.support.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customer_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerId;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;
}
