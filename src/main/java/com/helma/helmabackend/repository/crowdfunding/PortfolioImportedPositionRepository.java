package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.PortfolioImportedPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioImportedPositionRepository extends JpaRepository<PortfolioImportedPosition, Long> {
    List<PortfolioImportedPosition> findByInvestorUserIdOrderByCreatedAtDesc(Long investorUserId);
    void deleteByInvestorUserId(Long investorUserId);
}
