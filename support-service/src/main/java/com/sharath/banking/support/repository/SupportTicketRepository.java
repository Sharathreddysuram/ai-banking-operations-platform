package com.sharath.banking.support.repository;
import com.sharath.banking.support.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
