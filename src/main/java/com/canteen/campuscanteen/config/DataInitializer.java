package com.canteen.campuscanteen.config;

import com.canteen.campuscanteen.model.Canteen;
import com.canteen.campuscanteen.model.Food;
import com.canteen.campuscanteen.model.Student;
import com.canteen.campuscanteen.repository.CanteenRepository;
import com.canteen.campuscanteen.repository.FoodRepository;
import com.canteen.campuscanteen.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final FoodRepository foodRepository;
    private final StudentRepository studentRepository;
    private final CanteenRepository canteenRepository;

    public DataInitializer(FoodRepository foodRepository,
                           StudentRepository studentRepository,
                           CanteenRepository canteenRepository) {
        this.foodRepository = foodRepository;
        this.studentRepository = studentRepository;
        this.canteenRepository = canteenRepository;
    }

    private static Set<String> getCanteensForFood(String foodName) {
        return switch (foodName) {
            case "Chicken Biryani" -> new LinkedHashSet<>(List.of("Canteen1"));
            case "Crispy Masala Dosa" -> new LinkedHashSet<>(List.of("Canteen2"));
            case "Veg Fried Rice" -> new LinkedHashSet<>(List.of("Canteen3"));
            case "Grilled Veg Cheese Sandwich" -> new LinkedHashSet<>(List.of("Canteen1", "Canteen2"));
            case "Hot Masala Chai" -> new LinkedHashSet<>(List.of("Canteen1", "Canteen2", "Canteen3"));
            case "South Indian Filter Coffee" -> new LinkedHashSet<>(List.of("Canteen2", "Canteen3"));
            case "Crispy Samosa (2 Pcs)" -> new LinkedHashSet<>(List.of("Canteen1", "Canteen3"));
            case "Chilled Cold Coffee" -> new LinkedHashSet<>(List.of("Canteen1", "Canteen2"));
            case "Paneer Butter Masala Bowl" -> new LinkedHashSet<>(List.of("Canteen1"));
            case "Crispy Chicken Burger" -> new LinkedHashSet<>(List.of("Canteen2"));
            default -> new LinkedHashSet<>(List.of("Canteen1"));
        };
    }

    private static String getImageForFood(String foodName) {
        return switch (foodName) {
            case "Chicken Biryani"           -> "chicken-biryani.jpg";
            case "Crispy Masala Dosa"        -> "masala-dosa.jpg";
            case "Veg Fried Rice"            -> "veg-friedrice.jpg";
            case "Grilled Veg Cheese Sandwich" -> "veg-sandwich.jpg";
            case "Hot Masala Chai"           -> "tea.jpg";
            case "South Indian Filter Coffee" -> "filter-coffee.jpg";
            case "Crispy Samosa (2 Pcs)"     -> "samosa.jpg";
            case "Chilled Cold Coffee"       -> "cold-coffee.jpg";
            case "Paneer Butter Masala Bowl" -> "paneer-butter-masala.jpg";
            case "Crispy Chicken Burger"     -> "chicken-burger.jpg";
            default -> null;
        };
    }


    @Override
    public void run(String... args) throws Exception {
        // Ensure exactly Canteen1, Canteen2, and Canteen3 exist and are active
        List<String> defaultCanteens = List.of("Canteen1", "Canteen2", "Canteen3");
        for (String cName : defaultCanteens) {
            Canteen c = canteenRepository.findByNameIgnoreCase(cName).orElseGet(() -> new Canteen(cName, true));
            c.setActive(true);
            canteenRepository.save(c);
        }
        // Remove any test/stray canteens not matching the standard 3 canteens
        for (Canteen c : canteenRepository.findAll()) {
            if (!defaultCanteens.contains(c.getName())) {
                canteenRepository.delete(c);
            }
        }
        System.out.println(">> Verified standard 3 canteens: Canteen1, Canteen2, Canteen3.");

        if (foodRepository.count() == 0) {
            List<Food> preparedFoods = Arrays.asList(
                    new Food("Chicken Biryani", "Lunch", 120.0,
                            "Pre-cooked aromatic basmati rice with chicken and raita, hot in display counter.",
                            "chicken-biryani.jpg", false, "Hot Food Counter A", true),

                    new Food("Crispy Masala Dosa", "Breakfast", 50.0,
                            "Potato masala dosa served with coconut chutney & sambar.",
                            "masala-dosa.jpg", true, "Breakfast Counter 1", true),

                    new Food("Veg Fried Rice", "Lunch", 80.0,
                            "Seasoned fried rice with diced vegetables, hot in display counter.",
                            "veg-friedrice.jpg", true, "Hot Food Counter B", true),

                    new Food("Grilled Veg Cheese Sandwich", "Snacks", 45.0,
                            "Grilled cheese sandwich, sealed and ready in snack warmer.",
                            "veg-sandwich.jpg", true, "Snack Warmer Rack", true),

                    new Food("Hot Masala Chai", "Beverages", 15.0,
                            "Freshly brewed ginger-cardamom tea ready at the beverage dispenser.",
                            "tea.jpg", true, "Beverage Dispenser", true),

                    new Food("South Indian Filter Coffee", "Beverages", 20.0,
                            "Hot frothed filter coffee ready for immediate pour.",
                            "filter-coffee.jpg", true, "Beverage Dispenser", true),

                    new Food("Crispy Samosa (2 Pcs)", "Snacks", 30.0,
                            "Flaky samosas kept crisp under the heat lamp, ready to pick up.",
                            "samosa.jpg", true, "Hot Display Shelf", true),

                    new Food("Chilled Cold Coffee", "Beverages", 50.0,
                            "Pre-bottled cold coffee, chilled and ready in refrigerator display.",
                            "cold-coffee.jpg", true, "Beverage Chiller", true),

                    new Food("Paneer Butter Masala Bowl", "Specials", 110.0,
                            "Not on today's prepared counter menu. Cannot be pre-ordered.",
                            "paneer-butter-masala.jpg", true, "Display Counter", false),

                    new Food("Crispy Chicken Burger", "Specials", 95.0,
                            "Not prepared today. Only available on special festival days.",
                            "chicken-burger.jpg", false, "Display Counter", false)
            );

            for (Food food : preparedFoods) {
                food.setCanteens(getCanteensForFood(food.getName()));
            }

            foodRepository.saveAll(preparedFoods);
            System.out.println(">> Seeded " + preparedFoods.size() + " prepared dishes into Canteen Database.");
        } else {
            // Update existing dishes with counter location, availability, canteen mapping, and unique images
            for (Food food : foodRepository.findAll()) {
                if (food.getCounterLocation() == null || food.getCounterLocation().isEmpty()) {
                    food.setCounterLocation("Hot Display Counter");
                }
                // Keep 2 dishes unavailable so students see items that are not prepared today
                if ("Paneer Butter Masala Bowl".equalsIgnoreCase(food.getName()) ||
                    "Crispy Chicken Burger".equalsIgnoreCase(food.getName())) {
                    food.setAvailable(false);
                }
                if (food.getCanteens() == null || food.getCanteens().isEmpty() || food.getCanteens().stream().noneMatch(defaultCanteens::contains)) {
                    food.setCanteens(getCanteensForFood(food.getName()));
                }
                // Fix duplicate images — assign each food its own unique image
                String correctImage = getImageForFood(food.getName());
                if (correctImage != null) {
                    food.setImage(correctImage);
                }
                foodRepository.save(food);
            }
        }

        // Demo students
        if (!studentRepository.existsByRollNumber("CS101")) {
            Student demoStudent = new Student("CS101", "Alex Morgan", "alex.morgan@campus.edu",
                    "password123", "Computer Science", "9876543210");
            studentRepository.save(demoStudent);
        }

        if (!studentRepository.existsByRollNumber("EC202")) {
            Student student2 = new Student("EC202", "Sneha Patel", "sneha.patel@campus.edu",
                    "password123", "Electronics & Comm", "9876543211");
            studentRepository.save(student2);
        }
    }
}
