package com.face.lte_networkscanreboot.entity;

public class NameVersionEntity {
    private String name;
    private String androidVersion;

    public NameVersionEntity(String name, String androidVersion) {
        this.name = name;
        this.androidVersion = androidVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAndroidVersion() {
        return androidVersion;
    }

    public void setAndroidVersion(String androidVersion) {
        this.androidVersion = androidVersion;
    }
}
