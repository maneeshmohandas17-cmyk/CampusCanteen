package com.canteen.campuscanteen.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cart {

    private final Map<Long, CartItem> items = new LinkedHashMap<>();

    public void addItem(Food food, int quantity) {
        if (food == null || quantity <= 0) return;
        if (items.containsKey(food.getFoodId())) {
            CartItem existing = items.get(food.getFoodId());
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.put(food.getFoodId(), new CartItem(food, quantity));
        }
    }

    public void updateQuantity(Long foodId, int quantity) {
        if (items.containsKey(foodId)) {
            if (quantity <= 0) {
                items.remove(foodId);
            } else {
                items.get(foodId).setQuantity(quantity);
            }
        }
    }

    public void removeItem(Long foodId) {
        items.remove(foodId);
    }

    public void clear() {
        items.clear();
    }

    public List<CartItem> getItems() {
        return new ArrayList<>(items.values());
    }

    public int getTotalCount() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double getTotalAmount() {
        return items.values().stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
