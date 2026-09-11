package com.canteen.campuscanteen.service;

import com.canteen.campuscanteen.model.Cart;
import com.canteen.campuscanteen.model.Food;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService {

    public static final String SESSION_CART_KEY = "CANTEEN_CART";

    private final FoodService foodService;

    public CartService(FoodService foodService) {
        this.foodService = foodService;
    }

    public Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(SESSION_CART_KEY);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(SESSION_CART_KEY, cart);
        }
        return cart;
    }

    public void addToCart(HttpSession session, Long foodId, int quantity) {
        if (quantity <= 0) return;
        Optional<Food> foodOpt = foodService.getFoodById(foodId);
        if (foodOpt.isPresent() && foodOpt.get().isAvailable()) {
            Cart cart = getCart(session);
            cart.addItem(foodOpt.get(), quantity);
            session.setAttribute(SESSION_CART_KEY, cart);
        }
    }

    public void updateQuantity(HttpSession session, Long foodId, int quantity) {
        Cart cart = getCart(session);
        cart.updateQuantity(foodId, quantity);
        session.setAttribute(SESSION_CART_KEY, cart);
    }

    public void removeFromCart(HttpSession session, Long foodId) {
        Cart cart = getCart(session);
        cart.removeItem(foodId);
        session.setAttribute(SESSION_CART_KEY, cart);
    }

    public void clearCart(HttpSession session) {
        Cart cart = getCart(session);
        cart.clear();
        session.setAttribute(SESSION_CART_KEY, cart);
    }
}
