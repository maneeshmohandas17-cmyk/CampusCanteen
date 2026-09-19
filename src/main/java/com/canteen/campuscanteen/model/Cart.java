package com.canteen.campuscanteen.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cart {

    private final Map<String, CartItem> items = new LinkedHashMap<>();

    public void addItem(Food food, int quantity, String canteen) {
        if (food == null || quantity <= 0) return;
        String key = food.getFoodId() + ":" + canteen;
        if (items.containsKey(key)) {
            CartItem existing = items.get(key);
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.put(key, new CartItem(food, quantity, canteen));
        }
    }

    public void updateQuantity(Long foodId, String canteen, int quantity) {
        String key = foodId + ":" + canteen;
        if (items.containsKey(key)) {
            if (quantity <= 0) {
                items.remove(key);
            } else {
                items.get(key).setQuantity(quantity);
            }
        }
    }

    public void removeItem(Long foodId, String canteen) {
        items.remove(foodId + ":" + canteen);
    }

    public void clear() {
        items.clear();
    }

    public void removeInactiveCanteens(java.util.function.Predicate<String> isValidCanteen) {
        if (isValidCanteen == null) return;
        items.entrySet().removeIf(entry -> !isValidCanteen.test(entry.getValue().getCanteen()));
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
