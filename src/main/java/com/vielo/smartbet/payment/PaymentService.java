package com.vielo.smartbet.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.vielo.smartbet.user.AppUser;
import com.vielo.smartbet.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository repo;
    private final StripeProperties stripeProperties;
    private final UserService userService;

    @Value("${vielo.pricing.monthly:9.99}")
    private double monthlyPrice;

    @Value("${vielo.pricing.annual:79.99}")
    private double annualPrice;

    @Value("${vielo.premium.monthly-days:30}")
    private int monthlyDays;

    @Value("${vielo.premium.annual-days:365}")
    private int annualDays;

    public PaymentService(PaymentRepository repo, StripeProperties stripeProperties, UserService userService) {
        this.repo = repo;
        this.stripeProperties = stripeProperties;
        this.userService = userService;
    }

    @Transactional
    public PaymentOrder createOrder(AppUser user, PlanType planType) {
        userService.ensurePremiumRoleConsistency(user);
        if (user.isPremiumActive(LocalDateTime.now())) {
            throw new IllegalStateException("Ce compte a déjà un abonnement Premium actif. Impossible de payer une deuxième fois.");
        }

        List<PaymentOrder> recentPendingOrders = repo.findRecentByUserAndStatus(
                user,
                PaymentStatus.PENDING,
                LocalDateTime.now().minusMinutes(30)
        );
        if (!recentPendingOrders.isEmpty()) {
            throw new IllegalStateException("Une tentative de paiement est déjà en cours pour ce compte. Termine-la ou attends son expiration avant de recommencer.");
        }

        double amount = planType == PlanType.MONTHLY ? monthlyPrice : annualPrice;
        String orderId = "VIELO-" + UUID.randomUUID();

        PaymentOrder order = PaymentOrder.builder()
                .orderId(orderId)
                .user(user)
                .planType(planType)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return repo.save(order);
    }

    public String startStripeCheckout(PaymentOrder order) {
        validateStripeConfiguration();
        Stripe.apiKey = stripeProperties.secretKey();

        long amountInCents = BigDecimal.valueOf(order.getAmount())
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        String planLabel = order.getPlanType() == PlanType.ANNUAL ? "Plan Premium Annuel" : "Plan Premium Mensuel";
        String successUrl = stripeProperties.successUrl() + "?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = stripeProperties.cancelUrl() + "?orderId=" + order.getOrderId();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(order.getOrderId())
                .setCustomerEmail(order.getUser().getEmail())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(resolveCurrency())
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(planLabel)
                                                                .setDescription("Accès premium VIELO SmartBet")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            Session session = Session.create(params);
            order.setStripeCheckoutSessionId(session.getId());
            repo.save(order);
            return session.getUrl();
        } catch (StripeException e) {
            throw new RuntimeException("Erreur lors de la création de la session Stripe: " + e.getMessage(), e);
        }
    }

    @Transactional
    public boolean confirmStripeSession(String sessionId) {
        validateStripeConfiguration();
        Stripe.apiKey = stripeProperties.secretKey();

        try {
            Session session = Session.retrieve(sessionId);
            if (session == null || !"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                return false;
            }

            String orderId = session.getClientReferenceId();
            PaymentOrder order = null;

            if (StringUtils.hasText(orderId)) {
                order = repo.findByOrderIdForUpdate(orderId).orElse(null);
            }

            if (order == null) {
                order = repo.findByStripeCheckoutSessionIdForUpdate(sessionId)
                        .orElseThrow(() -> new IllegalArgumentException("Aucune commande trouvée pour cette session Stripe."));
            }

            if (order.getStatus() == PaymentStatus.PAID) {
                return true;
            }

            markPaidAndGrant(order, sessionId);
            return true;
        } catch (StripeException e) {
            throw new RuntimeException("Erreur lors de la confirmation Stripe: " + e.getMessage(), e);
        }
    }

    public Optional<PaymentOrder> findBySessionId(String sessionId) {
        return repo.findByStripeCheckoutSessionId(sessionId);
    }

    private void markPaidAndGrant(PaymentOrder order, String sessionId) {
        order.setStatus(PaymentStatus.PAID);
        order.setStripeCheckoutSessionId(sessionId);
        order.setPaidAt(LocalDateTime.now());
        repo.save(order);

        int days = order.getPlanType() == PlanType.MONTHLY ? monthlyDays : annualDays;
        userService.grantPremium(order.getUser(), days);
    }

    private void validateStripeConfiguration() {
        if (!stripeProperties.enabled()) {
            throw new IllegalStateException("Stripe est désactivé dans application.properties");
        }
        if (!StringUtils.hasText(stripeProperties.secretKey())) {
            throw new IllegalStateException("La clé secrète Stripe est vide.");
        }
        if (!StringUtils.hasText(stripeProperties.successUrl()) || !StringUtils.hasText(stripeProperties.cancelUrl())) {
            throw new IllegalStateException("Les URLs Stripe de retour et d'annulation sont obligatoires.");
        }
    }

    private String resolveCurrency() {
        return StringUtils.hasText(stripeProperties.currency())
                ? stripeProperties.currency().toLowerCase()
                : "usd";
    }
}
