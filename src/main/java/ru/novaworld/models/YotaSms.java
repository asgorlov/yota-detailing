package ru.novaworld.models;

import java.util.Date;

public class YotaSms extends YotaDetail {
    private int quantity;

    public YotaSms() {}

    public YotaSms(Date date, String type, int quantity) {
        super(date, type);
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
