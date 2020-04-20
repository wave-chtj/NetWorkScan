package com.face.networkscan.utils;

/**
 * Create on 2019/11/4
 * author chtj
 * desc
 */
public class KeyValueConst {
    //是否是第一次执行异常的统计
    public static final  String IS_NET_ERR_FIRST_COUNT="isCountFirst";
    //网络异常时统计的异常次数
    public static final  String ERR_COUNT="errCount";
    //记录网络异常时的状态 只有当正常变为异常时 才会执行次数加1的统计
    public static final  String LAST_STATUS="lastStatus";
    //网络周期执行时网络检测的次数
    public static final  String TOTAL_COUNT="totalCount";
    //默认访问的地址（ping的地址）
    public static final  String ADDR="addr";
    //网络正常时task1执行周期的间隔
    public static final  String CYCLE_INTERVAL="cycleInterval";
    //网络异常时task1执行周期的间隔
    public static final  String ERR_SCAN_COUNT="errScanCount";


}
