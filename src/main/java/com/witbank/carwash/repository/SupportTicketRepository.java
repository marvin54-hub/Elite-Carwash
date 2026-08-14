package com.witbank.carwash.repository;

import com.witbank.carwash.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCustomerIdOrderBySubmittedAtDesc(Long customerId);
    List<SupportTicket> findAllByOrderBySubmittedAtDesc();
}
