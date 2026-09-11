package com.canteen.campuscanteen.config;

import com.canteen.campuscanteen.model.Food;
import com.canteen.campuscanteen.model.Student;
import com.canteen.campuscanteen.repository.FoodRepository;
import com.canteen.campuscanteen.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final FoodRepository foodRepository;
    private final StudentRepository studentRepository;

    public DataInitializer(FoodRepository foodRepository, StudentRepository studentRepository) {
        this.foodRepository = foodRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
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
                            "tea.jpg", true, "Beverage Dispenser", true),

                    new Food("Crispy Samosa (2 Pcs)", "Snacks", 30.0,
                            "Flaky samosas kept crisp under the heat lamp, ready to pick up.",
                            "veg-sandwich.jpg", true, "Hot Display Shelf", true),

                    new Food("Chilled Cold Coffee", "Beverages", 50.0,
                            "Pre-bottled cold coffee, chilled and ready in refrigerator display.",
                            "tea.jpg", true, "Beverage Chiller", true),

                    new Food("Paneer Butter Masala Bowl", "Specials", 110.0,
                            "Not on today's prepared counter menu. Cannot be pre-ordered.",
                            "veg-friedrice.jpg", true, "Display Counter", false),

                    new Food("Crispy Chicken Burger", "Specials", 95.0,
                            "Not prepared today. Only available on special festival days.",
                            "chicken-biryani.jpg", false, "Display Counter", false)
            );

            foodRepository.saveAll(preparedFoods);
            System.out.println(">> Seeded " + preparedFoods.size() + " prepared dishes into Canteen Database.");
        } else {
            // Update existing dishes with counter location and availability without violating FK constraints
            for (Food food : foodRepository.findAll()) {
                if (food.getCounterLocation() == null || food.getCounterLocation().isEmpty()) {
                    food.setCounterLocation("Hot Display Counter");
                }
                // Keep 2 dishes unavailable so students see items that are not prepared today
                if ("Paneer Butter Masala Bowl".equalsIgnoreCase(food.getName()) ||
                    "Crispy Chicken Burger".equalsIgnoreCase(food.getName())) {
                    food.setAvailable(false);
                }
                foodRepository.save(food);
            }
        }

        // Demo students
        if (!studentRepository.existsByRollNumber("CS101")) {
            Student demoStudent = new Student("CS101", "Alex Morgan", "alex.morgan@campus.edu",
                    "password123", "Computer Science", "9876543210");
            demoStudent.setWalletBalance(500.0);
            studentRepository.save(demoStudent);
        }

        if (!studentRepository.existsByRollNumber("EC202")) {
            Student student2 = new Student("EC202", "Sneha Patel", "sneha.patel@campus.edu",
                    "password123", "Electronics & Comm", "9876543211");
            student2.setWalletBalance(350.0);
            studentRepository.save(student2);
        }
    }
}
