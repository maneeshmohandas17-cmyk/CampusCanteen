package com.canteen.campuscanteen;

import com.canteen.campuscanteen.controller.AdminController;
import com.canteen.campuscanteen.controller.StudentAuthController;
import com.canteen.campuscanteen.model.*;
import com.canteen.campuscanteen.repository.CanteenRepository;
import com.canteen.campuscanteen.repository.FoodRepository;
import com.canteen.campuscanteen.repository.OrderRepository;
import com.canteen.campuscanteen.repository.StudentRepository;
import com.canteen.campuscanteen.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class FlowIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CanteenRepository canteenRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    private Student testStudent;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testStudent = studentRepository.findById("CS101").orElseGet(() -> {
            Student s = new Student("CS101", "Alex Morgan", "alex.morgan@campus.edu",
                    "password123", "Computer Science", "9876543210");
            return studentRepository.save(s);
        });
    }

    @Test
    void testHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("featuredFoods", "categories", "canteens"));
    }

    @Test
    void testMenuPageFilters() throws Exception {
        // All menu
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attributeExists("foods", "categories", "canteens"));

        // Canteen filter
        mockMvc.perform(get("/menu").param("canteen", "Canteen1"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attribute("selectedCanteen", "Canteen1"));

        // Category filter
        mockMvc.perform(get("/menu").param("category", "Breakfast"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attribute("selectedCategory", "Breakfast"));

        // Search filter
        mockMvc.perform(get("/menu").param("search", "Biryani"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attribute("searchQuery", "Biryani"));

        // Veg only filter
        mockMvc.perform(get("/menu").param("vegOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attribute("vegOnly", true));
    }

    @Test
    void testStudentRegistrationAndLogin() throws Exception {
        String roll = "TEST" + System.currentTimeMillis();
        // Register new student
        mockMvc.perform(post("/register")
                        .param("rollNumber", roll)
                        .param("name", "Test User")
                        .param("email", roll + "@campus.edu")
                        .param("department", "Computer Science")
                        .param("phone", "9876543210")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/menu"));

        assertTrue(studentRepository.existsByRollNumber(roll));

        // Duplicate registration should fail gracefully
        mockMvc.perform(post("/register")
                        .param("rollNumber", roll)
                        .param("name", "Test User")
                        .param("email", roll + "@campus.edu")
                        .param("department", "Computer Science")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("error"));

        // Login with correct credentials
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login")
                        .session(session)
                        .param("rollNumber", roll)
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/menu"));

        assertNotNull(session.getAttribute(StudentAuthController.SESSION_STUDENT));

        // Login with wrong credentials
        mockMvc.perform(post("/login")
                        .param("rollNumber", roll)
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));

        // Logout
        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertNull(session.getAttribute(StudentAuthController.SESSION_STUDENT));
    }

    @Test
    void testCartOperations() throws Exception {
        Food biryani = foodRepository.findAll().stream()
                .filter(f -> "Chicken Biryani".equalsIgnoreCase(f.getName()))
                .findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();

        // Add to cart
        mockMvc.perform(post("/cart/add")
                        .session(session)
                        .param("foodId", biryani.getFoodId().toString())
                        .param("canteen", "Canteen1")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection());

        Cart cart = cartService.getCart(session);
        assertEquals(2, cart.getTotalCount());

        // View cart
        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("cart", "cartCount"));

        // Update quantity
        mockMvc.perform(post("/cart/update")
                        .session(session)
                        .param("foodId", biryani.getFoodId().toString())
                        .param("canteen", "Canteen1")
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        cart = cartService.getCart(session);
        assertEquals(3, cart.getTotalCount());

        // Remove item
        mockMvc.perform(post("/cart/remove")
                        .session(session)
                        .param("foodId", biryani.getFoodId().toString())
                        .param("canteen", "Canteen1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        cart = cartService.getCart(session);
        assertTrue(cart.isEmpty());
    }

    @Test
    void testOrderPlacementAndMultiCanteenRouting() throws Exception {
        Food biryani = foodRepository.findAll().stream()
                .filter(f -> "Chicken Biryani".equalsIgnoreCase(f.getName()))
                .findFirst().orElseThrow();
        Food dosa = foodRepository.findAll().stream()
                .filter(f -> "Crispy Masala Dosa".equalsIgnoreCase(f.getName()))
                .findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(StudentAuthController.SESSION_STUDENT, testStudent);

        // Add Biryani (Canteen1) and Dosa (Canteen2) to cart
        cartService.addToCart(session, biryani.getFoodId(), 1, "Canteen1");
        cartService.addToCart(session, dosa.getFoodId(), 1, "Canteen2");

        Cart cart = cartService.getCart(session);
        assertEquals(2, cart.getTotalCount());

        // Place Counter Order
        mockMvc.perform(post("/order/place")
                        .session(session)
                        .param("paymentMethod", "PAY_AT_COUNTER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/order/TK-*"))
                .andExpect(flash().attribute("orderSuccess", true))
                .andExpect(flash().attributeExists("info")); // Multi-canteen split message

        // Cart must now be cleared
        assertTrue(cartService.getCart(session).isEmpty());
    }

    @Test
    void testOnlinePaymentFlowSuccessAndFailure() throws Exception {
        Food chai = foodRepository.findAll().stream()
                .filter(f -> "Hot Masala Chai".equalsIgnoreCase(f.getName()))
                .findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(StudentAuthController.SESSION_STUDENT, testStudent);

        cartService.addToCart(session, chai.getFoodId(), 2, "Canteen1");

        // Place order with UPI_ONLINE -> should redirect to payment gateway
        mockMvc.perform(post("/order/place")
                        .session(session)
                        .param("paymentMethod", "UPI_ONLINE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/payment/TK-*"));

        List<Order> studentOrders = orderRepository.findByStudentOrderByOrderTimeDesc(testStudent);
        Order onlineOrder = studentOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.AWAITING_PAYMENT)
                .findFirst().orElseThrow();

        String token = onlineOrder.getTokenNumber();

        // View payment gateway
        mockMvc.perform(get("/payment/" + token).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-gateway"))
                .andExpect(model().attributeExists("order", "totalPayable"));

        // Simulate failed payment (contains "fail")
        mockMvc.perform(post("/payment/" + token + "/process")
                        .session(session)
                        .param("upiOrCardInput", "fail@demo")
                        .param("paymentType", "UPI_ONLINE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment/" + token))
                .andExpect(flash().attributeExists("paymentError"));

        Order afterFail = orderRepository.findByTokenNumber(token).orElseThrow();
        assertEquals(OrderStatus.AWAITING_PAYMENT, afterFail.getStatus());
        assertEquals("FAILED", afterFail.getPaymentStatus());

        // Simulate successful payment
        mockMvc.perform(post("/payment/" + token + "/process")
                        .session(session)
                        .param("upiOrCardInput", "student@okaxis")
                        .param("paymentType", "UPI_ONLINE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/" + token))
                .andExpect(flash().attribute("orderSuccess", true));

        Order afterSuccess = orderRepository.findByTokenNumber(token).orElseThrow();
        assertEquals(OrderStatus.READY_FOR_PICKUP, afterSuccess.getStatus());
        assertEquals("PAID", afterSuccess.getPaymentStatus());
        assertTrue(afterSuccess.isStockDeducted());

        // View Order Status page
        mockMvc.perform(get("/order/" + token).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("order-status"))
                .andExpect(model().attributeExists("order"));

        // View Printable Token page
        mockMvc.perform(get("/order/" + token + "/print").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("print-token"))
                .andExpect(model().attributeExists("order"));
    }

    @Test
    void testAdminAndCanteenStaffLoginAndManagement() throws Exception {
        // Staff Login Page
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));

        // Central Admin Login
        MockHttpSession adminSession = new MockHttpSession();
        mockMvc.perform(post("/admin/login")
                        .session(adminSession)
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        assertEquals("admin", adminSession.getAttribute(AdminController.SESSION_ADMIN));
        assertNull(adminSession.getAttribute(AdminController.SESSION_STAFF_CANTEEN));

        // Central Admin views orders
        mockMvc.perform(get("/admin/orders").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders"))
                .andExpect(model().attributeExists("orders", "canteens"));

        // Central Admin views menu
        mockMvc.perform(get("/admin/menu").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/menu"))
                .andExpect(model().attributeExists("foods", "canteens"));

        // Central Admin views canteens
        mockMvc.perform(get("/admin/canteens").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/canteens"))
                .andExpect(model().attributeExists("canteens"));

        // Canteen1 Staff Login
        MockHttpSession canteen1Session = new MockHttpSession();
        mockMvc.perform(post("/admin/login")
                        .session(canteen1Session)
                        .param("username", "Canteen1")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        assertEquals("Canteen1", canteen1Session.getAttribute(AdminController.SESSION_STAFF_CANTEEN));

        // Canteen1 staff redirected away from canteens management
        mockMvc.perform(get("/admin/canteens").session(canteen1Session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        // Canteen1 staff can view orders specifically for Canteen1
        mockMvc.perform(get("/admin/orders").session(canteen1Session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders"))
                .andExpect(model().attribute("staffCanteen", "Canteen1"));
    }

    @Test
    void testUnpaidOrderCannotPrintSlip() throws Exception {
        Food chai = foodRepository.findAll().stream()
                .filter(f -> "Hot Masala Chai".equalsIgnoreCase(f.getName()))
                .findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(StudentAuthController.SESSION_STUDENT, testStudent);

        cartService.addToCart(session, chai.getFoodId(), 1, "Canteen1");

        mockMvc.perform(post("/order/place")
                        .session(session)
                        .param("paymentMethod", "UPI_ONLINE"))
                .andExpect(status().is3xxRedirection());

        List<Order> studentOrders = orderRepository.findByStudentOrderByOrderTimeDesc(testStudent);
        Order unpaidOrder = studentOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.AWAITING_PAYMENT)
                .findFirst().orElseThrow();

        // Accessing print slip while unpaid must redirect to payment page
        mockMvc.perform(get("/order/" + unpaidOrder.getTokenNumber() + "/print").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment/" + unpaidOrder.getTokenNumber()));
    }

    @Test
    void testCanteenManagementAndStockCleanup() throws Exception {
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute(AdminController.SESSION_ADMIN, "admin");

        String newCanteenName = "TestCanteen" + System.currentTimeMillis();

        // 1. Add Canteen
        mockMvc.perform(post("/admin/canteens/add")
                        .session(adminSession)
                        .param("name", newCanteenName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/canteens"));

        Canteen added = canteenRepository.findByNameIgnoreCase(newCanteenName).orElseThrow();
        assertTrue(added.isActive());

        // 2. Add food dish for this canteen
        Food testFood = new Food("Test Food For " + newCanteenName, "Snacks", 40.0, "Desc", "tea.jpg", true, "Counter", true);
        testFood.setStockForCanteen(newCanteenName, 15);
        testFood = foodRepository.save(testFood);

        assertTrue(testFood.getCanteens().contains(newCanteenName));
        assertEquals(15, testFood.getStockForCanteen(newCanteenName));

        // 3. Delete Canteen
        mockMvc.perform(post("/admin/canteens/delete/" + added.getId())
                        .session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/canteens"));

        assertFalse(canteenRepository.existsById(added.getId()));

        // Verify food cleaned up both canteens set AND canteenStock map
        Food freshFood = foodRepository.findById(testFood.getFoodId()).orElseThrow();
        assertFalse(freshFood.getCanteens().contains(newCanteenName));
        assertEquals(0, freshFood.getStockForCanteen(newCanteenName));
        assertEquals(0, freshFood.getTotalStock());
        assertFalse(freshFood.isAvailable());
    }

    @Test
    void testOrderStatusUpdateAndCancellationByStaff() throws Exception {
        Food samosa = foodRepository.findAll().stream()
                .filter(f -> "Crispy Samosa (2 Pcs)".equalsIgnoreCase(f.getName()))
                .findFirst().orElseThrow();

        int initialStock = samosa.getStockForCanteen("Canteen1");
        assertTrue(initialStock >= 2);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(StudentAuthController.SESSION_STUDENT, testStudent);

        cartService.addToCart(session, samosa.getFoodId(), 2, "Canteen1");

        mockMvc.perform(post("/order/place")
                        .session(session)
                        .param("paymentMethod", "PAY_AT_COUNTER"))
                .andExpect(status().is3xxRedirection());

        Order placedOrder = orderRepository.findByStudentOrderByOrderTimeDesc(testStudent).get(0);
        assertEquals("PAY_ON_PICKUP", placedOrder.getPaymentStatus());
        assertEquals(OrderStatus.READY_FOR_PICKUP, placedOrder.getStatus());
        assertTrue(placedOrder.isStockDeducted());

        // Staff completes order -> paymentStatus becomes PAID
        MockHttpSession staffSession = new MockHttpSession();
        staffSession.setAttribute(AdminController.SESSION_ADMIN, "Canteen1");
        staffSession.setAttribute(AdminController.SESSION_STAFF_CANTEEN, "Canteen1");

        mockMvc.perform(post("/admin/orders/" + placedOrder.getOrderId() + "/status")
                        .session(staffSession)
                        .param("newStatus", "COMPLETED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        Order completedOrder = orderRepository.findById(placedOrder.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
        assertEquals("PAID", completedOrder.getPaymentStatus());

        // Now place another order and cancel it -> verify paymentStatus is CANCELLED and stock is restored
        cartService.addToCart(session, samosa.getFoodId(), 2, "Canteen1");
        mockMvc.perform(post("/order/place")
                        .session(session)
                        .param("paymentMethod", "PAY_AT_COUNTER"))
                .andExpect(status().is3xxRedirection());

        Order toCancel = orderRepository.findByStudentOrderByOrderTimeDesc(testStudent).get(0);

        Food foodBeforeCancel = foodRepository.findById(samosa.getFoodId()).orElseThrow();
        int stockBeforeCancel = foodBeforeCancel.getStockForCanteen("Canteen1");

        mockMvc.perform(post("/admin/orders/" + toCancel.getOrderId() + "/status")
                        .session(staffSession)
                        .param("newStatus", "CANCELLED"))
                .andExpect(status().is3xxRedirection());

        Order cancelledOrder = orderRepository.findById(toCancel.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
        assertEquals("CANCELLED", cancelledOrder.getPaymentStatus());

        Food foodAfterCancel = foodRepository.findById(samosa.getFoodId()).orElseThrow();
        assertEquals(stockBeforeCancel + 2, foodAfterCancel.getStockForCanteen("Canteen1"), "Stock must be restored on cancellation");
    }

    @Test
    void testCartReflectsUpdatedDatabaseStock() {
        Food food = new Food("Test Stock Live Refresh", "Snacks", 25.0, "Desc", "tea.jpg", true, "Counter", true);
        food.setStockForCanteen("Canteen1", 20);
        food = foodRepository.save(food);

        MockHttpSession session = new MockHttpSession();
        cartService.addToCart(session, food.getFoodId(), 2, "Canteen1");

        Cart cartBefore = cartService.getCart(session);
        assertEquals(20, cartBefore.getItems().get(0).getFood().getStockForCanteen("Canteen1"));

        // Admin changes stock in DB to 5
        food.setStockForCanteen("Canteen1", 5);
        foodRepository.save(food);

        // Fetching cart again must reflect the fresh stock of 5
        Cart cartAfter = cartService.getCart(session);
        assertEquals(5, cartAfter.getItems().get(0).getFood().getStockForCanteen("Canteen1"));
    }
}

