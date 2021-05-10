package ru.novaworld.models;

import java.util.Date;

public abstract class YotaDetail {
    private Date date;
    private String type;

    public YotaDetail() {}

    public YotaDetail(Date date, String type) {
        this.date = date;
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
