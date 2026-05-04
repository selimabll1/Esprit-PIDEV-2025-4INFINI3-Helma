package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.Pledge;
import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PledgeRepository extends JpaRepository<Pledge, Long> {

    List<Pledge> findByBackerUserIdOrderByCreatedAtDesc(Long backerUserId);

    Optional<Pledge> findByApplicationRaiseIdAndBackerUserId(Long applicationRaiseId, Long backerUserId);

    List<Pledge> findAllByOrderByCreatedAtDesc();

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Pledge p
            where p.applicationRaiseId = :applicationRaiseId
              and p.status = :status
            """)
    BigDecimal sumAmountByApplicationRaiseIdAndStatus(
            @Param("applicationRaiseId") Long applicationRaiseId,
            @Param("status") PledgeStatus status
    );
    List<Pledge> findByBackerUserIdAndStatusOrderByCreatedAtDesc(Long backerUserId, PledgeStatus status);
}
