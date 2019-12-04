package com.face.lte_networkscan.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Create on 2019/10/29
 * author chtj
 * desc
 */
public class DateUtil {
    /**
     * 获取当前时间 - Calendar方式
     *
     * @return
     */
    public static String getCurrentTimeYMDHMS() {
        //获取当前时间
        Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int date = c.get(Calendar.DATE);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        return year + "-" + (month + 1) + "-" + date + " " + hour + ":" + minute + ":" + second;
    }

    /**
     * 获取当前的Calendar
     * @return
     */
    public static Calendar getCurrentCalendar() {
        //获取当前时间
        Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
        return c;
    }

    /**
     * 字符串转换为Calendar
     *
     * @return
     */
    public static Calendar getCalendarByTime(String saveTime) throws ParseException {
        Calendar calendar = Calendar.getInstance();  // 默认是当前日期
        // 对 calendar 设置时间的方法
        // 设置传入的时间格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 指定一个日期
        Date date = dateFormat.parse(saveTime);
        // 对 calendar 设置为 date 所定的日期
        calendar.setTime(date);
        return calendar;
    }
}
