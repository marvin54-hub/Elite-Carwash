package com.witbank.carwash.repository;

import com.witbank.carwash.model.StaffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, Long> {

    List<StaffSchedule> findByWorkDateBetweenOrderByWorkDateAscShiftStartAsc(
            LocalDate from, LocalDate to);

    @Query("SELECT s FROM StaffSchedule s WHERE s.workDate = :date ORDER BY s.shiftStart")
    List<StaffSchedule> findByWorkDate(@Param("date") LocalDate date);

    List<StaffSchedule> findByStaffIdOrderByWorkDateAsc(Long staffId);
}
