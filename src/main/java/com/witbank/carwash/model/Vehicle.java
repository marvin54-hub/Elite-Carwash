package com.witbank.carwash.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String regPlate;

    private String color;

    public Vehicle() {}

    public Vehicle(Long customerId, String make, String model, String regPlate, String color) {
        this.customerId = customerId;
        this.make       = make;
        this.model      = model;
        this.regPlate   = regPlate;
        this.color      = color;
    }

    public Long   getId()         { return id; }
    public Long   getCustomerId() { return customerId; }
    public void   setCustomerId(Long v) { this.customerId = v; }
    public String getMake()       { return make; }
    public void   setMake(String v) { this.make = v; }
    public String getModel()      { return model; }
    public void   setModel(String v) { this.model = v; }
    public String getRegPlate()   { return regPlate; }
    public void   setRegPlate(String v) { this.regPlate = v; }
    public String getColor()      { return color; }
    public void   setColor(String v) { this.color = v; }

    public String getDisplayLabel() {
        String label = make + " " + model + " — " + regPlate;
        if (color != null && !color.isBlank()) label += " (" + color + ")";
        return label;
    }
}
