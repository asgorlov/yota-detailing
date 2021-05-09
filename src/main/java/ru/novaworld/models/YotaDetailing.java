package ru.novaworld.models;

import java.util.Objects;

public class YotaDetailing {
    private String date;
    private String type;
    private String quantity;

    public YotaDetailing(){}

    public YotaDetailing(String date, String type, String quantity) {
        this.date = date;
        this.type = type;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "{" +
                "date=" + date +
                ", type=" + type +
                ", quantity=" + quantity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YotaDetailing that = (YotaDetailing) o;
        return Objects.equals(date, that.date) &&
                Objects.equals(type, that.type) &&
                Objects.equals(quantity, that.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, type, quantity);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }
}
