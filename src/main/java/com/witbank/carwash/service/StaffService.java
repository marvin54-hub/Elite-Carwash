package com.witbank.carwash.service;

import com.witbank.carwash.model.Staff;
import com.witbank.carwash.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffService {

    @Autowired private StaffRepository staffRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public List<Staff>      getAll()                          { return staffRepository.findAll(); }
    public boolean          usernameTaken(String username)    { return staffRepository.findByUsername(username).isPresent(); }
    public Optional<Staff>  findById(Long id)                 { return staffRepository.findById(id); }

    public Staff register(String username, String password, String fullName, String role) {
        return staffRepository.save(new Staff(username.trim(),
                passwordEncoder.encode(password), fullName.trim(), role.toUpperCase()));
    }

    public void updateDetails(Long id, String fullName, String role) {
        staffRepository.findById(id).ifPresent(s -> {
            s.setFullName(fullName.trim()); s.setRole(role.toUpperCase());
            staffRepository.save(s);
        });
    }

    public void resetPassword(Long id, String newPassword) {
        staffRepository.findById(id).ifPresent(s -> {
            s.setPassword(passwordEncoder.encode(newPassword));
            staffRepository.save(s);
        });
    }

    public void setActive(Long id, boolean active) {
        staffRepository.findById(id).ifPresent(s -> { s.setActive(active); staffRepository.save(s); });
    }

    public void setOnLeave(Long id, boolean onLeave) {
        staffRepository.findById(id).ifPresent(s -> { s.setOnLeave(onLeave); staffRepository.save(s); });
    }

    public long countActiveAdmins() {
        return staffRepository.findAll().stream()
                .filter(s -> s.isActive() && "ADMIN".equalsIgnoreCase(s.getRole()))
                .count();
    }
}
