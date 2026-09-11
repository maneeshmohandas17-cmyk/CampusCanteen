package com.canteen.campuscanteen.controller;

import com.canteen.campuscanteen.model.Cart;
import com.canteen.campuscanteen.model.Food;
import com.canteen.campuscanteen.model.Student;
import com.canteen.campuscanteen.service.CartService;
import com.canteen.campuscanteen.service.FoodService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MenuController {

    private final FoodService foodService;
    private final CartService cartService;

    public MenuController(FoodService foodService, CartService cartService) {
        this.foodService = foodService;
        this.cartService = cartService;
    }

    private void addCommonAttributes(HttpSession session, Model model) {
        Cart cart = cartService.getCart(session);
        model.addAttribute("cartCount", cart.getTotalCount());
        Student student = (Student) session.getAttribute(StudentAuthController.SESSION_STUDENT);
        model.addAttribute("currentStudent", student);
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        addCommonAttributes(session, model);
        List<Food> allFood = foodService.getAllAvailableFood();
        // Highlight top 4 items for home hero
        model.addAttribute("featuredFoods", allFood.stream().limit(4).collect(Collectors.toList()));
        model.addAttribute("categories", foodService.getAllCategories());
        return "index";
    }

    @GetMapping("/menu")
    public String menu(@RequestParam(required = false) String category,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false, defaultValue = "false") boolean vegOnly,
                       HttpSession session,
                       Model model) {

        addCommonAttributes(session, model);

        List<Food> foods;
        if (search != null && !search.trim().isEmpty()) {
            foods = foodService.searchFood(search);
        } else if (category != null && !category.equalsIgnoreCase("All") && !category.trim().isEmpty()) {
            foods = foodService.getFoodByCategory(category);
        } else {
            foods = foodService.getAllFood();
        }

        if (vegOnly) {
            foods = foods.stream().filter(Food::isVeg).collect(Collectors.toList());
        }

        model.addAttribute("foods", foods);
        model.addAttribute("categories", foodService.getAllCategories());
        model.addAttribute("selectedCategory", category != null ? category : "All");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("vegOnly", vegOnly);

        return "menu";
    }
}
