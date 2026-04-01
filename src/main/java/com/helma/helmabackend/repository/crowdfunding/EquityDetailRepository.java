package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.EquityDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EquityDetailRepository extends JpaRepository<EquityDetail, Long> {

    Optional<EquityDetail> findByApplicationRaiseId(Long applicationRaiseId);

    List<EquityDetail> findByApplicationRaiseIdIn(Collection<Long> applicationRaiseIds);

    boolean existsByCompanyRegistrationNumberIgnoreCase(String companyRegistrationNumber);

    boolean existsByCompanyRegistrationNumberIgnoreCaseAndApplicationRaiseIdNot(
            String companyRegistrationNumber,
            Long applicationRaiseId
    );
}