package com.canteen.campuscanteen.controller;

import com.canteen.campuscanteen.model.Cart;
import com.canteen.campuscanteen.model.Order;
import com.canteen.campuscanteen.model.OrderStatus;
import com.canteen.campuscanteen.model.Student;
import com.canteen.campuscanteen.service.CartService;
import com.canteen.campuscanteen.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Simulated online payment gateway. No real money or third-party API is involved -
 * this exists purely to demonstrate an end-to-end "online payment" checkout flow
 * (UPI / Card) for the prototype, complete with a success and a failure path.
 */
@Controller
public class PaymentController {

    private final OrderService orderService;
    private final CartService cartService;

    public PaymentController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    private void addCommonAttributes(HttpSession session, Model model) {
        Cart cart = cartService.getCart(session);
        model.addAttribute("cartCount", cart.getTotalCount());
        Student student = (Student) session.getAttribute(StudentAuthController.SESSION_STUDENT);
        model.addAttribute("currentStudent", student);
    }

    @GetMapping("/payment/{token}")
    public String showGateway(@PathVariable String token, HttpSession session, Model model) {
        addCommonAttributes(session, model);

        Optional<Order> orderOpt = orderService.getOrderByToken(token);
        if (orderOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Order with token " + token + " not found!");
            return "error-page";
        }

        Order order = orderOpt.get();

        // If it's already been settled (paid, or not an online order), skip straight to the token page
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            return "redirect:/order/" + order.getTokenNumber();
        }

        model.addAttribute("order", order);
        return "payment-gateway";
    }

    @PostMapping("/payment/{token}/process")
    public String processPayment(@PathVariable String token,
                                  @RequestParam String upiOrCardInput,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        // Demo-only simulation rule: including the word "fail" in the UPI ID / card field
        // simulates a declined payment, so both flows can be shown without real money involved.
        boolean simulateFailure = upiOrCardInput != null && upiOrCardInput.trim().toLowerCase().contains("fail");

        try {
            Order order = orderService.settleOnlinePayment(token, !simulateFailure);

            if (!simulateFailure && order.getPaymentStatus().equals("PAID")) {
                redirectAttributes.addFlashAttribute("orderSuccess", true);
                return "redirect:/order/" + order.getTokenNumber();
            } else {
                redirectAttributes.addFlashAttribute("paymentError",
                        "Payment declined by your bank/UPI app. Please try again or choose a different method.");
                return "redirect:/payment/" + token;
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("paymentError", ex.getMessage());
            return "redirect:/payment/" + token;
        }
    }
}
