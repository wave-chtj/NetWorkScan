package me.goldze.mvvmhabit.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.widget.Toast;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;

public class NetworkUtil {
    private static final String TAG = NetworkUtil.class.getSimpleName();
    public static String url = "www.baidu.com";//阿里公共 DNS:223.5.5.5; google dns:8.8.8.8
    //当网络异常时 需要ping国内外的地址 以保证项目不会因手动设置的访问地址而产生网络异常
    static String[] publicUrl = new String[]{
            "8.8.8.8",
            "223.5.5.5"};
    public enum NET_TYPE{
        NET_CNNT_OK,NET_CNNT_BAIDU_TIMEOUT,NET_NOT_PREPARE,NET_ERROR
    }
   /* public static int NET_CNNT_BAIDU_OK = 1; // NetworkAvailable
    public static int NET_CNNT_BAIDU_TIMEOUT = 2; // no NetworkAvailable
    public static int NET_NOT_PREPARE = 3; // Net no ready
    public static int NET_ERROR = 4; //net error*/
   private static int TIMEOUT = 4000; // TIMEOUT


    /**
     * check NetworkAvailable
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (null == manager)
            return false;
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (null == info || !info.isAvailable())
            return false;
        return true;
    }

    /**
     * getLocalIpAddress
     *
     * @return
     */
    public static String getLocalIpAddress() {
        String ret = "";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ret = inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    /**
     * 返回当前网络状态
     *
     * @param context
     * @return
     */
    public static NET_TYPE getNetState(Context context, String urlAddr) {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo networkinfo = connectivity.getActiveNetworkInfo();
                if (networkinfo != null) {
                    if (networkinfo.isAvailable() && networkinfo.isConnected()) {
                        //第一次网络确认
                        if (!pingIp(urlAddr)) {
                            //第二次网络确认
                            if(isAgainConn(urlAddr)){
                                return NET_TYPE.NET_CNNT_OK;
                            }else{
                                return NET_TYPE.NET_CNNT_BAIDU_TIMEOUT;
                            }
                        } else {
                            return NET_TYPE.NET_CNNT_OK;
                        }
                    } else {
                        //网络未准备好
                        //需要再次确认网络是否正常
                        if(isAgainConn(urlAddr)){
                            return NET_TYPE.NET_CNNT_OK;
                        }else{
                            return NET_TYPE.NET_NOT_PREPARE;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            KLog.e(TAG,"getNetState() errMeg:"+e.getMessage());
        }
        //网络未准备好
        //需要再次确认网络是否正常
        if(isAgainConn(urlAddr)){
            return NET_TYPE.NET_CNNT_OK;
        }else{
            return NET_TYPE.NET_ERROR;
        }
    }


    /**
     * 网络异常时的再次确认 这时候要去访问国内外的地址
     * @param url
     * @return
     */
    public static boolean isAgainConn(String url) {
        boolean isAgain = false;
        //如果当前自己设置的访问地址 或者没有手动设置过地址
        //那么 先判断 这个地址是否为需要再次访问需要去确认的地址
        //如果当前地址存在这个公共地址中 那么 跳过 执行另一个
        //比如 是国内的 那么立即执行国外的dns
        //比如 是国外的 那么立即执行国内的dns
        //否则两个都要执行 如果其中一个成功 立即跳出
        KLog.e(TAG,"网络出现问题,正在调整访问地址以进行网络判断");
        if (url.equals(publicUrl[0]) || url.equals(publicUrl[1])) {
            if (url.equals(publicUrl[0])) {
                isAgain = pingIp(publicUrl[1]);
            } else {
                isAgain = pingIp(publicUrl[0]);
            }
            KLog.e(TAG,"-经过再次访问,确定了网络是否正常："+isAgain);
            return isAgain;
        } else {
            isAgain = pingIp(publicUrl[0]);
            if(isAgain){
                KLog.e(TAG,"--经过再次访问,确定了网络是否正常："+true);
                return true;
            }else{
                KLog.e(TAG,"--经过再次访问,确定了网络是否正常："+false);
                isAgain=  pingIp(publicUrl[1]);
                KLog.e(TAG,"---经过再次访问,确定了网络是否正常："+isAgain);
                return isAgain;
            }
        }
    }

    /**
     * ping "http://www.baidu.com"
     *
     * @return
     */
    static private boolean connectionNetwork() {
        boolean result = false;
        HttpURLConnection httpUrl = null;
        try {
            httpUrl = (HttpURLConnection) new URL(url)
                    .openConnection();
            httpUrl.setConnectTimeout(TIMEOUT);
            httpUrl.connect();
            result = true;
        } catch (IOException e) {
        } finally {
            if (null != httpUrl) {
                httpUrl.disconnect();
            }
            httpUrl = null;
        }
        return result;
    }

    //Ping
    public static boolean pingIp(String urlAddr) {
        boolean isConnect = false;
        try {
            if (urlAddr != null) {
                String command = "ping -c 2 -w 5 " + urlAddr;
                //KLog.e(TAG,"正在对地址:"+urlAddr+"进行访问,执行的命令为："+command);
                //代表ping 2 次 超时时间为5秒
                Process p = Runtime.getRuntime().exec(command);//ping2次
                int status = p.waitFor();
                if (status == 0) {
                    isConnect = true;
                    //代表成功
                } else {
                    //代表失败
                    isConnect = false;
                }
            } else {
                //代表失败
                isConnect = false;
            }
        } catch (Exception e) {
            isConnect = false;
            KLog.e(TAG, e.getMessage());
        }
        return isConnect;
    }

    /**
     * check is3G
     *
     * @param context
     * @return boolean
     */
    public static boolean is3G(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return true;
        }
        return false;
    }

    /**
     * isWifi
     *
     * @param context
     * @return boolean
     */
    public static boolean isWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    /**
     * is2G
     *
     * @param context
     * @return boolean
     */
    public static boolean is2G(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && (activeNetInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE
                || activeNetInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS || activeNetInfo
                .getSubtype() == TelephonyManager.NETWORK_TYPE_CDMA)) {
            return true;
        }
        return false;
    }

    /**
     * is wifi on
     */
    public static boolean isWifiEnabled(Context context) {
        ConnectivityManager mgrConn = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager mgrTel = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return ((mgrConn.getActiveNetworkInfo() != null && mgrConn
                .getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || mgrTel
                .getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);
    }

}
