package com.face.lte_networkscanreboot.utils;

import java.lang.reflect.Method;

import me.goldze.mvvmhabit.utils.KLog;

public class SystemInfoUtil {

    public static final String TAG=SystemInfoUtil.class.getSimpleName();

    /**
     * 获取机型
     * @return
     */
    public static String getModelType()
    {
        String serial = null;
        try {
            Class<?> c =Class.forName("android.os.SystemProperties");
            Method get =c.getMethod("get", String.class, String.class);
            serial = (String)get.invoke(c, "ro.board.platform","unkown");
        } catch (Exception e) {
            e.printStackTrace();
        }
        KLog.d(TAG, "getSerialNumber: serial=: "+serial);
        return serial;

    }
}
