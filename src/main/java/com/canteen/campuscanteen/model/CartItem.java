package com.canteen.campuscanteen.model;

public class CartItem {

    private Food food;
    private int quantity;
    private String canteen;

    public CartItem() {
    }

    public CartItem(Food food, int quantity, String canteen) {
        this.food = food;
        this.quantity = quantity;
        this.canteen = canteen;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCanteen() {
        return canteen;
    }

    public double getSubtotal() {
        if (food == null) return 0.0;
        return food.getPrice() * quantity;
    }
}
