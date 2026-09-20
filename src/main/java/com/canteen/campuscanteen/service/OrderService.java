package com.canteen.campuscanteen.service;

import com.canteen.campuscanteen.model.*;
import com.canteen.campuscanteen.repository.OrderRepository;
import com.canteen.campuscanteen.service.CanteenService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CanteenService canteenService;
    private final com.canteen.campuscanteen.repository.FoodRepository foodRepository;
    private static final AtomicInteger TOKEN_COUNTER = new AtomicInteger(100);

    public OrderService(OrderRepository orderRepository,
                        CanteenService canteenService,
                        com.canteen.campuscanteen.repository.FoodRepository foodRepository) {
        this.orderRepository = orderRepository;
        this.canteenService = canteenService;
        this.foodRepository = foodRepository;
    }

    @PostConstruct
    public void initTokenCounter() {
        int max = 100;
        try {
            List<Order> orders = orderRepository.findAll();
            for (Order o : orders) {
                String tok = o.getTokenNumber();
                if (tok != null && tok.toUpperCase().startsWith("TK-")) {
                    try {
                        int num = Integer.parseInt(tok.substring(3).trim());
                        if (num > max) {
                            max = num;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        TOKEN_COUNTER.set(max);
    }

    public synchronized String generateTokenNumber() {
        while (true) {
            int next = TOKEN_COUNTER.incrementAndGet();
            String candidate = "TK-" + next;
            if (orderRepository.findByTokenNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
    }

    @Transactional
    public List<Order> placeOrders(Student student, Cart cart, String paymentMethod) {
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

        boolean isOnlineGateway = "UPI_ONLINE".equalsIgnoreCase(paymentMethod) || "CARD_ONLINE".equalsIgnoreCase(paymentMethod);

        Map<String, List<CartItem>> itemsByCanteen = new LinkedHashMap<>();
        for (CartItem item : cart.getItems()) {
            if (!canteenService.isValidCanteen(item.getCanteen()) || !item.getFood().getCanteens().contains(item.getCanteen())) {
                throw new IllegalArgumentException("Sorry, canteen '" + item.getCanteen() + "' is not active or has been removed from the system.");
            }
            itemsByCanteen.computeIfAbsent(item.getCanteen(), ignored -> new ArrayList<>()).add(item);
        }

        Map<Long, Food> managedFoods = new LinkedHashMap<>();
        if (!isOnlineGateway) {
            // Counter payment: Deduct stock only when order is successfully placed
            for (CartItem item : cart.getItems()) {
                Food food = foodRepository.findByIdForUpdate(item.getFood().getFoodId())
                        .orElseThrow(() -> new IllegalArgumentException("Food not found: " + item.getFood().getName()));
                int availableStock = food.getStockForCanteen(item.getCanteen());
                if (availableStock < item.getQuantity()) {
                    throw new IllegalArgumentException("Sorry! Only " + availableStock + " portion(s) of '" + food.getName() + "' remaining in " + item.getCanteen() + ".");
                }
                food.decreaseStock(item.getCanteen(), item.getQuantity());
                foodRepository.save(food);
                managedFoods.put(food.getFoodId(), food);
            }
        } else {
            // Online demo payment: Check stock availability, do not permanently deduct yet
            for (CartItem item : cart.getItems()) {
                Food food = foodRepository.findById(item.getFood().getFoodId())
                        .orElseThrow(() -> new IllegalArgumentException("Food not found: " + item.getFood().getName()));
                int availableStock = food.getStockForCanteen(item.getCanteen());
                if (availableStock < item.getQuantity()) {
                    throw new IllegalArgumentException("Sorry! Only " + availableStock + " portion(s) of '" + food.getName() + "' remaining in " + item.getCanteen() + ".");
                }
                managedFoods.put(food.getFoodId(), food);
            }
        }

        List<Order> orders = new ArrayList<>();
        String groupId = "GRP-" + System.currentTimeMillis();
        for (Map.Entry<String, List<CartItem>> entry : itemsByCanteen.entrySet()) {
            Order order = new Order();
            order.setTokenNumber(generateTokenNumber());
            order.setOrderGroupId(groupId);
            order.setStudent(student);
            order.setCanteen(entry.getKey());
            order.setOrderTime(LocalDateTime.now());
            order.setPickupTimeSlot("Immediate Pickup");
            order.setStatus(isOnlineGateway ? OrderStatus.AWAITING_PAYMENT : OrderStatus.READY_FOR_PICKUP);
            order.setPaymentMethod(paymentMethod != null ? paymentMethod : "PAY_AT_COUNTER");
            order.setPaymentStatus(isOnlineGateway ? "PENDING_PAYMENT" : "PAY_ON_PICKUP");
            order.setStockDeducted(!isOnlineGateway);
            order.setTotalAmount(entry.getValue().stream().mapToDouble(item -> {
                Food f = managedFoods.getOrDefault(item.getFood().getFoodId(), item.getFood());
                return f.getPrice() * item.getQuantity();
            }).sum());

            for (CartItem item : entry.getValue()) {
                Food f = managedFoods.getOrDefault(item.getFood().getFoodId(), item.getFood());
                order.addItem(new OrderItem(order, f, item.getQuantity(), f.getPrice()));
            }
            orders.add(orderRepository.save(order));
        }
        return orders;
    }

    /**
     * Called from the mock payment gateway once the student completes (or the demo simulates)
     * the online checkout step. Only orders still AWAITING_PAYMENT can be settled this way.
     */
    @Transactional
    public Order settleOnlinePayment(String tokenNumber, boolean success) {
        return settleOnlinePayment(tokenNumber, success, null);
    }

    @Transactional
    public Order settleOnlinePayment(String tokenNumber, boolean success, String paymentMethod) {
        Order order = orderRepository.findByTokenNumber(tokenNumber.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Order not found for token: " + tokenNumber));

        List<Order> targetOrders;
        if (order.getOrderGroupId() != null && !order.getOrderGroupId().isEmpty()) {
            targetOrders = orderRepository.findByOrderGroupId(order.getOrderGroupId());
        } else {
            targetOrders = List.of(order);
        }

        for (Order o : targetOrders) {
            if (o.getStatus() == OrderStatus.AWAITING_PAYMENT) {
                if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
                    o.setPaymentMethod(paymentMethod.trim().toUpperCase());
                }
                if (success) {
                    // Deduct stock only after successful payment settlement
                    if (!o.isStockDeducted()) {
                        for (OrderItem item : o.getItems()) {
                            Food food = foodRepository.findByIdForUpdate(item.getFood().getFoodId())
                                    .orElseThrow(() -> new IllegalArgumentException("Food not found: " + item.getFood().getName()));
                            int availableStock = food.getStockForCanteen(o.getCanteen());
                            if (availableStock < item.getQuantity()) {
                                throw new IllegalArgumentException("Sorry! '" + food.getName() + "' is out of stock in " + o.getCanteen() + ".");
                            }
                            food.decreaseStock(o.getCanteen(), item.getQuantity());
                            foodRepository.save(food);
                        }
                        o.setStockDeducted(true);
                    }
                    o.setPaymentStatus("PAID");
                    o.setStatus(OrderStatus.READY_FOR_PICKUP);
                } else {
                    // Payment failed: do not deduct stock
                    o.setPaymentStatus("FAILED");
                }
                orderRepository.save(o);
            }
        }
        return orderRepository.findByTokenNumber(tokenNumber.trim().toUpperCase()).orElse(order);
    }

    @Transactional
    public Order cancelOrder(String tokenNumber) {
        Order order = orderRepository.findByTokenNumber(tokenNumber.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Order not found for token: " + tokenNumber));

        List<Order> targetOrders;
        if (order.getOrderGroupId() != null && !order.getOrderGroupId().isEmpty()) {
            targetOrders = orderRepository.findByOrderGroupId(order.getOrderGroupId());
        } else {
            targetOrders = List.of(order);
        }

        for (Order o : targetOrders) {
            // If already deducted, restore deducted stock exactly once
            if (o.isStockDeducted()) {
                for (OrderItem item : o.getItems()) {
                    foodRepository.findByIdForUpdate(item.getFood().getFoodId()).ifPresent(food -> {
                        food.increaseStock(o.getCanteen(), item.getQuantity());
                        foodRepository.save(food);
                    });
                }
                o.setStockDeducted(false);
            }
            if (o.getStatus() == OrderStatus.AWAITING_PAYMENT || o.getStatus() == OrderStatus.READY_FOR_PICKUP || o.getStatus() == OrderStatus.CONFIRMED) {
                o.setStatus(OrderStatus.CANCELLED);
                o.setPaymentStatus("CANCELLED");
                orderRepository.save(o);
            }
        }
        return orderRepository.findByTokenNumber(tokenNumber.trim().toUpperCase()).orElse(order);
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

    public List<Order> getOrdersByCanteen(String canteen) {
        return orderRepository.findByCanteenOrderByOrderTimeDesc(canteen);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByOrderTimeAsc(status);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (newStatus == OrderStatus.CANCELLED && order.isStockDeducted()) {
            // Restore deducted stock exactly once
            for (OrderItem item : order.getItems()) {
                foodRepository.findByIdForUpdate(item.getFood().getFoodId()).ifPresent(food -> {
                    food.increaseStock(order.getCanteen(), item.getQuantity());
                    foodRepository.save(food);
                });
            }
            order.setStockDeducted(false);
        } else if (newStatus != OrderStatus.CANCELLED && !order.isStockDeducted() && order.getStatus() == OrderStatus.CANCELLED) {
            // Re-activating a previously cancelled order: verify and deduct stock
            for (OrderItem item : order.getItems()) {
                Food food = foodRepository.findByIdForUpdate(item.getFood().getFoodId())
                        .orElseThrow(() -> new IllegalArgumentException("Food not found: " + item.getFood().getName()));
                int stock = food.getStockForCanteen(order.getCanteen());
                if (stock < item.getQuantity()) {
                    throw new IllegalArgumentException("Cannot re-activate: only " + stock + " portion(s) of '" + food.getName() + "' remaining in " + order.getCanteen() + ".");
                }
                food.decreaseStock(order.getCanteen(), item.getQuantity());
                foodRepository.save(food);
            }
            order.setStockDeducted(true);
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.COMPLETED && "PAY_ON_PICKUP".equalsIgnoreCase(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setPaymentStatus("CANCELLED");
        }
        return orderRepository.save(order);
    }

    public List<Order> getOrdersByGroupId(String orderGroupId) {
        if (orderGroupId == null || orderGroupId.isEmpty()) return List.of();
        return orderRepository.findByOrderGroupId(orderGroupId);
    }

    public List<Order> getOrdersByCanteenAndStatus(String canteen, OrderStatus status) {
        return orderRepository.findByCanteenAndStatusOrderByOrderTimeAsc(canteen, status);
    }

    public long getCountByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public long getCountByStatusAndCanteen(OrderStatus status, String canteen) {
        return orderRepository.countByStatusAndCanteen(status, canteen);
    }

    public double getTotalRevenue() {
        return orderRepository.calculateTotalRevenue();
    }

    public double getTotalRevenueByCanteen(String canteen) {
        return orderRepository.calculateTotalRevenueByCanteen(canteen);
    }

    public long getValidOrderCount() {
        return orderRepository.countValidOrders();
    }

    public long getValidOrderCountByCanteen(String canteen) {
        return orderRepository.countValidOrdersByCanteen(canteen);
    }
}
