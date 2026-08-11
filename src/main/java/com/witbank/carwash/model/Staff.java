package com.witbank.carwash.model;

import jakarta.persistence.*;

@Entity
@Table(name = "staff")
public class Staff {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role;  // "ADMIN" | "STAFF"

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean onLeave = false;

    public Staff() {}

    public Staff(String username, String password, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role     = role;
    }

    public Long    getId()        { return id; }
    public void    setId(Long v)  { this.id = v; }
    public String  getUsername()  { return username; }
    public void    setUsername(String v) { this.username = v; }
    public String  getPassword()  { return password; }
    public void    setPassword(String v) { this.password = v; }
    public String  getFullName()  { return fullName; }
    public void    setFullName(String v) { this.fullName = v; }
    public String  getRole()      { return role; }
    public void    setRole(String v) { this.role = v; }
    public boolean isActive()     { return active; }
    public void    setActive(boolean v) { this.active = v; }
    public boolean isOnLeave()    { return onLeave; }
    public void    setOnLeave(boolean v) { this.onLeave = v; }
}
