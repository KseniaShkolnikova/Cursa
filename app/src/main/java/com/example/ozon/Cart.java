package com.example.ozon;

import com.google.firebase.firestore.DocumentId;

/**
 * Класс Cart представляет модель данных для элемента корзины в приложении "OZON".
 * Содержит информацию о товаре в корзине, включая идентификатор документа, идентификатор товара,
 * название, цену, количество, изображение в формате Base64 и идентификатор пользователя.
 */
public class Cart {
    @DocumentId
    private String documentId;
    private String productId;
    private String name;
    private int price;
    private int quantity;
    private String imageBase64;
    private String userId;

    /**
     * Конструктор по умолчанию для создания пустого объекта Cart.
     * Используется Firebase Firestore для десериализации данных.
     */
    public Cart() {}

    /**
     * Конструктор для создания объекта Cart с заданными значениями.
     * Инициализирует поля класса: идентификатор товара, название, цену, количество,
     * изображение в формате Base64 и идентификатор пользователя.
     */
    public Cart(String productId, String name, int price, int quantity,
                String imageBase64, String userId) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageBase64 = imageBase64;
        this.userId = userId;
    }

    /**
     * Возвращает идентификатор документа корзины в базе данных Firebase Firestore.
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Устанавливает идентификатор документа корзины в базе данных Firebase Firestore.
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * Возвращает идентификатор товара, добавленного в корзину.
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Устанавливает идентификатор товара, добавленного в корзину.
     */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * Возвращает идентификатор пользователя, которому принадлежит корзина.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Устанавливает идентификатор пользователя, которому принадлежит корзина.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Возвращает название товара в корзине.
     */
    public String getName() {
        return name;
    }

    /**
     * Устанавливает название товара в корзине.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Возвращает цену товара в корзине.
     */
    public int getPrice() {
        return price;
    }

    /**
     * Устанавливает цену товара в корзине.
     */
    public void setPrice(int price) {
        this.price = price;
    }

    /**
     * Возвращает количество единиц товара в корзине.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Устанавливает количество единиц товара в корзине.
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
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
}