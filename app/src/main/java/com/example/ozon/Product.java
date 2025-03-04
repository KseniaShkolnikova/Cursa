package com.example.ozon;

import android.util.Log;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class Product {
    private String id;
    private String name;
    private int price;
    private String productType;
    private String imageBase64;
    private String description;
    private String sellerId;

    public Product() {}

    public Product(String name, int price, String productType, String imageBase64, String description) {
        this.name = name;
        this.price = price;
        this.productType = productType;
        this.imageBase64 = imageBase64;
        this.description = description;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
}