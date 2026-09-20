package com.canteen.campuscanteen.service;

import com.canteen.campuscanteen.model.Food;
import com.canteen.campuscanteen.repository.FoodRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final com.canteen.campuscanteen.repository.OrderItemRepository orderItemRepository;

    public static final List<String> CATEGORIES = Arrays.asList(
            "Breakfast", "Lunch", "Snacks", "Beverages", "Specials"
    );

    public FoodService(FoodRepository foodRepository,
                       com.canteen.campuscanteen.repository.OrderItemRepository orderItemRepository) {
        this.foodRepository = foodRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<Food> getAllFood() {
        return foodRepository.findAll();
    }

    public List<Food> getAllAvailableFood() {
        return foodRepository.findByAvailableTrue();
    }

    public Optional<Food> getFoodById(Long id) {
        return foodRepository.findById(id);
    }

    public List<Food> getFoodByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All")) {
            return foodRepository.findAll();
        }
        return foodRepository.findByCategoryIgnoreCase(category);
    }

    public List<Food> searchFood(String query) {
        if (query == null || query.trim().isEmpty()) {
            return foodRepository.findAll();
        }
        return foodRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query.trim(), query.trim());
    }

    public Food saveFood(Food food) {
        if (food != null) {
            food.syncAvailability();
        }
        return foodRepository.save(food);
    }

    public boolean toggleAvailability(Long foodId) {
        Optional<Food> opt = foodRepository.findById(foodId);
        if (opt.isPresent()) {
            Food f = opt.get();
            boolean newStatus = !f.isAvailable();
            f.setAvailable(newStatus);
            if (newStatus && f.getTotalStock() == 0) {
                // If toggled on while at 0 stock, provide a practical restock for its canteens
                if (f.getCanteens() != null && !f.getCanteens().isEmpty()) {
                    for (String c : f.getCanteens()) {
                        f.setStockForCanteen(c, 15);
                    }
                } else {
                    f.setStockForCanteen("Canteen1", 15);
                }
                f.setAvailable(true);
            }
            foodRepository.save(f);
            return f.isAvailable();
        }
        return false;
    }

    public Food updateStock(Long foodId, String canteen, int quantity) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("Food not found with id: " + foodId));
        food.setStockForCanteen(canteen, quantity);
        return foodRepository.save(food);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteFood(Long foodId) {
        orderItemRepository.deleteByFood_FoodId(foodId);
        foodRepository.deleteById(foodId);
    }

    public List<String> getAllCategories() {
        return CATEGORIES;
    }
}
