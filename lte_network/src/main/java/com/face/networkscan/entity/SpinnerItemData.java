package com.face.networkscan.entity;

import me.goldze.mvvmhabit.binding.viewadapter.spinner.IKeyAndValue;

public class SpinnerItemData  implements IKeyAndValue {
    //key是下拉显示的文字
    private String key;
    //value是对应需要上传给后台的值, 这个可以根据具体业务具体定义
    private String value;

    public SpinnerItemData(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }
}
