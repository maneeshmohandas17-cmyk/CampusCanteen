package com.canteen.campuscanteen.controller;

import com.canteen.campuscanteen.model.Food;
import com.canteen.campuscanteen.model.Order;
import com.canteen.campuscanteen.model.OrderStatus;
import com.canteen.campuscanteen.service.FoodService;
import com.canteen.campuscanteen.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    public static final String SESSION_ADMIN = "currentAdmin";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    private final OrderService orderService;
    private final FoodService foodService;

    public AdminController(OrderService orderService, FoodService foodService) {
        this.orderService = orderService;
        this.foodService = foodService;
    }

    private boolean isAuthenticated(HttpSession session) {
        return session.getAttribute(SESSION_ADMIN) != null;
    }

    @GetMapping("/login")
    public String adminLoginPage(HttpSession session) {
        if (isAuthenticated(session)) {
            return "redirect:/admin/dashboard";
        }
        return "admin/login";
    }

    @PostMapping("/login")
    public String handleAdminLogin(@RequestParam String username,
                                  @RequestParam String password,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        if (ADMIN_USER.equals(username.trim()) && ADMIN_PASS.equals(password.trim())) {
            session.setAttribute(SESSION_ADMIN, username.trim());
            return "redirect:/admin/dashboard";
        }

        redirectAttributes.addFlashAttribute("error", "Invalid Canteen Staff credentials!");
        return "redirect:/admin/login";
    }

    @GetMapping("/logout")
    public String adminLogout(HttpSession session) {
        session.removeAttribute(SESSION_ADMIN);
        return "redirect:/admin/login";
    }

    @GetMapping({"", "/", "/dashboard", "/orders"})
    public String kitchenDashboard(@RequestParam(required = false) String filter,
                                   HttpSession session,
                                   Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        List<Order> orders;
        if (filter != null && !filter.isEmpty() && !"ALL".equalsIgnoreCase(filter)) {
            try {
                OrderStatus status = OrderStatus.valueOf(filter.toUpperCase());
                orders = orderService.getOrdersByStatus(status);
            } catch (IllegalArgumentException e) {
                orders = orderService.getAllOrders();
            }
        } else {
            orders = orderService.getAllOrders();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentFilter", filter != null ? filter.toUpperCase() : "ALL");

        // Counter summary metrics
        model.addAttribute("confirmedCount", orderService.getCountByStatus(OrderStatus.CONFIRMED));
        model.addAttribute("readyCount", orderService.getCountByStatus(OrderStatus.READY_FOR_PICKUP));
        model.addAttribute("pendingCount", 0L);
        model.addAttribute("completedCount", orderService.getCountByStatus(OrderStatus.COMPLETED));
        model.addAttribute("totalRevenue", orderService.getTotalRevenue());
        model.addAttribute("totalOrders", orderService.getValidOrderCount());

        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String newStatus,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        try {
            OrderStatus status = OrderStatus.valueOf(newStatus.toUpperCase());
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Order status updated to " + status.getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update status: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/menu")
    public String manageMenu(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        model.addAttribute("foods", foodService.getAllFood());
        model.addAttribute("categories", foodService.getAllCategories());
        model.addAttribute("newFood", new Food());

        return "admin/menu";
    }

    @PostMapping("/menu/toggle/{id}")
    public String toggleFoodAvailability(@PathVariable Long id,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        boolean nowAvailable = foodService.toggleAvailability(id);
        redirectAttributes.addFlashAttribute("info", "Dish stock status updated: " + (nowAvailable ? "In Stock" : "Sold Out"));
        return "redirect:/admin/menu";
    }

    @PostMapping("/menu/add")
    public String addFoodItem(@ModelAttribute Food food,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        if (food.getImage() == null || food.getImage().trim().isEmpty()) {
            food.setImage(food.isVeg() ? "veg-sandwich.jpg" : "chicken-biryani.jpg");
        }
        foodService.saveFood(food);
        redirectAttributes.addFlashAttribute("success", "New dish '" + food.getName() + "' added to the canteen menu!");
        return "redirect:/admin/menu";
    }

    @PostMapping("/menu/delete/{id}")
    public String deleteFoodItem(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        foodService.deleteFood(id);
        redirectAttributes.addFlashAttribute("info", "Dish removed from menu.");
        return "redirect:/admin/menu";
    }
}
