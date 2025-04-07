package com.example.ozon;
import com.google.firebase.Timestamp;
import java.util.List;
public class Order {
    private String userId;
    private int totalAmount;
    private String paymentMethod;
    private List<Product> products;
    private String status;
    private Timestamp orderDate;
    private Long days;
    private Long initialDays;
    private Boolean notificationSent;
    private String deliveryAddress;
    private Long lastNotifiedDays;
    public Order() {
    }
    public Order(String userId, int totalAmount, String paymentMethod, List<Product> products,
                 String status, Timestamp orderDate, Long initialDays, String deliveryAddress) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.products = products;
        this.status = status;
        this.orderDate = orderDate;
        this.days = 0L;
        this.initialDays = initialDays;
        this.notificationSent = false;
        this.deliveryAddress = deliveryAddress;
        this.lastNotifiedDays = null;
    }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
    public Long getDays() { return days; }
    public void setDays(Long days) { this.days = days; }
    public Long getInitialDays() { return initialDays; }
    public void setInitialDays(Long initialDays) { this.initialDays = initialDays; }
    public Boolean getNotificationSent() { return notificationSent; }
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public Long getLastNotifiedDays() { return lastNotifiedDays; }
    public void setLastNotifiedDays(Long lastNotifiedDays) { this.lastNotifiedDays = lastNotifiedDays; }
}