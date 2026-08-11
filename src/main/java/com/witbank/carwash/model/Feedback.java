package com.witbank.carwash.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    private String email;

    private Long bookingId;
    private Long customerId;

    @Column(nullable = false)
    private String serviceType;

    @Column(nullable = false)
    private int rating; // 1–5

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    public Feedback() {}

    public Feedback(String customerName, String email, String serviceType, int rating, String comment) {
        this.customerName = customerName;
        this.email        = email;
        this.serviceType  = serviceType;
        this.rating       = rating;
        this.comment      = comment;
    }

    public Long          getId()            { return id; }
    public String        getCustomerName()  { return customerName; }
    public void          setCustomerName(String v) { this.customerName = v; }
    public String        getEmail()         { return email; }
    public void          setEmail(String v) { this.email = v; }
    public Long          getBookingId()     { return bookingId; }
    public void          setBookingId(Long v) { this.bookingId = v; }
    public Long          getCustomerId()    { return customerId; }
    public void          setCustomerId(Long v) { this.customerId = v; }
    public String        getServiceType()   { return serviceType; }
    public void          setServiceType(String v) { this.serviceType = v; }
    public int           getRating()        { return rating; }
    public void          setRating(int v)   { this.rating = v; }
    public String        getComment()       { return comment; }
    public void          setComment(String v) { this.comment = v; }
    public LocalDateTime getSubmittedAt()   { return submittedAt; }
    public void          setSubmittedAt(LocalDateTime v) { this.submittedAt = v; }

    public String getStars() { return "★".repeat(rating) + "☆".repeat(5 - rating); }
}
