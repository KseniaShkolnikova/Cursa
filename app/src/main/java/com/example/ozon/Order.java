package com.example.ozon;

import java.util.List;

public class Order {
    private String userId;
    private int totalAmount;
    private String cardNumber;
    private List<String> productIds; // Список ID товаров

    public Order() {
        // Пустой конструктор для Firestore
    }

    public Order(String userId, int totalAmount, String cardNumber, List<String> productIds) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.cardNumber = cardNumber;
        this.productIds = productIds;
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
}
