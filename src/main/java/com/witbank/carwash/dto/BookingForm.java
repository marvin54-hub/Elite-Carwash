package com.witbank.carwash.dto;

import jakarta.validation.constraints.*;

public class BookingForm {

    @NotBlank(message = "Please enter your name.")
    private String name;

    @NotBlank(message = "Please enter a cellphone number.")
    @Pattern(regexp = "^(\\+27\\d{9}|0\\d{9})$",
             message = "Enter a valid SA cellphone number, e.g. 0812345678.")
    private String cellphone;

    @NotBlank(message = "Please enter your email address.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Please select a service package.")
    private String service;

    @NotBlank(message = "Please select a date and time.")
    private String time;

    private String vehicleReg;
    private String vehicleId;

    public String getName()      { return name; }
    public void   setName(String v) { this.name = v; }
    public String getCellphone() { return cellphone; }
    public void   setCellphone(String v) { this.cellphone = v; }
    public String getEmail()     { return email; }
    public void   setEmail(String v) { this.email = v; }
    public String getService()   { return service; }
    public void   setService(String v) { this.service = v; }
    public String getTime()      { return time; }
    public void   setTime(String v) { this.time = v; }
    public String getVehicleReg() { return vehicleReg; }
    public void   setVehicleReg(String v) { this.vehicleReg = v; }
    public String getVehicleId() { return vehicleId; }
    public void   setVehicleId(String v) { this.vehicleId = v; }
}
