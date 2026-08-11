package com.witbank.carwash.repository;

import com.witbank.carwash.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    long countByServiceType(String serviceType);
    List<Booking> findByCustomerIdOrderByBookingTimeDesc(Long customerId);
    List<Booking> findByBookingTime(LocalDateTime bookingTime);
    List<Booking> findByReminderSentFalseAndBookingTimeBetween(LocalDateTime from, LocalDateTime to);
    List<Booking> findByAssignedStaffId(Long staffId);
}
