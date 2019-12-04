package com.face.lte_networkscan.entity;

public class ParamEntity {
    private int type;//类型 1 为循环判断网络时间 2 网络异常扫描错误
    private Object param;//参数
    private int position;

    public ParamEntity(int type, Object param, int position) {
        this.type = type;
        this.param = param;
        this.position = position;
    }

    public ParamEntity() {
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }


}
