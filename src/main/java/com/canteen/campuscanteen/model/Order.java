package com.canteen.campuscanteen.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "canteen_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false, unique = true)
    private String tokenNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_roll", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDateTime orderTime = LocalDateTime.now();

    @Column(nullable = false)
    private String pickupTimeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.READY_FOR_PICKUP;

    @Column(nullable = false)
    private String paymentMethod = "PAY_AT_COUNTER";

    @Column(nullable = false)
    private String paymentStatus = "PENDING";

    @Column(nullable = false)
    private double totalAmount;

    private String specialInstructions;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Order(String tokenNumber, Student student, String pickupTimeSlot,
                 String paymentMethod, String paymentStatus, String specialInstructions) {
        this.tokenNumber = tokenNumber;
        this.student = student;
        this.orderTime = LocalDateTime.now();
        this.pickupTimeSlot = pickupTimeSlot;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.specialInstructions = specialInstructions;
        this.status = OrderStatus.READY_FOR_PICKUP;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public String getFormattedOrderTime() {
        if (orderTime == null) return "";
        return orderTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    public String getPickupTimeSlot() {
        return pickupTimeSlot;
    }

    public void setPickupTimeSlot(String pickupTimeSlot) {
        this.pickupTimeSlot = pickupTimeSlot;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
