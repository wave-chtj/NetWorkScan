package com.face.ethlinstener.ui.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.EthernetDataTracker;
import android.net.LinkAddress;
import android.net.ethernet.EthernetManager;
import android.os.IBinder;
import android.util.Log;
import android.net.NetworkUtils;
import android.widget.RemoteViews;
import com.face.ethlinstener.R;
import com.face.ethlinstener.ui.activity.EthLinstenerActivity;
import com.face_chtj.base_iotutils.keeplive.AbsWorkService;
import com.face_chtj.base_iotutils.keeplive.BaseIotUtils;

import java.lang.reflect.Constructor;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.goldze.mvvmhabit.http.NetworkUtil;
import me.goldze.mvvmhabit.utils.SPUtils;

/**
 * Create on 2019/11/7
 * author chtj
 * desc
 */
public class EthLinstenerService extends AbsWorkService {
    public static final String TAG = "EthLinstenerService";
    //是否 任务完成, 不再需要服务运行?
    public static boolean sShouldStopService;
    public static Disposable sDisposable;
    //系统通知
    private NotificationManager manager = null;
    private Notification.Builder builder = null;
    //自定义的系统通知视图
    private RemoteViews contentView = null;
    
    public static void stopService() {
        //我们现在不再需要服务运行了, 将标志位置为 true
        sShouldStopService = true;
        //取消对任务的订阅
        if (sDisposable != null) sDisposable.dispose();
        //取消 Job / Alarm / Subscription
        cancelJobAlarmSub();
    }

    /**
     * 是否 任务完成, 不再需要服务运行?
     *
     * @return 应当停止服务, true; 应当启动服务, false; 无法判断, 什么也不做, null.
     */
    @Override
    public Boolean shouldStopService(Intent intent, int flags, int startId) {
        return sShouldStopService;
    }

    @Override
    public void startWork(Intent intent, int flags, int startId) {
        manager = (NotificationManager) EthLinstenerService.this.getSystemService(NOTIFICATION_SERVICE);
        builder = new Notification.Builder(EthLinstenerService.this);
        Intent notificationIntent =new Intent(EthLinstenerService.this, EthLinstenerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentItent = PendingIntent.getActivity(EthLinstenerService.this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        contentView = new RemoteViews(getApplication().getPackageName(), R.layout.activity_notification);
        contentView.setTextViewText(R.id.tvEthStatus,"当前以太网状态:正在加载...");
        builder.setContent(contentView);
        builder.setContentIntent(contentItent);
        builder.setSmallIcon(R.drawable.network_eth);  //小图标，在大图标右下角
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.network_eth)); //大图标，没有设置时小图标就是大图标
        builder.setOngoing(true);//滑动不能清除
        builder.setAutoCancel(false);   //点击的时候消失
        manager.notify(14, builder.build());  //参数一为ID，用来区分不同APP的Notification
        int time = SPUtils.getInstance().getInt("cycleInterval", 1);
        Log.e(TAG, "开始任务....，当前的循环周期为：" + time);
        sDisposable = Observable
                .interval(0, time, TimeUnit.MINUTES)
                //取消任务时取消定时唤醒
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        cancelJobAlarmSub();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long count) throws Exception {
                        NetworkUtil.NET_TYPE net_type = NetworkUtil.getNetState(EthLinstenerService.this, "www.baidu.com");
                        if (net_type != NetworkUtil.NET_TYPE.NET_CNNT_OK) {
                            try{
                                contentView.setTextViewText(R.id.tvEthStatus,"当前以太网状态：网络异常");
                                manager.notify(14, builder.build());
                                EthernetManager ethernetManager = (EthernetManager) BaseIotUtils.getContext().getSystemService("ethernet");
                                int status=ethernetManager.getEthernetConnectState();
                                Log.e(TAG,"status="+status);
                                boolean isEthClose = ethernetManager.setEthernetEnabled(false);//关闭以太网
                                if (isEthClose) {
                                    Log.e(TAG, "关闭以太网成功！");
                                    boolean isEthOpen = ethernetManager.setEthernetEnabled(true);//开启以太网
                                    if (isEthOpen) {
                                        Log.e(TAG, "开启以太网成功！");
                                        contentView.setTextViewText(R.id.tvEthStatus,"当前以太网状态：重置成功");
                                        manager.notify(14, builder.build());
                                    } else {
                                        contentView.setTextViewText(R.id.tvEthStatus,"当前以太网状态：重置失败");
                                        manager.notify(14, builder.build());
                                        Log.e(TAG, "开启以太网失败！");
                                    }
                                }else{
                                    Log.e(TAG, "关闭以太网失败！");
                                    contentView.setTextViewText(R.id.tvEthStatus,"当前以太网状态：重置失败");
                                    manager.notify(14,builder.build());
                                }
                            }catch(Exception e){
                                e.printStackTrace();
                                Log.e(TAG,"errMeg:"+e.getMessage());
                                contentView.setTextViewText(R.id.tvEthStatus,"当前以太网状态：重置失败");
                                manager.notify(14,builder.build());
                            }
                        }else{
                            contentView.setTextViewText(R.id.tvEthStatus,"当前以太网状态：网络正常");
                            manager.notify(14,builder.build());
                            Log.e(TAG,"当前网络正常，不做任何操作");
                        }
                    }
                });
    }

    @Override
    public void stopWork(Intent intent, int flags, int startId) {
        stopService();
    }

    /**
     * 任务是否正在运行?
     *
     * @return 任务正在运行, true; 任务当前不在运行, false; 无法判断, 什么也不做, null.
     */
    @Override
    public Boolean isWorkRunning(Intent intent, int flags, int startId) {
        //若还没有取消订阅, 就说明任务仍在运行.
        return sDisposable != null && !sDisposable.isDisposed();
    }

    @Override
    public IBinder onBind(Intent intent, Void v) {
        return null;
    }

    @Override
    public void onServiceKilled(Intent rootIntent) {
        System.out.println("保存数据到磁盘。");
    }
}
