package com.vielo.smartbet.payment;

import com.vielo.smartbet.user.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderId(String orderId);
    Optional<PaymentOrder> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentOrder p where p.orderId = :orderId")
    Optional<PaymentOrder> findByOrderIdForUpdate(@Param("orderId") String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentOrder p where p.stripeCheckoutSessionId = :sessionId")
    Optional<PaymentOrder> findByStripeCheckoutSessionIdForUpdate(@Param("sessionId") String sessionId);

    @Query("select p from PaymentOrder p where p.user = :user and p.status = :status and p.createdAt >= :since order by p.createdAt desc")
    List<PaymentOrder> findRecentByUserAndStatus(@Param("user") AppUser user,
                                                 @Param("status") PaymentStatus status,
                                                 @Param("since") LocalDateTime since);
}
