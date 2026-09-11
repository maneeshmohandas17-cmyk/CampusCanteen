package com.canteen.campuscanteen.controller;

import com.canteen.campuscanteen.model.Cart;
import com.canteen.campuscanteen.model.Order;
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

import java.util.List;
import java.util.Optional;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    private void addCommonAttributes(HttpSession session, Model model) {
        Cart cart = cartService.getCart(session);
        model.addAttribute("cartCount", cart.getTotalCount());
        Student student = (Student) session.getAttribute(StudentAuthController.SESSION_STUDENT);
        model.addAttribute("currentStudent", student);
    }

    @PostMapping("/order/place")
    public String placeOrder(@RequestParam String pickupSlot,
                             @RequestParam String paymentMethod,
                             @RequestParam(required = false) String specialInstructions,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        Student student = (Student) session.getAttribute(StudentAuthController.SESSION_STUDENT);
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Please login with your Student Roll Number to place an order.");
            return "redirect:/login?redirect=/cart";
        }

        Cart cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Your tray is empty! Please add food items first.");
            return "redirect:/menu";
        }

        try {
            Order order = orderService.placeOrder(student, cart, pickupSlot, paymentMethod, specialInstructions);
            cartService.clearCart(session);
            // Refresh student in session with updated wallet balance if changed
            session.setAttribute(StudentAuthController.SESSION_STUDENT, order.getStudent());

            // Online payment methods must be settled at the mock gateway before the token is confirmed
            if (order.getStatus() == com.canteen.campuscanteen.model.OrderStatus.AWAITING_PAYMENT) {
                return "redirect:/payment/" + order.getTokenNumber();
            }

            redirectAttributes.addFlashAttribute("orderSuccess", true);
            return "redirect:/order/" + order.getTokenNumber();

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/order/{token}")
    public String viewOrderStatus(@PathVariable String token,
                                  HttpSession session,
                                  Model model) {
        addCommonAttributes(session, model);

        Optional<Order> orderOpt = orderService.getOrderByToken(token);
        if (orderOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Order with token " + token + " not found!");
            return "error-page";
        }

        model.addAttribute("order", orderOpt.get());
        return "order-status";
    }

    @GetMapping("/orders")
    public String myOrders(HttpSession session, Model model) {
        Student student = (Student) session.getAttribute(StudentAuthController.SESSION_STUDENT);
        if (student == null) {
            return "redirect:/login?redirect=/orders";
        }

        addCommonAttributes(session, model);
        List<Order> orders = orderService.getOrdersByStudent(student);
        model.addAttribute("orders", orders);

        return "order-history";
    }
}
