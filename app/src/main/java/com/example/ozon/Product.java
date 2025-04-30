package com.example.ozon;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

/**
 * Класс Product представляет модель товара в приложении "OZON".
 * Содержит информацию о товаре, такую как идентификатор, название, цена, тип,
 * изображение в формате Base64, описание, идентификатор продавца и доступное количество.
 * Реализует интерфейс Serializable для передачи объекта между компонентами.
 */
public class Product implements Serializable {
    private String id;
    private String name;
    private int price;
    private String productType;
    private String imageBase64;
    private String description;
    private String sellerId;
    private int quantity;

    /**
     * Конструктор по умолчанию для создания пустого объекта товара.
     */
    public Product() {}

    /**
     * Конструктор для создания объекта товара с заданными параметрами. Инициализирует
     * основные поля товара, такие как название, цена, тип, изображение, описание и количество.
     */
    public Product(String name, int price, String productType, String imageBase64, String description, int quantity) {
        this.name = name;
        this.price = price;
        this.productType = productType;
        this.imageBase64 = imageBase64;
        this.description = description;
        this.quantity = quantity;
    }

    /**
     * Возвращает идентификатор товара. Использует аннотацию PropertyName для соответствия
     * полю "productId" в Firestore.
     */
    @PropertyName("productId")
    public String getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор товара. Использует аннотацию PropertyName для соответствия
     * полю "productId" в Firestore.
     */
    @PropertyName("productId")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Возвращает название товара.
     */
    public String getName() {
        return name;
    }

    /**
     * Устанавливает название товара.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Возвращает цену товара в рублях.
     */
    public int getPrice() {
        return price;
    }

    /**
     * Устанавливает цену товара.
     */
    public void setPrice(int price) {
        this.price = price;
    }

    /**
     * Возвращает тип товара (например, категория или вид).
     */
    public String getProductType() {
        return productType;
    }

    /**
     * Устанавливает тип товара.
     */
    public void setProductType(String productType) {
        this.productType = productType;
    }

    /**
     * Возвращает изображение товара в формате Base64.
     */
    public String getImageBase64() {
        return imageBase64;
    }

    /**
     * Устанавливает изображение товара в формате Base64.
     */
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    /**
     * Возвращает описание товара.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание товара.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Возвращает идентификатор продавца, добавившего товар.
     */
    public String getSellerId() {
        return sellerId;
    }

    /**
     * Устанавливает идентификатор продавца для товара.
     */
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    /**
     * Возвращает доступное количество товара на складе.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Устанавливает доступное количество товара на складе.
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Возвращает строковое представление объекта товара. Включает идентификатор,
     * название, цену и количество.
     */
    @Override
    public String toString() {
        return "Product{id='" + id + "', name='" + name + "', price=" + price + ", quantity=" + quantity + "}";
    }
}