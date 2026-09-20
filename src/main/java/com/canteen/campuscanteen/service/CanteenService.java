package com.canteen.campuscanteen.service;

import com.canteen.campuscanteen.model.Canteen;
import com.canteen.campuscanteen.model.Food;
import com.canteen.campuscanteen.repository.CanteenRepository;
import com.canteen.campuscanteen.repository.FoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CanteenService {

    private final CanteenRepository canteenRepository;
    private final FoodRepository foodRepository;

    public CanteenService(CanteenRepository canteenRepository, FoodRepository foodRepository) {
        this.canteenRepository = canteenRepository;
        this.foodRepository = foodRepository;
    }

    /** All canteen entities (active & inactive), ordered by insertion order. */
    public List<Canteen> getAllCanteens() {
        return canteenRepository.findAllByOrderByIdAsc();
    }

    /** All active canteen entities. */
    public List<Canteen> getAllActiveCanteens() {
        return canteenRepository.findByActiveTrueOrderByIdAsc();
    }

    /** Names of currently active canteens — used for dropdowns, pills, login options, and validation. */
    public List<String> getAllCanteenNames() {
        return canteenRepository.findByActiveTrueOrderByIdAsc()
                .stream()
                .map(Canteen::getName)
                .collect(Collectors.toList());
    }

    /** True if the given name matches an existing active canteen (case-insensitive). */
    public boolean isValidCanteen(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        return canteenRepository.findByNameIgnoreCaseAndActiveTrue(name.trim()).isPresent();
    }

    /**
     * Add a new canteen.
     * @throws IllegalArgumentException if the name is blank or already exists.
     */
    @Transactional
    public Canteen addCanteen(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Canteen name cannot be blank.");
        }
        String trimmed = name.trim();
        if (canteenRepository.existsByNameIgnoreCase(trimmed)) {
            throw new IllegalArgumentException("A canteen named '" + trimmed + "' already exists.");
        }
        return canteenRepository.save(new Canteen(trimmed, true));
    }

    /**
     * Toggle active status of a canteen.
     */
    @Transactional
    public Canteen toggleCanteen(Long id) {
        Canteen canteen = canteenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Canteen not found with id: " + id));
        canteen.setActive(!canteen.isActive());
        return canteenRepository.save(canteen);
    }

    /**
     * Delete a canteen by ID and clean up food associations.
     * @throws IllegalArgumentException if not found.
     */
    @Transactional
    public void deleteCanteen(Long id) {
        Canteen canteen = canteenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Canteen not found with id: " + id));
        String canteenName = canteen.getName();
        canteenRepository.delete(canteen);

        // Remove the deleted canteen from all food menus and canteen stock
        List<Food> foods = foodRepository.findAll();
        for (Food food : foods) {
            boolean modified = false;
            if (food.getCanteens() != null && food.getCanteens().remove(canteenName)) {
                modified = true;
            }
            if (food.getCanteenStock() != null && food.getCanteenStock().remove(canteenName) != null) {
                modified = true;
            }
            if (modified) {
                food.syncAvailability();
                foodRepository.save(food);
            }
        }
    }

    public long countCanteens() {
        return canteenRepository.count();
    }
}
