package com.canteen.campuscanteen.repository;

import com.canteen.campuscanteen.model.Order;
import com.canteen.campuscanteen.model.OrderStatus;
import com.canteen.campuscanteen.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByTokenNumber(String tokenNumber);

    List<Order> findByStudentOrderByOrderTimeDesc(Student student);

    List<Order> findByStatusOrderByOrderTimeAsc(OrderStatus status);

    List<Order> findAllByOrderByOrderTimeDesc();

    List<Order> findByCanteenOrderByOrderTimeDesc(String canteen);

    List<Order> findByCanteenAndStatusOrderByOrderTimeAsc(String canteen, OrderStatus status);

    List<Order> findByOrderGroupId(String orderGroupId);

    long countByStatus(OrderStatus status);

    long countByStatusAndCanteen(OrderStatus status, String canteen);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Order o WHERE o.status NOT IN ('CANCELLED', 'AWAITING_PAYMENT')")
    double calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Order o WHERE o.canteen = :canteen AND o.status NOT IN ('CANCELLED', 'AWAITING_PAYMENT')")
    double calculateTotalRevenueByCanteen(@Param("canteen") String canteen);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status NOT IN ('CANCELLED', 'AWAITING_PAYMENT')")
    long countValidOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.canteen = :canteen AND o.status NOT IN ('CANCELLED', 'AWAITING_PAYMENT')")
    long countValidOrdersByCanteen(@Param("canteen") String canteen);
}
