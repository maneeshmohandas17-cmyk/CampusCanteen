package com.canteen.campuscanteen.repository;

import com.canteen.campuscanteen.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteByFood_FoodId(Long foodId);
}
