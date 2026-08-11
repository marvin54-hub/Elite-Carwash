package com.witbank.carwash.service;

import com.witbank.carwash.model.NotificationLog;
import com.witbank.carwash.repository.NotificationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification system:
 *   EMAIL → queued and sent via EmailJS on the frontend (real delivery)
 *          recipient = actual customer email address
 *   SMS   → Outbox Logged (simulated) — logged with the customer's real mobile number
 *
 * All notifications are CC'd / also sent to marvinnicolasmathebula@gmail.com
 * (configured via the EmailJS template's reply_to / bcc field).
 */
@Service
public class NotificationService {

    @Autowired private NotificationLogRepository logRepository;

    /** Admin / owner notification email that receives copies of all emails. */
    public static final String ADMIN_EMAIL = "marvinnicolasmathebula@gmail.com";

    public void dispatchEmail(String recipientEmail, String subject, String body) {
        String recipient = (recipientEmail != null && !recipientEmail.isBlank())
                ? recipientEmail.trim() : ADMIN_EMAIL;
        String logMsg = "TO: " + recipient + " | SUBJECT: " + subject + " | " + body;
        NotificationLog log = new NotificationLog(null, recipient, "EMAIL", logMsg);
        logRepository.save(log);
    }

    public void dispatchSms(String recipientPhone, String message) {
        // SMS is simulated but logged with the actual customer mobile number
        String phone = (recipientPhone != null && !recipientPhone.isBlank())
                ? recipientPhone.trim() : "N/A";
        String logMsg = "TO: " + phone + " | " + message;
        NotificationLog log = new NotificationLog(null, phone, "SMS", logMsg);
        logRepository.save(log);
    }

    public List<NotificationLog> getAllDispatchedLogs() {
        return logRepository.findAll();
    }

    public long getUnreadCount() {
        return logRepository.count();
    }
}
