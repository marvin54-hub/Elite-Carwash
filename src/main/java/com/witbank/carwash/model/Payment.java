package com.witbank.carwash.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String method;  // CARD | EFT | CASH_ON_SITE | YOCO

    @Column(nullable = false)
    private String status;  // Paid | Cash on Site – Pending

    private String reference;

    @Column(nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    public Payment() {}

    public Payment(Long bookingId, double amount, String method, String status, String reference) {
        this.bookingId = bookingId;
        this.amount    = amount;
        this.method    = method;
        this.status    = status;
        this.reference = reference;
    }

    public Long          getId()        { return id; }
    public Long          getBookingId() { return bookingId; }
    public double        getAmount()    { return amount; }
    public String        getMethod()    { return method; }
    public String        getStatus()    { return status; }
    public void          setStatus(String v) { this.status = v; }
    public String        getReference() { return reference; }
    public void          setReference(String v) { this.reference = v; }
    public LocalDateTime getPaidAt()    { return paidAt; }
}
