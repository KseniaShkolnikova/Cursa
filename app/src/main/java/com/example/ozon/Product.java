package com.example.ozon;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
public class Product implements Serializable {
    private String id;
    private String name;
    private int price;
    private String productType;
    private String imageBase64;
    private String description;
    private String sellerId;
    private int quantity;
    public Product() {}
    public Product(String name, int price, String productType, String imageBase64, String description, int quantity) {
        this.name = name;
        this.price = price;
        this.productType = productType;
        this.imageBase64 = imageBase64;
        this.description = description;
        this.quantity = quantity;
    }
    @PropertyName("productId")
    public String getId() {
        return id;
    }
    @PropertyName("productId")
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
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    @Override
    public String toString() {
        return "Product{id='" + id + "', name='" + name + "', price=" + price + ", quantity=" + quantity + "}";
    }
}