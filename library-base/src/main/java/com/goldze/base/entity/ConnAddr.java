package com.goldze.base.entity;

public class ConnAddr {
    private String id;
    private String addr;

    public ConnAddr() {
    }

    public ConnAddr(String id, String addr) {
        this.id = id;
        this.addr = addr;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }
}
