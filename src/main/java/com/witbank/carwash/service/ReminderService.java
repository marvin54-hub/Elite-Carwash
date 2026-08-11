package com.witbank.carwash.service;

import com.witbank.carwash.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Sends automated SMS/email reminders ~24 hours before each booking.
 * Runs every 15 minutes; marks Booking.reminderSent so it fires once only.
 */
@Service
public class ReminderService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private NotificationService notificationService;

    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.findByReminderSentFalseAndBookingTimeBetween(
                now.plusHours(23), now.plusHours(24))
            .stream()
            .filter(b -> !"Cancelled".equalsIgnoreCase(b.getStatus()))
            .forEach(b -> {
                notificationService.dispatchSms(b.getCellphone(),
                        "Witbank Elite reminder: your " + b.getServiceType()
                        + " is tomorrow at " + b.getBookingTime().toLocalTime() + ". See you then!");
                notificationService.dispatchEmail(b.getEmail(),
                        "Reminder: Your Wash Tomorrow – Witbank Elite",
                        "Hi " + b.getCustomerName() + ", your " + b.getServiceType()
                        + " is scheduled for " + b.getBookingTime() + ".");
                b.setReminderSent(true);
                bookingRepository.save(b);
            });
    }
}
