package com.canteen.campuscanteen.model;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "foods")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long foodId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private double price;

    @Column(length = 1000)
    private String description;

    private String image;

    private boolean isVeg = true;

    private String counterLocation = "Hot Display Counter";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "food_canteens", joinColumns = @JoinColumn(name = "food_id"))
    @Column(name = "canteen_name")
    private Set<String> canteens = new LinkedHashSet<>();

    // Indicates whether this dish is currently prepared and ready on display today
    private boolean available = true;

    public Food() {
    }

    public Food(String name, String category, double price, String description,
                String image, boolean isVeg, String counterLocation, boolean available) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.image = image;
        this.isVeg = isVeg;
        this.counterLocation = counterLocation;
        this.available = available;
    }

    public Long getFoodId() {
        return foodId;
    }

    public void setFoodId(Long foodId) {
        this.foodId = foodId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isVeg() {
        return isVeg;
    }

    public void setVeg(boolean isVeg) {
        this.isVeg = isVeg;
    }

    public String getCounterLocation() {
        return counterLocation;
    }

    public void setCounterLocation(String counterLocation) {
        this.counterLocation = counterLocation;
    }

    public Set<String> getCanteens() {
        return canteens;
    }

    public void setCanteens(Set<String> canteens) {
        this.canteens = new LinkedHashSet<>();
        if (canteens != null) {
            this.canteens.addAll(canteens);
        }
    }

    public boolean isAvailableInCanteen(String canteen) {
        return canteens != null && canteens.contains(canteen);
    }

    public boolean hasAnyActiveCanteen(java.util.Collection<String> activeCanteens) {
        if (canteens == null || activeCanteens == null || canteens.isEmpty()) return false;
        return canteens.stream().anyMatch(activeCanteens::contains);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
