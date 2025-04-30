package com.example.ozon;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * Класс Order представляет модель заказа в приложении "OZON".
 * Содержит информацию о заказе, такую как идентификатор пользователя, общая сумма,
 * способ оплаты, список продуктов, статус, дата заказа, сроки доставки, адрес доставки
 * и данные о уведомлениях.
 */
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

    /**
     * Конструктор по умолчанию для создания пустого объекта заказа.
     */
    public Order() {
    }

    /**
     * Конструктор для создания объекта заказа с заданными параметрами. Инициализирует
     * основные поля заказа, устанавливает начальные значения для дней доставки и
     * статуса уведомлений.
     */
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

    /**
     * Возвращает идентификатор пользователя, сделавшего заказ.
     */
    public String getUserId() { return userId; }

    /**
     * Устанавливает идентификатор пользователя для заказа.
     */
    public void setUserId(String userId) { this.userId = userId; }

    /**
     * Возвращает общую сумму заказа в рублях.
     */
    public int getTotalAmount() { return totalAmount; }

    /**
     * Устанавливает общую сумму заказа.
     */
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }

    /**
     * Возвращает способ оплаты, выбранный для заказа.
     */
    public String getPaymentMethod() { return paymentMethod; }

    /**
     * Устанавливает способ оплаты для заказа.
     */
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    /**
     * Возвращает список продуктов, включенных в заказ.
     */
    public List<Product> getProducts() { return products; }

    /**
     * Устанавливает список продуктов для заказа.
     */
    public void setProducts(List<Product> products) { this.products = products; }

    /**
     * Возвращает текущий статус заказа (например, "создан", "в процессе", "доставлен").
     */
    public String getStatus() { return status; }

    /**
     * Устанавливает статус заказа.
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * Возвращает дату создания заказа в формате Timestamp.
     */
    public Timestamp getOrderDate() { return orderDate; }

    /**
     * Устанавливает дату создания заказа.
     */
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }

    /**
     * Возвращает количество оставшихся дней до доставки заказа.
     */
    public Long getDays() { return days; }

    /**
     * Устанавливает количество оставшихся дней до доставки заказа.
     */
    public void setDays(Long days) { this.days = days; }

    /**
     * Возвращает изначальное количество дней, заданное для доставки заказа.
     */
    public Long getInitialDays() { return initialDays; }

    /**
     * Устанавливает изначальное количество дней для доставки заказа.
     */
    public void setInitialDays(Long initialDays) { this.initialDays = initialDays; }

    /**
     * Возвращает флаг, указывающий, было ли отправлено уведомление о заказе.
     */
    public Boolean getNotificationSent() { return notificationSent; }

    /**
     * Устанавливает флаг, указывающий, было ли отправлено уведомление о заказе.
     */
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }

    /**
     * Возвращает адрес доставки заказа.
     */
    public String getDeliveryAddress() { return deliveryAddress; }

    /**
     * Устанавливает адрес доставки для заказа.
     */
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    /**
     * Возвращает количество дней, оставшихся до доставки на момент последнего уведомления.
     */
    public Long getLastNotifiedDays() { return lastNotifiedDays; }

    /**
     * Устанавливает количество дней, оставшихся до доставки на момент последнего уведомления.
     */
    public void setLastNotifiedDays(Long lastNotifiedDays) { this.lastNotifiedDays = lastNotifiedDays; }
}