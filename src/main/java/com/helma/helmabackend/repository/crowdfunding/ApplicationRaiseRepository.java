package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface ApplicationRaiseRepository
        extends JpaRepository<ApplicationRaise, Long>, JpaSpecificationExecutor<ApplicationRaise> {

    boolean existsByOwnerUserIdAndBusinessNameIgnoreCase(Long ownerUserId, String businessName);





    boolean existsByOwnerUserIdAndTypeAndStatusIn(
            Long ownerUserId,
            CrowdfundingType type,
            Collection<ApplicationRaiseStatus> statuses
    );

    boolean existsByOwnerUserIdAndTypeIsNullAndStatus(
            Long ownerUserId,
            ApplicationRaiseStatus status
    );

    Optional<ApplicationRaise> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}