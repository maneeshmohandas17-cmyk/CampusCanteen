package com.canteen.campuscanteen;

import com.canteen.campuscanteen.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class CampusCanteenApplicationTests {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testOrderRepositoryQueries() {
        assertDoesNotThrow(() -> {
            orderRepository.calculateTotalRevenue();
            orderRepository.calculateTotalRevenueByCanteen("Canteen1");
            orderRepository.countValidOrders();
            orderRepository.countValidOrdersByCanteen("Canteen1");
        });
    }

}

