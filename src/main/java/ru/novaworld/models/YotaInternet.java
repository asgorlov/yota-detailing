package ru.novaworld.models;

import java.util.Date;

public class YotaInternet extends YotaDetail {
    private double quantity;

    public YotaInternet() {}

    public YotaInternet(Date date, String type, double quantity) {
        super(date, type);
        this.quantity = quantity;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
}
