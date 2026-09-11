package com.canteen.campuscanteen.service;

import com.canteen.campuscanteen.model.*;
import com.canteen.campuscanteen.repository.OrderRepository;
import com.canteen.campuscanteen.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final StudentRepository studentRepository;
    private static final AtomicInteger TOKEN_COUNTER = new AtomicInteger(100);

    public OrderService(OrderRepository orderRepository, StudentRepository studentRepository) {
        this.orderRepository = orderRepository;
        this.studentRepository = studentRepository;
    }

    public synchronized String generateTokenNumber() {
        int next = TOKEN_COUNTER.incrementAndGet();
        return "TK-" + next;
    }

    @Transactional
    public Order placeOrder(Student student, Cart cart, String pickupSlot,
                           String paymentMethod, String specialInstructions) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cannot reserve with an empty tray");
        }

        if (student == null) {
            throw new IllegalArgumentException("Student must be logged in to reserve food");
        }

        // Strict validation: Food must be currently prepared & available on counter
        for (CartItem item : cart.getItems()) {
            if (!item.getFood().isAvailable()) {
                throw new IllegalArgumentException("Sorry! '" + item.getFood().getName() + "' is not prepared or available in the counter today.");
            }
        }

        double total = cart.getTotalAmount();
        boolean isOnlineGateway = "UPI_ONLINE".equalsIgnoreCase(paymentMethod) || "CARD_ONLINE".equalsIgnoreCase(paymentMethod);

        // Handle wallet payment (deducted immediately - it's already the student's own balance)
        String paymentStatus;
        OrderStatus initialStatus;
        if ("STUDENT_WALLET".equalsIgnoreCase(paymentMethod)) {
            if (student.getWalletBalance() < total) {
                throw new IllegalArgumentException("Insufficient wallet balance. Please choose another payment method.");
            }
            student.setWalletBalance(student.getWalletBalance() - total);
            studentRepository.save(student);
            paymentStatus = "PAID";
            initialStatus = OrderStatus.READY_FOR_PICKUP; // food is pre-prepared
        } else if (isOnlineGateway) {
            // Held at the payment gateway step until the student completes the mock checkout
            paymentStatus = "PENDING_PAYMENT";
            initialStatus = OrderStatus.AWAITING_PAYMENT;
        } else {
            paymentStatus = "PAY_ON_PICKUP";
            initialStatus = OrderStatus.READY_FOR_PICKUP; // food is pre-prepared
        }

        String token = generateTokenNumber();

        Order order = new Order();
        order.setTokenNumber(token);
        order.setStudent(student);
        order.setOrderTime(LocalDateTime.now());
        order.setPickupTimeSlot(pickupSlot != null && !pickupSlot.trim().isEmpty() ? pickupSlot : "Immediate Pickup");
        order.setStatus(initialStatus);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "PAY_AT_COUNTER");
        order.setPaymentStatus(paymentStatus);
        order.setTotalAmount(total);
        order.setSpecialInstructions(specialInstructions != null ? specialInstructions.trim() : "");

        for (CartItem item : cart.getItems()) {
            OrderItem orderItem = new OrderItem(order, item.getFood(), item.getQuantity(), item.getFood().getPrice());
            order.addItem(orderItem);
        }

        return orderRepository.save(order);
    }

    /**
     * Called from the mock payment gateway once the student completes (or the demo simulates)
     * the online checkout step. Only orders still AWAITING_PAYMENT can be settled this way.
     */
    @Transactional
    public Order settleOnlinePayment(String tokenNumber, boolean success) {
        Order order = orderRepository.findByTokenNumber(tokenNumber.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Order not found for token: " + tokenNumber));

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            // Already settled (e.g. page refreshed/back button) - just return as-is
            return order;
        }

        if (success) {
            order.setPaymentStatus("PAID");
            order.setStatus(OrderStatus.READY_FOR_PICKUP); // food is pre-prepared
        } else {
            order.setPaymentStatus("FAILED");
            // Leave status as AWAITING_PAYMENT so the student can retry the same order/token
        }
        return orderRepository.save(order);
    }

    public Optional<Order> getOrderByToken(String tokenNumber) {
        if (tokenNumber == null) return Optional.empty();
        return orderRepository.findByTokenNumber(tokenNumber.trim().toUpperCase());
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByStudent(Student student) {
        return orderRepository.findByStudentOrderByOrderTimeDesc(student);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderTimeDesc();
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByOrderTimeAsc(status);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        order.setStatus(newStatus);
        if (newStatus == OrderStatus.COMPLETED && "PAY_ON_PICKUP".equalsIgnoreCase(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
        }
        return orderRepository.save(order);
    }

    public long getCountByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public double getTotalRevenue() {
        return orderRepository.calculateTotalRevenue();
    }

    public long getValidOrderCount() {
        return orderRepository.countValidOrders();
    }
}
