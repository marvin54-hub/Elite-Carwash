package com.witbank.carwash.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String recipient; // Cellphone number or Email address
    private String type;      // "SMS" or "EMAIL"
    @Column(length = 2000)
    private String message;
    private LocalDateTime timestamp;
    private String status;    // "Delivered Internally"

    public NotificationLog() {}

    public NotificationLog(Long id, String recipient, String type, String message) {
        this.id = id;
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.status = "Delivered Internally";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
