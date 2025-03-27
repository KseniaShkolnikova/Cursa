package com.example.ozon;

import com.google.firebase.Timestamp;
import java.util.List;

public class Order {
    private String userId;
    private int totalAmount;
    private String cardNumber;
    private List<String> productIds;
    private String status; // Добавлено: статус заказа
    private Timestamp orderDate; // Добавлено: дата заказа
    private Long days; // Добавлено: срок доставки в днях

    public Order() {
        // Пустой конструктор для Firestore
    }

    public Order(String userId, int totalAmount, String cardNumber, List<String> productIds,
                 String status, Timestamp orderDate, Long days) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.cardNumber = cardNumber;
        this.productIds = productIds;
        this.status = status;
        this.orderDate = orderDate;
        this.days = days;
    }

    // Геттеры и сеттеры
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    public Long getDays() {
        return days;
    }

    public void setDays(Long days) {
        this.days = days;
    }
}