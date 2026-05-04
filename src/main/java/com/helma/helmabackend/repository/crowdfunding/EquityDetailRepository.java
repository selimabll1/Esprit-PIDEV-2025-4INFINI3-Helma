package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.EquityDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquityDetailRepository extends JpaRepository<EquityDetail, Long> {
    Optional<EquityDetail> findByApplicationRaiseId(Long applicationRaiseId);

    boolean existsByCompanyRegistrationNumberIgnoreCase(String companyRegistrationNumber);
    List<EquityDetail> findByApplicationRaiseIdIn(java.util.Collection<Long> applicationRaiseIds);

    boolean existsByCompanyRegistrationNumberIgnoreCaseAndApplicationRaiseIdNot(
            String companyRegistrationNumber,
            Long applicationRaiseId
    );
}