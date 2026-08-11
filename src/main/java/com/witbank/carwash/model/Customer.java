package com.witbank.carwash.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String cellphone;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private int loyaltyPoints = 0;

    @Column(nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    public Customer() {}

    public Customer(String fullName, String email, String cellphone, String password) {
        this.fullName = fullName;
        this.email    = email;
        this.cellphone = cellphone;
        this.password = password;
    }

    public Long          getId()             { return id; }
    public void          setId(Long v)       { this.id = v; }
    public String        getFullName()       { return fullName; }
    public void          setFullName(String v) { this.fullName = v; }
    public String        getEmail()          { return email; }
    public void          setEmail(String v)  { this.email = v; }
    public String        getCellphone()      { return cellphone; }
    public void          setCellphone(String v) { this.cellphone = v; }
    public String        getPassword()       { return password; }
    public void          setPassword(String v) { this.password = v; }
    public int           getLoyaltyPoints()  { return loyaltyPoints; }
    public void          setLoyaltyPoints(int v) { this.loyaltyPoints = v; }
    public LocalDateTime getRegisteredAt()   { return registeredAt; }
    public void          setRegisteredAt(LocalDateTime v) { this.registeredAt = v; }
}
