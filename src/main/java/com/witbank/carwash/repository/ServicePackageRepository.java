package com.witbank.carwash.repository;

import com.witbank.carwash.model.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, String> {
}
