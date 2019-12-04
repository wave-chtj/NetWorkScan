package com.face.lte_networkscan.utils;

import com.face.lte_networkscan.entity.NameVersionEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DevicesUtils {
    public static final String FEI_SI_KA_ER = "imx6";
    public static final String RK_3399 = "rk3399";
    public static final String RK_3288 = "rk3288";

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
        modelTypeList.put("echo 1 > /sys/devices/soc0/sim-gpios.40/sim_sysfs/state",nameVersionEntity);//5.1.1
        return modelTypeList;
    }

    /**
     * 获取默认访问的地址
     * @return
     */
    public static List<String> getAddrList(){
        List<String> addrList=new ArrayList<>();
        addrList.add("www.google.cn");
        addrList.add("www.baidu.com");
        addrList.add("223.5.5.5");
        addrList.add("8.8.8.8");//google公共dns
        return addrList;
    }
}
