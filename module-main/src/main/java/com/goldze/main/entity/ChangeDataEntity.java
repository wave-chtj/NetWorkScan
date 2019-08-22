package com.goldze.main.entity;

import java.util.List;

public class ChangeDataEntity {
    public enum DATA_TYPE{
        TYPE_CONN_ADDR,TYPE_MODEL_TYPE
    }

    private int id;
    private int defaultPositon;
    private List<String> data;
    private DATA_TYPE data_type;

    public ChangeDataEntity(int id, int defaultPositon, List<String> data, DATA_TYPE data_type) {
        this.id = id;
        this.defaultPositon = defaultPositon;
        this.data = data;
        this.data_type = data_type;
    }

    public ChangeDataEntity() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDefaultPositon() {
        return defaultPositon;
    }

    public void setDefaultPositon(int defaultPositon) {
        this.defaultPositon = defaultPositon;
    }

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    public DATA_TYPE getData_type() {
        return data_type;
    }

    public void setData_type(DATA_TYPE data_type) {
        this.data_type = data_type;
    }
}
