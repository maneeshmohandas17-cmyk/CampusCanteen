package com.canteen.campuscanteen.controller;

import com.canteen.campuscanteen.model.Cart;
import com.canteen.campuscanteen.model.Student;
import com.canteen.campuscanteen.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
public class CartController {

    private final CartService cartService;

    public static final List<String> PICKUP_SLOTS = Arrays.asList(
            "ASAP (Next 10-15 mins)",
            "10:45 AM - Morning Recess",
            "01:15 PM - Lunch Break Session 1",
            "01:45 PM - Lunch Break Session 2",
            "03:30 PM - Afternoon Tea Break",
            "04:45 PM - Evening After-Class Pickup"
    );

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Cart cart = cartService.getCart(session);
        Student student = (Student) session.getAttribute(StudentAuthController.SESSION_STUDENT);

        model.addAttribute("cart", cart);
        model.addAttribute("cartCount", cart.getTotalCount());
        model.addAttribute("currentStudent", student);
        model.addAttribute("pickupSlots", PICKUP_SLOTS);

        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long foodId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpServletRequest request,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        cartService.addToCart(session, foodId, quantity);
        redirectAttributes.addFlashAttribute("successToast", "Item added to your canteen tray!");

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/menu";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@RequestParam Long foodId,
                                 @RequestParam int quantity,
                                 HttpSession session) {
        cartService.updateQuantity(session, foodId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeItem(@RequestParam Long foodId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        cartService.removeFromCart(session, foodId);
        redirectAttributes.addFlashAttribute("info", "Item removed from tray.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        cartService.clearCart(session);
        return "redirect:/cart";
    }
}
