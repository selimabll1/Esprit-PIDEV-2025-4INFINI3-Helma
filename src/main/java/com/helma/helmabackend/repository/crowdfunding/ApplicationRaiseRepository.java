package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRaiseRepository extends JpaRepository<ApplicationRaise, Long> {

    List<ApplicationRaise> findByFounderUserId(Long founderUserId);

    List<ApplicationRaise> findByStatusOrderByCreatedAtDesc(ApplicationRaiseStatus status);

    boolean existsByFounderUserIdAndBusinessNameIgnoreCase(Long founderUserId, String businessName);

    boolean existsByCompanyNumberIgnoreCase(String companyNumber);

    boolean existsByCompanyNumberIgnoreCaseAndIdNot(String companyNumber, Long id);
}
