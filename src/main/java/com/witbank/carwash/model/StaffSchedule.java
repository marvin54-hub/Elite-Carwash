package com.witbank.carwash.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "staff_schedule")
public class StaffSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private String shiftStart;

    @Column(nullable = false)
    private String shiftEnd;

    private String notes;

    public StaffSchedule() {}

    public StaffSchedule(Staff staff, LocalDate workDate,
                         String shiftStart, String shiftEnd, String notes) {
        this.staff      = staff;
        this.workDate   = workDate;
        this.shiftStart = shiftStart;
        this.shiftEnd   = shiftEnd;
        this.notes      = notes;
    }

    public Long      getId()         { return id; }
    public Staff     getStaff()      { return staff; }
    public void      setStaff(Staff v)         { this.staff = v; }
    public LocalDate getWorkDate()   { return workDate; }
    public void      setWorkDate(LocalDate v)  { this.workDate = v; }
    public String    getShiftStart() { return shiftStart; }
    public void      setShiftStart(String v)   { this.shiftStart = v; }
    public String    getShiftEnd()   { return shiftEnd; }
    public void      setShiftEnd(String v)     { this.shiftEnd = v; }
    public String    getNotes()      { return notes; }
    public void      setNotes(String v)        { this.notes = v; }
}
