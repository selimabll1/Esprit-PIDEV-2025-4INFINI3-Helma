package com.helma.helmabackend.repository;

 import com.helma.helmabackend.entity.Voucher;
 import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findBySavingsGoalId(Long goalId);
    List<Voucher> findBySavingsGoalUserId(Long userId);

}
