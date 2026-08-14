package com.witbank.carwash.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;
    private String cellphone;
    private String email;

    @Column(nullable = false)
    private String serviceType;

    @Column(nullable = false)
    private LocalDateTime bookingTime;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String status = "Pending";

    /** Optional: vehicle registration number entered at booking time. */
    private String vehicleReg;

    /** FK to Customer account — null for guest bookings. */
    private Long customerId;

    /** FK to saved Vehicle — null for guests or unlinked bookings. */
    private Long vehicleId;

    /** FK to Staff member assigned to this booking — null = unassigned. */
    private Long assignedStaffId;

    @Column(nullable = false)
    private String paymentStatus = "Unpaid";

    private String paymentRef;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean reminderSent = false;

    public Booking() {}

    public Booking(Long id, String customerName, String cellphone, String email,
                   String serviceType, LocalDateTime bookingTime, double price) {
        this.id           = id;
        this.customerName = customerName;
        this.cellphone    = cellphone;
        this.email        = email;
        this.serviceType  = serviceType;
        this.bookingTime  = bookingTime;
        this.price        = price;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long          getId()               { return id; }
    public void          setId(Long v)         { this.id = v; }
    public String        getCustomerName()     { return customerName; }
    public void          setCustomerName(String v) { this.customerName = v; }
    public String        getCellphone()        { return cellphone; }
    public void          setCellphone(String v) { this.cellphone = v; }
    public String        getEmail()            { return email; }
    public void          setEmail(String v)    { this.email = v; }
    public String        getServiceType()      { return serviceType; }
    public void          setServiceType(String v) { this.serviceType = v; }
    public LocalDateTime getBookingTime()      { return bookingTime; }
    public void          setBookingTime(LocalDateTime v) { this.bookingTime = v; }
    public double        getPrice()            { return price; }
    public void          setPrice(double v)    { this.price = v; }
    public String        getStatus()           { return status; }
    public void          setStatus(String v)   { this.status = v; }
    public String        getVehicleReg()       { return vehicleReg; }
    public void          setVehicleReg(String v) { this.vehicleReg = v; }
    public Long          getCustomerId()       { return customerId; }
    public void          setCustomerId(Long v) { this.customerId = v; }
    public Long          getVehicleId()        { return vehicleId; }
    public void          setVehicleId(Long v)  { this.vehicleId = v; }
    public Long          getAssignedStaffId()  { return assignedStaffId; }
    public void          setAssignedStaffId(Long v) { this.assignedStaffId = v; }
    public String        getPaymentStatus()    { return paymentStatus; }
    public void          setPaymentStatus(String v) { this.paymentStatus = v; }
    public String        getPaymentRef()       { return paymentRef; }
    public void          setPaymentRef(String v) { this.paymentRef = v; }
    public boolean       isVerified()          { return verified; }
    public void          setVerified(boolean v) { this.verified = v; }
    public boolean       isReminderSent()      { return reminderSent; }
    public void          setReminderSent(boolean v) { this.reminderSent = v; }

    @Column(length = 1000)
    private String selectedAddOns;

    @Column(length = 1000)
    private String serviceNotes;

    public String getSelectedAddOns() { return selectedAddOns; }
    public void setSelectedAddOns(String v) { this.selectedAddOns = v; }

    /** Convenience: true when paymentStatus is "Paid" (any variant). */
    public String getServiceNotes() { return serviceNotes; }
    public void setServiceNotes(String v) { this.serviceNotes = v; }

    public boolean isPaid() {
        return paymentStatus != null && paymentStatus.toLowerCase().contains("paid");
    }

    /** Returns stage number (1 to 7) or -1 for Cancelled. */
    public int getStatusStageIndex() {
        if (status == null) return 1;
        switch (status.trim()) {
            case "Confirmed": return 2;
            case "Vehicle Received": return 3;
            case "In Progress":
            case "Washing": return 4;
            case "Quality Check": return 5;
            case "Completed": return 6;
            case "Vehicle Collected": return 7;
            case "Cancelled": return -1;
            default: return 1; // Pending
        }
    }

    /** Returns status emoji icon. */
    public String getStatusIcon() {
        if (status == null) return "⌛";
        switch (status.trim()) {
            case "Confirmed": return "📅";
            case "Vehicle Received": return "🔑";
            case "In Progress":
            case "Washing": return "🧼";
            case "Quality Check": return "✨";
            case "Completed": return "🎉";
            case "Vehicle Collected": return "🚗";
            case "Cancelled": return "✕";
            default: return "⌛";
        }
    }
}
