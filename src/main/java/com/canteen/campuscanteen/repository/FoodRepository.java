package com.canteen.campuscanteen.repository;

import com.canteen.campuscanteen.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    List<Food> findByCategory(String category);

    List<Food> findByCategoryAndAvailableTrue(String category);

    List<Food> findByAvailableTrue();

    List<Food> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

    List<Food> findByCategoryIgnoreCase(String category);
}
