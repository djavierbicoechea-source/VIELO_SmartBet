package com.vielo.smartbet.payment;

import com.vielo.smartbet.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order", columnList = "orderId", unique = true),
        @Index(name = "idx_payment_stripe_session", columnList = "stripeCheckoutSessionId", unique = true)
})
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanType planType;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(unique = true)
    private String stripeCheckoutSessionId;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public PaymentOrder() {
    }

    public PaymentOrder(Long id, String orderId, AppUser user, PlanType planType, Double amount,
                        PaymentStatus status, String stripeCheckoutSessionId,
                        LocalDateTime createdAt, LocalDateTime paidAt) {
        this.id = id;
        this.orderId = orderId;
        this.user = user;
        this.planType = planType;
        this.amount = amount;
        this.status = status;
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public static class Builder {
        private Long id;
        private String orderId;
        private AppUser user;
        private PlanType planType;
        private Double amount;
        private PaymentStatus status;
        private String stripeCheckoutSessionId;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder user(AppUser user) {
            this.user = user;
            return this;
        }

        public Builder planType(PlanType planType) {
            this.planType = planType;
            return this;
        }

        public Builder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public Builder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder stripeCheckoutSessionId(String stripeCheckoutSessionId) {
            this.stripeCheckoutSessionId = stripeCheckoutSessionId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public PaymentOrder build() {
            return new PaymentOrder(id, orderId, user, planType, amount, status, stripeCheckoutSessionId, createdAt, paidAt);
        }
    }
}
