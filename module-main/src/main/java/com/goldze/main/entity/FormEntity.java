package com.goldze.main.entity;

import android.databinding.BaseObservable;
import android.os.Parcel;
import android.os.Parcelable;

public class FormEntity extends BaseObservable implements Parcelable {
    private String addr;

    public FormEntity() {
    }

    public FormEntity(String addr) {
        this.addr = addr;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public static Creator<FormEntity> getCREATOR() {
        return CREATOR;
    }

    protected FormEntity(Parcel in) {
        addr = in.readString();
    }

    public static final Creator<FormEntity> CREATOR = new Creator<FormEntity>() {
        @Override
        public FormEntity createFromParcel(Parcel in) {
            return new FormEntity(in);
        }

        @Override
        public FormEntity[] newArray(int size) {
            return new FormEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(addr);
    }
}
