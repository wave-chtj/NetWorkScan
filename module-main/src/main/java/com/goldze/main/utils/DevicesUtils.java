package com.goldze.main.utils;

import com.goldze.main.entity.NameVersionEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DevicesUtils {
    public static final String FEI_SI_KA_ER = "imx6";
    public static final String RK_3399 = "rk3399";

    /**
     * 默认机型的添加
     * @return 机型
     */
    public static LinkedHashMap<String, NameVersionEntity> getModelTypeList() {
        LinkedHashMap<String, NameVersionEntity> modelTypeList = new LinkedHashMap<>();
        NameVersionEntity nameVersionEntity=new NameVersionEntity("rk3399","7.1.2");
        modelTypeList.put("echo 1 > /sys/class/spi_sim_ctl/state", nameVersionEntity);
        nameVersionEntity=new NameVersionEntity("飞思卡尔1","4.4.2");
        modelTypeList.put("echo 1 > /sys/devices/platform/imx6q_sim/sim_sysfs/state",nameVersionEntity);//4.4.2
        nameVersionEntity=new NameVersionEntity("飞思卡尔2","5.1.1");
        modelTypeList.put("echo 1 > /sys/bus/platform/devices/sim-gpios.40/sim_sysfs/state",nameVersionEntity);//5.1.1
        return modelTypeList;
    }

    /**
     * 获取默认访问的地址
     * @return
     */
    public static List<String> getAddrList(){
        List<String> addrList=new ArrayList<>();
        addrList.add("www.google.cn");
        addrList.add("223.5.5.5");
        return addrList;
    }
}