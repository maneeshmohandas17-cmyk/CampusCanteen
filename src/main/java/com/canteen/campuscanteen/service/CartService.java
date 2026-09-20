package com.canteen.campuscanteen.service;

import com.canteen.campuscanteen.model.Cart;
import com.canteen.campuscanteen.model.Food;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;
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

            // Refresh food entities from database to ensure fresh stock, price, and availability
            List<com.canteen.campuscanteen.model.CartItem> items = new java.util.ArrayList<>(cart.getItems());
            for (com.canteen.campuscanteen.model.CartItem item : items) {
                if (item.getFood() != null && item.getFood().getFoodId() != null) {
                    Optional<Food> freshFood = foodService.getFoodById(item.getFood().getFoodId());
                    if (freshFood.isPresent()) {
                        item.setFood(freshFood.get());
                    } else {
                        cart.removeItem(item.getFood().getFoodId(), item.getCanteen());
                    }
                }
            }
        }
        return cart;
    }

    public void addToCart(HttpSession session, Long foodId, int quantity, String canteen) {
        if (quantity <= 0 || canteen == null || !canteenService.isValidCanteen(canteen)) return;
        Optional<Food> foodOpt = foodService.getFoodById(foodId);
        if (foodOpt.isPresent()) {
            Food food = foodOpt.get();
            if (!food.isAvailable() || !food.getCanteens().contains(canteen)) {
                throw new IllegalArgumentException("Sorry, '" + food.getName() + "' is not available in " + canteen + ".");
            }
            int availableStock = food.getStockForCanteen(canteen);
            if (availableStock <= 0) {
                throw new IllegalArgumentException("Sorry, '" + food.getName() + "' is currently sold out in " + canteen + ".");
            }
            Cart cart = getCart(session);
            int currentInCart = cart.getItemQuantity(foodId, canteen);
            if (currentInCart + quantity > availableStock) {
                int remainingAllowed = Math.max(0, availableStock - currentInCart);
                throw new IllegalArgumentException("Cannot add " + quantity + " portion(s). Only " + remainingAllowed + " more available in " + canteen + ".");
            }
            cart.addItem(food, quantity, canteen);
            session.setAttribute(SESSION_CART_KEY, cart);
        }
    }

    public void updateQuantity(HttpSession session, Long foodId, String canteen, int quantity) {
        Cart cart = getCart(session);
        if (quantity > 0) {
            Optional<Food> foodOpt = foodService.getFoodById(foodId);
            if (foodOpt.isPresent()) {
                int availableStock = foodOpt.get().getStockForCanteen(canteen);
                if (quantity > availableStock) {
                    throw new IllegalArgumentException("Cannot set quantity to " + quantity + ". Only " + availableStock + " available in " + canteen + ".");
                }
            }
        }
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
