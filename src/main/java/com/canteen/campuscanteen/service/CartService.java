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
    private final CanteenService canteenService;

    public CartService(FoodService foodService, CanteenService canteenService) {
        this.foodService = foodService;
        this.canteenService = canteenService;
    }

    public Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(SESSION_CART_KEY);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(SESSION_CART_KEY, cart);
        } else {
            // Prune items if their canteen has been removed or disabled
            cart.removeInactiveCanteens(canteenService::isValidCanteen);
        }
        return cart;
    }

    public void addToCart(HttpSession session, Long foodId, int quantity, String canteen) {
        if (quantity <= 0 || canteen == null || !canteenService.isValidCanteen(canteen)) return;
        Optional<Food> foodOpt = foodService.getFoodById(foodId);
        if (foodOpt.isPresent() && foodOpt.get().isAvailable() && foodOpt.get().getCanteens().contains(canteen)) {
            Cart cart = getCart(session);
            cart.addItem(foodOpt.get(), quantity, canteen);
            session.setAttribute(SESSION_CART_KEY, cart);
        }
    }

    public void updateQuantity(HttpSession session, Long foodId, String canteen, int quantity) {
        Cart cart = getCart(session);
        cart.updateQuantity(foodId, canteen, quantity);
        session.setAttribute(SESSION_CART_KEY, cart);
    }

    public void removeFromCart(HttpSession session, Long foodId, String canteen) {
        Cart cart = getCart(session);
        cart.removeItem(foodId, canteen);
        session.setAttribute(SESSION_CART_KEY, cart);
    }

    public void clearCart(HttpSession session) {
        Cart cart = getCart(session);
        cart.clear();
        session.setAttribute(SESSION_CART_KEY, cart);
    }
}
