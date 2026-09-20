package com.canteen.campuscanteen;

import com.canteen.campuscanteen.model.*;
import com.canteen.campuscanteen.repository.CanteenRepository;
import com.canteen.campuscanteen.repository.FoodRepository;
import com.canteen.campuscanteen.repository.OrderRepository;
import com.canteen.campuscanteen.repository.StudentRepository;
import com.canteen.campuscanteen.service.CartService;
import com.canteen.campuscanteen.service.FoodService;
import com.canteen.campuscanteen.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class StockManagementTests {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private FoodService foodService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CanteenRepository canteenRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        try {
            jdbcTemplate.execute("ALTER TABLE canteen_orders ADD COLUMN IF NOT EXISTS stock_deducted BOOLEAN DEFAULT FALSE");
        } catch (Exception ignored) {}

        testStudent = studentRepository.findById("CS101").orElseGet(() -> {
            Student s = new Student("CS101", "Alex Morgan", "alex.morgan@campus.edu",
                    "password123", "Computer Science", "9876543210");
            return studentRepository.save(s);
        });
    }

    @Test
    void testExistingFoodItemsHaveStartingQuantitiesPerCanteen() {
        Food biryani = foodRepository.findAll().stream()
                .filter(f -> "Chicken Biryani".equalsIgnoreCase(f.getName()))
                .findFirst().orElseThrow();

        assertTrue(biryani.getStockForCanteen("Canteen1") > 0, "Canteen1 must have practical starting stock");
        assertEquals(0, biryani.getStockForCanteen("Canteen2"), "Canteen2 should have 0 stock if not offered");

        Food chai = foodRepository.findAll().stream()
                .filter(f -> "Hot Masala Chai".equalsIgnoreCase(f.getName()))
                .findFirst().orElseThrow();

        // Chai is served in Canteen1, Canteen2, and Canteen3
        assertTrue(chai.getStockForCanteen("Canteen1") > 0);
        assertTrue(chai.getStockForCanteen("Canteen2") > 0);
        assertTrue(chai.getStockForCanteen("Canteen3") > 0);
    }

    @Test
    void testAddingToCartDoesNotDecreaseDatabaseStock() {
        Food food = new Food("Test Snack Item", "Snacks", 30.0, "Delicious", "samosa.jpg", true, "Hot Counter", true);
        food.setStockForCanteen("Canteen1", 20);
        food = foodRepository.save(food);

        MockHttpSession session = new MockHttpSession();
        cartService.addToCart(session, food.getFoodId(), 3, "Canteen1");

        // Verify cart has 3 items
        Cart cart = cartService.getCart(session);
        assertEquals(3, cart.getTotalCount());

        // Verify database stock is UNCHANGED (still 20)
        Food fresh = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(20, fresh.getStockForCanteen("Canteen1"), "Adding to cart must NOT decrease database stock!");
    }

    @Test
    void testCounterPaymentDeductsStockImmediatelyUponOrderPlacement() {
        Food food = new Food("Test Counter Food", "Lunch", 60.0, "Ready", "veg-friedrice.jpg", true, "Counter", true);
        food.setStockForCanteen("Canteen1", 10);
        food.setStockForCanteen("Canteen2", 15);
        food = foodRepository.save(food);

        Cart cart = new Cart();
        cart.addItem(food, 4, "Canteen1");

        List<Order> orders = orderService.placeOrders(testStudent, cart, "PAY_AT_COUNTER");
        assertEquals(1, orders.size());
        assertTrue(orders.get(0).isStockDeducted(), "Stock must be marked deducted for counter order");

        Food fresh = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(6, fresh.getStockForCanteen("Canteen1"), "Canteen1 stock must decrease from 10 to 6");
        assertEquals(15, fresh.getStockForCanteen("Canteen2"), "Canteen2 stock must remain unaffected");
    }

    @Test
    void testOnlineDemoPaymentDeductsStockOnlyAfterSuccessfulSettlement() {
        Food food = new Food("Test Online Food", "Lunch", 80.0, "Ready", "veg-friedrice.jpg", true, "Counter", true);
        food.setStockForCanteen("Canteen1", 12);
        food = foodRepository.save(food);

        Cart cart = new Cart();
        cart.addItem(food, 3, "Canteen1");

        // Place order with UPI online
        List<Order> orders = orderService.placeOrders(testStudent, cart, "UPI_ONLINE");
        assertEquals(1, orders.size());
        Order order = orders.get(0);
        assertEquals(OrderStatus.AWAITING_PAYMENT, order.getStatus());
        assertFalse(order.isStockDeducted(), "Stock must NOT be deducted before online payment succeeds");

        // Stock in DB should still be 12
        Food freshBefore = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(12, freshBefore.getStockForCanteen("Canteen1"), "Stock must remain 12 while awaiting payment");

        // Failed payment simulation: stock still not deducted
        orderService.settleOnlinePayment(order.getTokenNumber(), false, "UPI_ONLINE");
        Food freshAfterFail = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(12, freshAfterFail.getStockForCanteen("Canteen1"), "Failed payment must not deduct stock");

        // Successful payment settlement: stock is now deducted
        orderService.settleOnlinePayment(order.getTokenNumber(), true, "UPI_ONLINE");
        Food freshAfterSuccess = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(9, freshAfterSuccess.getStockForCanteen("Canteen1"), "Successful payment settlement must deduct stock from 12 to 9");
    }

    @Test
    void testCancelledOrderRestoresStockExactlyOnce() {
        Food food = new Food("Test Cancel Dish", "Snacks", 25.0, "Ready", "samosa.jpg", true, "Counter", true);
        food.setStockForCanteen("Canteen1", 8);
        food = foodRepository.save(food);

        Cart cart = new Cart();
        cart.addItem(food, 3, "Canteen1");

        List<Order> orders = orderService.placeOrders(testStudent, cart, "PAY_AT_COUNTER");
        Order order = orders.get(0);

        Food afterOrder = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(5, afterOrder.getStockForCanteen("Canteen1"));

        // Admin cancels the order
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELLED);
        Food afterCancel = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(8, afterCancel.getStockForCanteen("Canteen1"), "Cancelled order must restore deducted stock");

        // Cancelling again must NOT double-restore stock
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELLED);
        Food afterSecondCancel = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(8, afterSecondCancel.getStockForCanteen("Canteen1"), "Double cancel must not restore stock again");
    }

    @Test
    void testZeroStockMarksUnavailableAndRestockMarksAvailable() {
        Food food = new Food("Test Stock Availability", "Snacks", 20.0, "Quick", "tea.jpg", true, "Counter", true);
        food.setStockForCanteen("Canteen1", 2);
        food = foodRepository.save(food);
        assertTrue(food.isAvailable());

        // Drain stock to 0
        foodService.updateStock(food.getFoodId(), "Canteen1", 0);
        Food zeroFood = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(0, zeroFood.getStockForCanteen("Canteen1"));
        assertFalse(zeroFood.isAvailable(), "0 stock must automatically mark available = false");
        assertFalse(zeroFood.isAvailableInCanteen("Canteen1"));

        // Restock above 0
        foodService.updateStock(food.getFoodId(), "Canteen1", 10);
        Food restocked = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(10, restocked.getStockForCanteen("Canteen1"));
        assertTrue(restocked.isAvailable(), "Restocking above 0 must automatically mark available = true");
        assertTrue(restocked.isAvailableInCanteen("Canteen1"));
    }

    @Test
    void testStudentCannotOrderMoreThanAvailableQuantity() {
        Food food = new Food("Test Scarcity Item", "Lunch", 50.0, "Limited", "chicken-biryani.jpg", false, "Counter", true);
        food.setStockForCanteen("Canteen1", 2);
        food = foodRepository.save(food);

        Cart cart = new Cart();
        cart.addItem(food, 5, "Canteen1"); // Attempt to order 5 when only 2 available

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrders(testStudent, cart, "PAY_AT_COUNTER");
        });

        assertTrue(exception.getMessage().contains("remaining") || exception.getMessage().contains("available"),
                "Should provide clear error indicating insufficient stock");

        // Verify stock remained 2
        Food fresh = foodRepository.findById(food.getFoodId()).orElseThrow();
        assertEquals(2, fresh.getStockForCanteen("Canteen1"));
    }

    @Test
    void testConcurrencyPreventsNegativeStock() throws InterruptedException {
        final Food food = new Food("Concurrent Test Dish", "Snacks", 10.0, "Limited", "tea.jpg", true, "Counter", true);
        food.setStockForCanteen("Canteen1", 5);
        final Food savedFood = foodRepository.save(food);

        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Cart cart = new Cart();
                    cart.addItem(savedFood, 1, "Canteen1");
                    orderService.placeOrders(testStudent, cart, "PAY_AT_COUNTER");
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all 10 threads concurrently
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        Food fresh = foodRepository.findById(savedFood.getFoodId()).orElseThrow();
        assertEquals(0, fresh.getStockForCanteen("Canteen1"), "Stock should reach exactly 0, never negative");
        assertEquals(5, successCount.get(), "Exactly 5 orders should succeed for 5 available stock");
        assertEquals(5, failCount.get(), "Remaining 5 concurrent attempts must fail gracefully");
    }
}
