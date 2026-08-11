package com.witbank.carwash.repository;

import com.witbank.carwash.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderBySubmittedAtDesc();
    Optional<Feedback> findByBookingId(Long bookingId);
    List<Feedback> findByCustomerId(Long customerId);
}
