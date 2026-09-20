package com.canteen.campuscanteen.controller;

import com.canteen.campuscanteen.model.Cart;
import com.canteen.campuscanteen.model.Food;
import com.canteen.campuscanteen.model.Student;
import com.canteen.campuscanteen.service.CanteenService;
import com.canteen.campuscanteen.service.CartService;
import com.canteen.campuscanteen.service.FoodService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    private final CartService cartService;
    private final CanteenService canteenService;
    private final FoodService foodService;

    public CartController(CartService cartService, CanteenService canteenService, FoodService foodService) {
        this.cartService = cartService;
        this.canteenService = canteenService;
        this.foodService = foodService;
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Cart cart = cartService.getCart(session);
        Student student = (Student) session.getAttribute(StudentAuthController.SESSION_STUDENT);

        model.addAttribute("cart", cart);
        model.addAttribute("cartCount", cart.getTotalCount());
        model.addAttribute("currentStudent", student);
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long foodId,
                            @RequestParam(required = false) String canteen,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpServletRequest request,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        String selectedCanteen = canteen;
        if (selectedCanteen == null || selectedCanteen.trim().isEmpty()) {
            java.util.Optional<Food> foodOpt = foodService.getFoodById(foodId);
            if (foodOpt.isPresent() && foodOpt.get().getCanteens() != null) {
                Food f = foodOpt.get();
                selectedCanteen = f.getCanteens().stream()
                        .filter(c -> canteenService.isValidCanteen(c) && f.getStockForCanteen(c) > 0)
                        .findFirst()
                        .orElseGet(() -> f.getCanteens().stream()
                                .filter(canteenService::isValidCanteen)
                                .findFirst()
                                .orElse(null));
            }
        }

        if (selectedCanteen == null || !canteenService.isValidCanteen(selectedCanteen)) {
            redirectAttributes.addFlashAttribute("error", "The selected canteen is currently inactive or removed.");
            return "redirect:/menu";
        }

        try {
            cartService.addToCart(session, foodId, quantity, selectedCanteen);
            redirectAttributes.addFlashAttribute("successToast", "Item added to your canteen tray!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/menu";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@RequestParam Long foodId,
                                 @RequestParam String canteen,
                                 @RequestParam int quantity,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(session, foodId, canteen, quantity);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeItem(@RequestParam Long foodId,
                             @RequestParam String canteen,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        cartService.removeFromCart(session, foodId, canteen);
        redirectAttributes.addFlashAttribute("info", "Item removed from tray.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        cartService.clearCart(session);
        return "redirect:/cart";
    }
}
