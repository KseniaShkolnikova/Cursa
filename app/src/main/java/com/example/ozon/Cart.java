package com.example.ozon;

import com.google.firebase.firestore.DocumentId;

public class Cart {
    @DocumentId
    private String documentId; // Уникальный идентификатор документа
    private String name;
    private int price;
    private int quantity;
    private String imageBase64;
    private String userId;

    // Пустой конструктор для Firestore
    public Cart() {}

    public Cart(String documentId, String name, int price, int quantity, String imageBase64, String userId) {
        this.documentId = documentId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageBase64 = imageBase64;
        this.userId = userId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}