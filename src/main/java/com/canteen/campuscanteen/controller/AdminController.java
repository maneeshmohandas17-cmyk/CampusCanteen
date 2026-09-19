package com.canteen.campuscanteen.controller;

import com.canteen.campuscanteen.model.Food;
import com.canteen.campuscanteen.model.Order;
import com.canteen.campuscanteen.model.OrderStatus;
import com.canteen.campuscanteen.service.CanteenService;
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
    public static final String SESSION_STAFF_CANTEEN = "staffCanteen";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    private final OrderService orderService;
    private final FoodService foodService;
    private final CanteenService canteenService;

    public AdminController(OrderService orderService, FoodService foodService, CanteenService canteenService) {
        this.orderService = orderService;
        this.foodService = foodService;
        this.canteenService = canteenService;
    }

    private boolean isAuthenticated(HttpSession session) {
        return session.getAttribute(SESSION_ADMIN) != null;
    }

    @GetMapping("/login")
    public String adminLoginPage(HttpSession session, Model model) {
        if (isAuthenticated(session)) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("canteens", canteenService.getAllCanteenNames());
        return "admin/login";
    }

    @PostMapping("/login")
    public String handleAdminLogin(@RequestParam String username,
                                  @RequestParam String password,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        String user = username != null ? username.trim() : "";
        String pass = password != null ? password.trim() : "";

        if (ADMIN_USER.equalsIgnoreCase(user) && ADMIN_PASS.equals(pass)) {
            session.setAttribute(SESSION_ADMIN, ADMIN_USER);
            session.removeAttribute(SESSION_STAFF_CANTEEN);
            return "redirect:/admin/dashboard";
        }

        // Support direct login for each canteen (looked up from DB, not hardcoded)
        for (String c : canteenService.getAllCanteenNames()) {
            if (c.equalsIgnoreCase(user) && (ADMIN_PASS.equals(pass) || c.equalsIgnoreCase(pass))) {
                session.setAttribute(SESSION_ADMIN, c);
                session.setAttribute(SESSION_STAFF_CANTEEN, c);
                return "redirect:/admin/dashboard";
            }
        }

        redirectAttributes.addFlashAttribute("error", "Invalid Canteen Staff credentials!");
        return "redirect:/admin/login";
    }

    @GetMapping("/logout")
    public String adminLogout(HttpSession session) {
        session.removeAttribute(SESSION_ADMIN);
        session.removeAttribute(SESSION_STAFF_CANTEEN);
        return "redirect:/admin/login";
    }

    @GetMapping({"", "/", "/dashboard", "/orders"})
    public String kitchenDashboard(@RequestParam(required = false) String filter,
                                   @RequestParam(required = false) String canteen,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null && !canteenService.isValidCanteen(staffCanteen)) {
            session.removeAttribute(SESSION_ADMIN);
            session.removeAttribute(SESSION_STAFF_CANTEEN);
            redirectAttributes.addFlashAttribute("error", "The canteen '" + staffCanteen + "' is no longer active. Order access has been disabled.");
            return "redirect:/admin/login";
        }

        String activeCanteen = canteen;
        if (staffCanteen != null && !staffCanteen.isEmpty()) {
            activeCanteen = staffCanteen;
        }

        List<String> canteenNames = canteenService.getAllCanteenNames();
        boolean hasCanteenFilter = activeCanteen != null && canteenNames.contains(activeCanteen);
        List<Order> orders;

        if (filter != null && !filter.isEmpty() && !"ALL".equalsIgnoreCase(filter)) {
            try {
                OrderStatus status = OrderStatus.valueOf(filter.toUpperCase());
                if (hasCanteenFilter) {
                    orders = orderService.getOrdersByCanteenAndStatus(activeCanteen, status);
                } else {
                    orders = orderService.getOrdersByStatus(status);
                }
            } catch (IllegalArgumentException e) {
                orders = hasCanteenFilter ? orderService.getOrdersByCanteen(activeCanteen) : orderService.getAllOrders();
            }
        } else {
            orders = hasCanteenFilter ? orderService.getOrdersByCanteen(activeCanteen) : orderService.getAllOrders();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentFilter", filter != null ? filter.toUpperCase() : "ALL");
        model.addAttribute("canteens", canteenNames);
        model.addAttribute("selectedCanteen", hasCanteenFilter ? activeCanteen : "ALL");
        model.addAttribute("staffCanteen", staffCanteen);

        // Counter summary metrics (canteen-specific or aggregate)
        if (hasCanteenFilter) {
            model.addAttribute("confirmedCount", orderService.getCountByStatusAndCanteen(OrderStatus.CONFIRMED, activeCanteen));
            model.addAttribute("readyCount", orderService.getCountByStatusAndCanteen(OrderStatus.READY_FOR_PICKUP, activeCanteen));
            model.addAttribute("pendingCount", 0L);
            model.addAttribute("completedCount", orderService.getCountByStatusAndCanteen(OrderStatus.COMPLETED, activeCanteen));
            model.addAttribute("totalRevenue", orderService.getTotalRevenueByCanteen(activeCanteen));
            model.addAttribute("totalOrders", orderService.getValidOrderCountByCanteen(activeCanteen));
        } else {
            model.addAttribute("confirmedCount", orderService.getCountByStatus(OrderStatus.CONFIRMED));
            model.addAttribute("readyCount", orderService.getCountByStatus(OrderStatus.READY_FOR_PICKUP));
            model.addAttribute("pendingCount", 0L);
            model.addAttribute("completedCount", orderService.getCountByStatus(OrderStatus.COMPLETED));
            model.addAttribute("totalRevenue", orderService.getTotalRevenue());
            model.addAttribute("totalOrders", orderService.getValidOrderCount());
        }

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

        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null && !canteenService.isValidCanteen(staffCanteen)) {
            session.removeAttribute(SESSION_ADMIN);
            session.removeAttribute(SESSION_STAFF_CANTEEN);
            redirectAttributes.addFlashAttribute("error", "The canteen '" + staffCanteen + "' is no longer active. Order access has been disabled.");
            return "redirect:/admin/login";
        }

        java.util.Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found with id: " + id);
            return "redirect:/admin/dashboard";
        }

        Order order = orderOpt.get();
        if (staffCanteen != null && !staffCanteen.equalsIgnoreCase(order.getCanteen())) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized: You can only manage orders belonging to " + staffCanteen);
            return "redirect:/admin/dashboard";
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
    public String manageMenu(@RequestParam(required = false) String canteen,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null && !canteenService.isValidCanteen(staffCanteen)) {
            session.removeAttribute(SESSION_ADMIN);
            session.removeAttribute(SESSION_STAFF_CANTEEN);
            redirectAttributes.addFlashAttribute("error", "The canteen '" + staffCanteen + "' is no longer active. Order access has been disabled.");
            return "redirect:/admin/login";
        }

        final String filterCanteen = (staffCanteen != null && !staffCanteen.isEmpty()) ? staffCanteen : canteen;

        List<String> canteenNames = canteenService.getAllCanteenNames();
        List<Food> foods = foodService.getAllFood();
        if (filterCanteen != null && canteenNames.contains(filterCanteen)) {
            foods = foods.stream().filter(f -> f.getCanteens() != null && f.getCanteens().contains(filterCanteen)).toList();
        }

        model.addAttribute("foods", foods);
        model.addAttribute("categories", foodService.getAllCategories());
        model.addAttribute("canteens", canteenNames);
        model.addAttribute("selectedCanteen", (filterCanteen != null && canteenNames.contains(filterCanteen)) ? filterCanteen : "ALL");
        model.addAttribute("staffCanteen", staffCanteen);
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

        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null && !canteenService.isValidCanteen(staffCanteen)) {
            session.removeAttribute(SESSION_ADMIN);
            session.removeAttribute(SESSION_STAFF_CANTEEN);
            redirectAttributes.addFlashAttribute("error", "The canteen '" + staffCanteen + "' is no longer active. Access disabled.");
            return "redirect:/admin/login";
        }

        if (staffCanteen != null) {
            java.util.Optional<Food> foodOpt = foodService.getFoodById(id);
            if (foodOpt.isEmpty() || foodOpt.get().getCanteens() == null || !foodOpt.get().getCanteens().contains(staffCanteen)) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized: You can only toggle stock for dishes served by " + staffCanteen);
                return "redirect:/admin/menu";
            }
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

        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null) {
            food.setCanteens(java.util.Set.of(staffCanteen));
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

        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null && !canteenService.isValidCanteen(staffCanteen)) {
            session.removeAttribute(SESSION_ADMIN);
            session.removeAttribute(SESSION_STAFF_CANTEEN);
            redirectAttributes.addFlashAttribute("error", "The canteen '" + staffCanteen + "' is no longer active. Access disabled.");
            return "redirect:/admin/login";
        }

        if (staffCanteen != null) {
            java.util.Optional<Food> foodOpt = foodService.getFoodById(id);
            if (foodOpt.isEmpty() || foodOpt.get().getCanteens() == null || !foodOpt.get().getCanteens().contains(staffCanteen)) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized: You can only delete dishes served by " + staffCanteen);
                return "redirect:/admin/menu";
            }
        }

        foodService.deleteFood(id);
        redirectAttributes.addFlashAttribute("info", "Dish removed from menu.");
        return "redirect:/admin/menu";
    }

    // =========================================================
    // Canteen Management
    // =========================================================

    @GetMapping("/canteens")
    public String manageCanteens(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }
        // Only full admin (not canteen staff) can manage canteens
        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("canteens", canteenService.getAllCanteens());
        model.addAttribute("staffCanteen", null);
        return "admin/canteens";
    }

    @PostMapping("/canteens/add")
    public String addCanteen(@RequestParam String name,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }
        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null) {
            return "redirect:/admin/dashboard";
        }

        try {
            canteenService.addCanteen(name);
            redirectAttributes.addFlashAttribute("success", "Canteen '" + name.trim() + "' added successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/canteens";
    }

    @PostMapping("/canteens/toggle/{id}")
    public String toggleCanteen(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }
        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null) {
            return "redirect:/admin/dashboard";
        }

        try {
            com.canteen.campuscanteen.model.Canteen canteen = canteenService.toggleCanteen(id);
            redirectAttributes.addFlashAttribute("info", "Canteen '" + canteen.getName() + "' status changed to: " + (canteen.isActive() ? "Active" : "Disabled"));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/canteens";
    }

    @PostMapping("/canteens/delete/{id}")
    public String deleteCanteen(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }
        String staffCanteen = (String) session.getAttribute(SESSION_STAFF_CANTEEN);
        if (staffCanteen != null) {
            return "redirect:/admin/dashboard";
        }

        try {
            canteenService.deleteCanteen(id);
            redirectAttributes.addFlashAttribute("info", "Canteen removed.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/canteens";
    }
}
