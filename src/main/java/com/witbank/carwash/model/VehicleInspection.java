package com.witbank.carwash.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_inspections")
public class VehicleInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    private String vehicleReg;

    @Column(nullable = false)
    private String conditionRating = "Good";

    @Column(length = 1000)
    private String existingDamageNotes;

    @Column(length = 1000)
    private String photoUrls;

    private String inspectedByStaff;

    @Column(nullable = false)
    private LocalDateTime inspectedAt = LocalDateTime.now();

    public VehicleInspection() {}

    public VehicleInspection(Long bookingId, String vehicleReg, String conditionRating,
                             String existingDamageNotes, String photoUrls, String inspectedByStaff) {
        this.bookingId = bookingId;
        this.vehicleReg = vehicleReg;
        this.conditionRating = conditionRating;
        this.existingDamageNotes = existingDamageNotes;
        this.photoUrls = photoUrls;
        this.inspectedByStaff = inspectedByStaff;
        this.inspectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getVehicleReg() { return vehicleReg; }
    public void setVehicleReg(String vehicleReg) { this.vehicleReg = vehicleReg; }

    public String getConditionRating() { return conditionRating; }
    public void setConditionRating(String conditionRating) { this.conditionRating = conditionRating; }

    public String getExistingDamageNotes() { return existingDamageNotes; }
    public void setExistingDamageNotes(String existingDamageNotes) { this.existingDamageNotes = existingDamageNotes; }

    public String getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(String photoUrls) { this.photoUrls = photoUrls; }

    public String getInspectedByStaff() { return inspectedByStaff; }
    public void setInspectedByStaff(String inspectedByStaff) { this.inspectedByStaff = inspectedByStaff; }

    public LocalDateTime getInspectedAt() { return inspectedAt; }
    public void setInspectedAt(LocalDateTime inspectedAt) { this.inspectedAt = inspectedAt; }
}
