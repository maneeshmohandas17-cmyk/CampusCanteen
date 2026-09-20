package com.canteen.campuscanteen.model;

import jakarta.persistence.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "food_canteen_stock", joinColumns = @JoinColumn(name = "food_id"))
    @MapKeyColumn(name = "canteen_name")
    @Column(name = "stock_quantity")
    private Map<String, Integer> canteenStock = new LinkedHashMap<>();

    // Transient field used only to capture the initial quantity entered in the Add Food form
    @Transient
    private Integer availableQuantity = 20;

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

    public Map<String, Integer> getCanteenStock() {
        return canteenStock;
    }

    public void setCanteenStock(Map<String, Integer> canteenStock) {
        this.canteenStock = new LinkedHashMap<>();
        if (canteenStock != null) {
            this.canteenStock.putAll(canteenStock);
        }
        syncAvailability();
    }

    public int getStockForCanteen(String canteen) {
        if (canteenStock == null || canteen == null) {
            return 0;
        }
        return canteenStock.getOrDefault(canteen, 0);
    }

    public void setStockForCanteen(String canteen, int quantity) {
        if (canteen == null) return;
        if (this.canteenStock == null) {
            this.canteenStock = new LinkedHashMap<>();
        }
        int validQty = Math.max(0, quantity);
        this.canteenStock.put(canteen, validQty);
        if (this.canteens == null) {
            this.canteens = new LinkedHashSet<>();
        }
        this.canteens.add(canteen);
        syncAvailability();
    }

    public void decreaseStock(String canteen, int amount) {
        if (canteen == null || amount <= 0) return;
        int current = getStockForCanteen(canteen);
        setStockForCanteen(canteen, Math.max(0, current - amount));
    }

    public void increaseStock(String canteen, int amount) {
        if (canteen == null || amount <= 0) return;
        int current = getStockForCanteen(canteen);
        setStockForCanteen(canteen, current + amount);
    }

    public int getTotalStock() {
        if (canteenStock == null || canteenStock.isEmpty()) {
            return 0;
        }
        return canteenStock.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void syncAvailability() {
        if (getTotalStock() == 0) {
            this.available = false;
        } else {
            this.available = true;
        }
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public boolean isAvailableInCanteen(String canteen) {
        return available && canteens != null && canteens.contains(canteen) && getStockForCanteen(canteen) > 0;
    }

    public boolean hasAnyActiveCanteen(java.util.Collection<String> activeCanteens) {
        if (canteens == null || activeCanteens == null || canteens.isEmpty()) return false;
        return canteens.stream().anyMatch(c -> activeCanteens.contains(c) && getStockForCanteen(c) > 0);
    }

    public boolean hasActiveCanteen(java.util.Collection<String> activeCanteens) {
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
