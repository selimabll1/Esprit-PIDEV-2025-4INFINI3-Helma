package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationRaiseRepository
        extends JpaRepository<ApplicationRaise, Long>, JpaSpecificationExecutor<ApplicationRaise> {

    boolean existsByOwnerUserIdAndBusinessNameIgnoreCase(Long ownerUserId, String businessName);

    boolean existsByCompanyNumberIgnoreCase(String companyNumber);

    boolean existsByCompanyNumberIgnoreCaseAndIdNot(String companyNumber, Long id);
}