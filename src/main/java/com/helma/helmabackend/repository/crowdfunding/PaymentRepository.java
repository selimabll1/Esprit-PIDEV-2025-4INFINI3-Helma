package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.Payment;
import com.helma.helmabackend.entity.crowdfunding.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBackerUserIdOrderByCreatedAtDesc(Long backerUserId);

    List<Payment> findByPledgeIdOrderByCreatedAtDesc(Long pledgeId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    Optional<Payment> findFirstByPledgeIdAndStatusInOrderByCreatedAtDesc(Long pledgeId, Collection<PaymentStatus> statuses);

    Optional<Payment> findByCheckoutSessionId(String checkoutSessionId);

    List<Payment> findByPledgeIdAndStatusInOrderByCreatedAtDesc(Long pledgeId, Collection<PaymentStatus> statuses);
}
