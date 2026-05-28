package com.yourorg.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {
    List<BillingRecord> findAllBySubscriptionUserIdOrderByPaidAtDesc(Long userId);
    List<BillingRecord> findAllBySubscriptionUserIdAndPaidAtAfterOrderByPaidAtAsc(Long userId, java.time.Instant paidAt);
}
