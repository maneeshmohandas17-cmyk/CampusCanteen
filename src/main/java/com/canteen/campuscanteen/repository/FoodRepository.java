package com.canteen.campuscanteen.repository;

import com.canteen.campuscanteen.model.Food;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    List<Food> findByCategory(String category);

    List<Food> findByCategoryAndAvailableTrue(String category);

    List<Food> findByAvailableTrue();

    List<Food> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

    List<Food> findByCategoryIgnoreCase(String category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Food f WHERE f.foodId = :id")
    Optional<Food> findByIdForUpdate(@Param("id") Long id);
}
