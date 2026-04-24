package com.vielo.smartbet.payment;

import com.vielo.smartbet.security.AutoLoginService;
import com.vielo.smartbet.user.AppUser;
import com.vielo.smartbet.user.UserRepository;
import com.vielo.smartbet.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AutoLoginService autoLoginService;

    public PaymentController(PaymentService paymentService,
                             UserRepository userRepository,
                             UserService userService,
                             AutoLoginService autoLoginService) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.autoLoginService = autoLoginService;
    }

    @PostMapping("/payment/stripe/start")
    public String start(Authentication auth, @RequestParam("plan") String plan, RedirectAttributes redirectAttributes) {
        AppUser user = userRepository.findByEmail(auth.getName()).orElseThrow();
        userService.ensurePremiumRoleConsistency(user);

        if (userService.hasPremiumActive(user)) {
            redirectAttributes.addFlashAttribute("paymentError", "Un abonnement premium est déjà actif sur ce compte.");
            return "redirect:/pricing";
        }

        PlanType planType = "annual".equalsIgnoreCase(plan) ? PlanType.ANNUAL : PlanType.MONTHLY;
        try {
            PaymentOrder order = paymentService.createOrder(user, planType);
            String redirectUrl = paymentService.startStripeCheckout(order);
            return "redirect:" + redirectUrl;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("paymentError", e.getMessage());
            return "redirect:/pricing";
        }
    }

    @GetMapping("/payment/stripe/success")
    public String success(Authentication auth,
                          @RequestParam("session_id") String sessionId,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        boolean ok = paymentService.confirmStripeSession(sessionId);
        PaymentOrder order = paymentService.findBySessionId(sessionId).orElse(null);

        if (ok && order != null && order.getUser() != null) {
            autoLoginService.login(order.getUser().getEmail(), request, response);
            redirectAttributes.addFlashAttribute("paymentSuccess", "Paiement confirmé. Ton abonnement Premium est maintenant actif.");
            return "redirect:/dashboard";
        }

        if (auth != null && auth.isAuthenticated() && !userService.isAnonymous(auth)) {
            redirectAttributes.addFlashAttribute(ok ? "paymentSuccess" : "paymentError",
                    ok ? "Paiement confirmé. Ton abonnement Premium est maintenant actif."
                            : "Paiement non confirmé. Vérifie la session Stripe.");
            return "redirect:/dashboard";
        }

        model.addAttribute("ok", ok);
        model.addAttribute("orderId", order != null ? order.getOrderId() : "N/A");
        model.addAttribute("sessionId", sessionId);
        return "payment_result";
    }

    @GetMapping("/payment/stripe/cancel")
    public String cancel(@RequestParam(value = "orderId", required = false) String orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "payment_cancel";
    }
}
