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

    public static final List<String> CATEGORIES = Arrays.asList(
            "Breakfast", "Lunch", "Snacks", "Beverages", "Specials"
    );

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
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
        return foodRepository.save(food);
    }

    public boolean toggleAvailability(Long foodId) {
        Optional<Food> opt = foodRepository.findById(foodId);
        if (opt.isPresent()) {
            Food f = opt.get();
            f.setAvailable(!f.isAvailable());
            foodRepository.save(f);
            return f.isAvailable();
        }
        return false;
    }

    public void deleteFood(Long foodId) {
        foodRepository.deleteById(foodId);
    }

    public List<String> getAllCategories() {
        return CATEGORIES;
    }
}
