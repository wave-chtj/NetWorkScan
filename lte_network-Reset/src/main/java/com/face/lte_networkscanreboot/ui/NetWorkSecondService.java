package com.face.lte_networkscanreboot.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import com.chtj.base_iotutils.ShellUtils;
import com.face.lte_networkscanreboot.R;
import com.face.lte_networkscanreboot.entity.NetTimerParamEntity;
import com.face.lte_networkscanreboot.entity.ThreadNotice;
import com.face.lte_networkscanreboot.entity.TotalEntity;
import com.face.lte_networkscanreboot.utils.KeyValueConst;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.http.NetworkUtil;
import me.goldze.mvvmhabit.netbus.NetChangeObserver;
import me.goldze.mvvmhabit.netbus.NetStateReceiver;
import me.goldze.mvvmhabit.netbus.NetUtils;
import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;

/**
 * 网络检测 服务类
 * ①网络正常时：一分钟执行一次检测
 * ②网络异常时：默认先执行一次检测，然后按周期三分钟执行
 * 软复位：
 *      如果到达了指定的网络异常次数{@link #nowNetErrCount},方法{@link #resetSoftware4G(boolean)}，执行软复位
 * 硬复位：
 *      如果到达了指定的软复位执行的次数{@link #max4GResetCount},方法{@link #resetHardWare4G()}，则执行硬复位
 *
 * 注：网络正常时重置所有状态
 * 注：网络正常后又异常则执行②
 *
 */
public class NetWorkSecondService extends Service {
    public static final String TAG = NetWorkSecondService.class.getSimpleName();
    private static final String FLAG_RESET = "reset";
    //监听停止该服务的广播
    public static final String ACTION_CLOSE_ALL = "com.close.service.and.notification1";
    private int nowNetErrCount = 0;//记录当前网络异常的次数 临时变量
    private int netUserSetErrCount = 1;//网络异常复位前需要达到的异常次数
    private int now4GResetCount = 0;//4G网络模块重置的次数 临时变量 网络正常时重置为0
    private int max4GResetCount = 10;//硬件复位的条件：当模块重置3次过后
    private int cycleInterval;//按设定的周期检查网络
    private int nowRecodeTime = 0;//当前记录的分钟
    private String urlAddr = "";//默认ping的地址
    private String commandToReset =  "echo 1 > /dev/lte_state";//硬复位
    private String commandToReset2 = "setprop rild.simcom.reboot 1";//软复位
    private Context mContext;//上下文
    private Disposable sDisposable1;//控制线程
    private Disposable sDisposable2;//控制线程
    private Disposable sDisposable3;//控制线程
    private boolean isRunningTask1 = false; //网络正常时检测的线程
    private boolean isRunningTask2 = false; //网络异常时检测的线程
    //系统通知
    private NotificationManager manager = null;
    private Notification.Builder builder = null;
    //自定义的系统通知视图
    private RemoteViews contentView = null;
    /**
     * 网络观察者
     */
    protected NetChangeObserver mNetChangeObserver = null;
    static NetWorkSecondService netWorkMonitorService;

    //单例
    public static NetWorkSecondService getInstance() {
        if (netWorkMonitorService == null) {
            netWorkMonitorService = new NetWorkSecondService();
        }
        return netWorkMonitorService;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //进程被杀之后重新再执行
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        NetUtils.NetType netType = NetUtils.getAPNType(mContext);
        int errCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_COUNT, 0);
        manager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        builder = new Notification.Builder(mContext);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 1, new Intent(ACTION_CLOSE_ALL), PendingIntent.FLAG_UPDATE_CURRENT);
        contentView = new RemoteViews(getApplication().getPackageName(), R.layout.activity_notification);
        contentView.setTextViewText(R.id.tvNetType, "网络类型:" + netType.name());
        contentView.setTextViewText(R.id.tvNextRebootTime, "异常重启时间:3m后重置" + max4GResetCount + "次后无效");
        contentView.setTextViewText(R.id.tvNetErrCount, "网络总异常:" + errCount + "次");
        contentView.setTextViewText(R.id.tvExeuTime, "已执行时间:" + nowRecodeTime + "m");
        contentView.setOnClickPendingIntent(R.id.btnClose, pendingIntent);
        builder.setContent(contentView);
        builder.setSmallIcon(R.mipmap.network);  //小图标，在大图标右下角
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.network)); //大图标，没有设置时小图标就是大图标
        builder.setOngoing(true);//滑动不能清除
        builder.setAutoCancel(false);   //点击的时候消失
        manager.notify(12, builder.build());  //参数一为ID，用来区分不同APP的Notification

        // 网络改变的一个回调类
        mNetChangeObserver = new NetChangeObserver() {
            @Override
            public void onNetConnected(NetUtils.NetType type) {
                /*boolean isMobileConn = NetUtils.isNet4GConnted(mContext);
                KLog.e(TAG, type.name() + ",isMobileConn=" + isMobileConn);
                if (isMobileConn) {
                    NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(mContext, urlAddr);
                    if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {
                        //开启startTimerTask1()
                        //关闭startTimerTask2()
                        closeTimerTask2();
                        if (!isRunningTask1) {
                            KLog.e(TAG, "网络已连接 恢复正常 正在刷新线程状态 " + type.name());
                            startTimerTask1(0);
                        }else{
                            KLog.e(TAG, "startTimerTask1 已经在执行");
                        }
                    } else {
                        closeTimerTask1();
                        open4GNetWork(0);
                        if (!isRunningTask2) {
                            KLog.e(TAG, "网络已连接 但未能正常访问  正在刷新线程状态 " + type.name());
                            startTimerTask2();
                        } else {
                            KLog.e(TAG, "startTimerTask2 已经在执行");
                        }
                    }
                } else {
                    KLog.e(TAG, "onNetConnected() 当前网络非4G网络 请检查天线和SIM卡是否正常接入");
                    closeTimerTask1();
                    open4GNetWork(0);
                    if (!isRunningTask2) {
                        startTimerTask2();
                    } else {
                        KLog.e(TAG, "startTimerTask2 已经在执行");
                    }
                }*/
            }

            @Override
            public void onNetDisConnect() {
                /*
                KLog.e(TAG, "网络已断开 正在刷新线程状态");
                if (!isRunningTask2) {
                    startTimerTask2();
                }else{
                    KLog.e(TAG, "startTimerTask2 已经在执行");
                }*/
            }
        };
        //开启广播去监听 网络 改变事件
        NetStateReceiver.registerObserver(mNetChangeObserver);

        initSomeParam();//初始化基本参数

        startTimerTask1(3);//开启网络检测

        reSetAndReboot();//4G模块复位

        startCountTime();//整点复位

        listenerParamSet();//监听参数设置

        closeTask();//执行关闭线程
    }


    /**
     * 初始化一些基本参数
     */
    private void initSomeParam() {
        urlAddr = SPUtils.getInstance().getString(KeyValueConst.ADDR, NetworkUtil.url);
        KLog.e(TAG, "您当前设置的访问地址为：" + urlAddr);
        cycleInterval = SPUtils.getInstance().getInt(KeyValueConst.CYCLE_INTERVAL, 3);
        KLog.e(TAG, "循环判断网络的间隔时间为：" + cycleInterval + " 分钟");
        netUserSetErrCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_SCAN_COUNT, 1);
        KLog.e(TAG, "设置的异常扫描次数为:" + netUserSetErrCount);
    }

    /**
     * 4g模块复位，并且重启
     */
    public void reSetAndReboot() {
        RxBus.getDefault().toObservable(String.class).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                if (s.equals(FLAG_RESET)) {
                    //当前软复位的执行次数达到10次之后，需要执行硬件复位
                    //KLog.e(TAG,"当前异常次数："+now4GResetCount);
                    if (now4GResetCount+1==max4GResetCount) {
                        KLog.e(TAG, "当前已执行软复位4G模块" + max4GResetCount + "次,网络还是没有恢复正常，现在执行硬复位...");
                        resetHardWare4G();
                    } else {
                        resetSoftware4G(true);
                    }
                }
                nowNetErrCount = 0; //当前网络异常的次数清为0
            }
        });
    }

    /**
     * 重置4G模块 硬件复位
     *  硬件复位时不执行now4GResetCount 当前的复位次数加一
     */
    private void resetHardWare4G() {
        KLog.e(TAG,"当前软复位到达了10次，接着执行硬复位");
        KLog.e(TAG, "硬复位执行 command=" + commandToReset);
        ShellUtils.CommandResult resetCommand = ShellUtils.execCommand(commandToReset, false);
        KLog.e(TAG, resetCommand.result == 0 ? "复位成功" : "复位失败 errMeg=" + resetCommand.errorMsg);
        now4GResetCount++;
        KLog.e(TAG, "当前4G模块已重置的次数为→now4GResetCount=" + now4GResetCount);
    }

    /**
     * 重置4G模块 软件复位
     */
    private void resetSoftware4G(boolean isAddCount) {
        //KLog.e(TAG, "isAddCount=" + isAddCount);
        KLog.e(TAG, "软复位执行 command=" + commandToReset2);
        ShellUtils.CommandResult result0 = ShellUtils.execCommand(commandToReset2, true);
        KLog.e(result0.result == 0 ? "复位成功" : "errMeg0=" + result0.errorMsg);
        //4G模块复位后就+1
        if (isAddCount) {
            now4GResetCount++;
            KLog.e(TAG, "当前4G模块已重置的次数为→now4GResetCount=" + now4GResetCount);
        }
    }


    /**
     * 监听设置的参数
     * 开机判断当前网络是否是4G功能。是的话要3分钟去判断一次是否网络正常。
     * 如果出现不能上网的情况。要每2分钟扫描一次，看看网络状态是否回复。
     * 查看调用情况 请查看MainAtyViewModule： RxBus.getDefault().post(new NetTimerParamEntity());
     * 相当于发起一个通知，可以传递参数等，可在实体类设置
     */
    public void listenerParamSet() {
        RxBus.getDefault().toObservable(NetTimerParamEntity.class).subscribe(new Consumer<NetTimerParamEntity>() {
            @Override
            public void accept(NetTimerParamEntity netTimerParamEntity) throws Exception {
                //重新调用 以重新获取更新设置的参数
                KLog.e(TAG, "执行线程重启的方法");
                closeAllTimerTask();//先关闭线程
                initSomeParam();//初始化一些基本参数
                startTimerTask1(0);
            }
        });
    }

    /**
     * 开始计时 到达整点 凌晨两点时执行软复位
     */
    public void startCountTime() {
        sDisposable3 = Observable
                .interval(1, TimeUnit.SECONDS)
                //取消任务时取消定时唤醒
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {

                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long count) throws Exception {
                        resetPoint();
                    }
                });
    }

    /**
     * 整点重置
     */
    public void resetPoint() {
        Calendar nowCalendar = Calendar.getInstance();
        int nowHour = nowCalendar.get(Calendar.HOUR_OF_DAY);
        int nowMinute = nowCalendar.get(Calendar.MINUTE);
        int nowSeconds = nowCalendar.get(Calendar.SECOND);
        if (nowHour == 2 && nowMinute == 0 && nowSeconds == 0) {
            //如果时间到达凌晨两点时执行软件复位
            resetSoftware4G(true);
        }
    }


    /**
     * 网络正常时按设定的时间周期检查网络是否会出现异常
     */
    public void startTimerTask1(int initialDelay) {
        isRunningTask1 = true;
        sDisposable1 = Observable
                .interval(initialDelay, 1, TimeUnit.MINUTES)
                //取消任务时取消定时唤醒
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        isRunningTask1 = false;
                        KLog.e(TAG, "startTimerTask1 closed");

                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long count) throws Exception {
                        int totalCount = SPUtils.getInstance().getInt(KeyValueConst.TOTAL_COUNT, 0);
                        SPUtils.getInstance().put(KeyValueConst.TOTAL_COUNT, totalCount + 1);
                        //判断当前线程是否需要运行
                        //一般情况下 两个线程不会并行
                        //判断当前是否是手机网络连接  并且网络为4G
                        boolean isMobileConn = NetUtils.isNet4GConnted(mContext);
                        NetUtils.NetType netType=NetUtils.getAPNType(mContext);
                        KLog.e(TAG, "1 是否为4G:" + isMobileConn+",netType="+netType.name());
                        if (isMobileConn||((netType!= NetUtils.NetType.WIFI)&&(netType!= NetUtils.NetType.ETH))) {
                            //如果当前网络为4G
                            //获取当前网络状态
                            NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(mContext, urlAddr);
                            if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {//如果网络正常 能够正常访问网络
                                netConnSuccess();
                            } else {
                                KLog.e(TAG, "1当前网络状态：" + netStatus.name());
                                KLog.e(TAG, "1当前网络异常 执行下一步");
                                closeTimerTask1();
                                open4GNetWork(0);
                                startTimerTask2();
                            }
                        } else {
                            KLog.e(TAG, "1当前网络非4G网络 请检查天线和SIM卡是否正常接入");
                        }
                    }
                });
    }

    /**
     * 开启4G网络
     */
    public void open4GNetWork(int type) {
        ShellUtils.CommandResult networkBy4GOpenResult = ShellUtils.execCommand("svc data enable", true);
        //KLog.e(TAG, networkBy4GOpenResult.result == 0 ? "开启4G网络成功！command=svc data enable" : "开启4G网络失败！command=svc data enable");
        ShellUtils.CommandResult networkBy4GOpenResult2 = ShellUtils.execCommand("start ril-daemon", true);
        //KLog.e(TAG, networkBy4GOpenResult2.result == 0 ? "开启4G网络成功！start ril-daemon" : "开启4G网络失败！start ril-daemon");
        //KLog.e(TAG, "type=" + type);
    }

    /**
     * 网络异常时按设定的次数每隔{@link #cycleInterval}分钟检测一次的线程
     */
    public void startTimerTask2() {
        isRunningTask2 = true;
        sDisposable2 = Observable
                .interval(0, cycleInterval, TimeUnit.MINUTES)
                //取消任务时取消定时唤醒
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        KLog.e(TAG, "startTimerTask2 closed");
                        isRunningTask2 = false;
                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long count) throws Exception {
                        //总次数加1
                        int totalCount = SPUtils.getInstance().getInt(KeyValueConst.TOTAL_COUNT, 0);
                        SPUtils.getInstance().put(KeyValueConst.TOTAL_COUNT, totalCount + 1);
                        //判断网络是否为4G
                        boolean isMobileConn = NetUtils.isNet4GConnted(mContext);
                        NetUtils.NetType netType=NetUtils.getAPNType(mContext);
                        KLog.e(TAG, "2 是否为4G:" + isMobileConn+",netType="+netType.name());
                        if (isMobileConn||((netType!= NetUtils.NetType.WIFI)&&(netType!= NetUtils.NetType.ETH))) {
                            NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(mContext, urlAddr);
                            KLog.e(TAG, "2当前网络状态：" + netStatus.name());
                            if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {
                                netConnSuccess();
                                //执行终止命令
                                //继续执行第一个线程 检查网络是否会存在异常
                                closeTimerTask2();
                                //KLog.e(TAG, "2任务需要终止>isRunningTask2:" + false + ",isRunningTask1:" + true);
                                startTimerTask1(0);
                            } else {
                                netConnFailed(nowRecodeTime);
                                //KLog.e(TAG, "2网络异常 网络异常复位前需要达到的异常次数：" + netUserSetErrCount + ",用于做4G模块复位前的判断,当前网络异常次数为：" + nowNetErrCount);
                                if (nowNetErrCount == netUserSetErrCount) {
                                    //KLog.e(TAG, "2网络无法恢复！达到了指定的异常网络次数:" + netUserSetErrCount + "次");
                                    //4G模块复位
                                    RxBus.getDefault().post(FLAG_RESET);
                                }
                            }
                        } else {
                            KLog.e(TAG, "2 当前网络非4G 请检查天线和SIM卡是否正常接入 任务需要终止>>>");
                            //当前网络非4G情况下
                            //执行终止命令
                            //继续执行第一个线程 检查网络是否会存在异常
                            closeTimerTask2();
                            startTimerTask1(0);
                        }
                    }
                });
    }

    /**
     * 4G网络正常时的操作
     *
     * @see #nowNetErrCount 记录当前网络异常的次数
     * @see #now4GResetCount 4G网络模块重置的次数 临时变量 网络正常时重置为0
     */
    public void netConnSuccess() {
        KLog.e(TAG, "网络正常了，这里需要重置一些数据！");
        //清除记录的时间
        nowNetErrCount = 0;
        nowRecodeTime = 0;
        now4GResetCount = 0;
        SPUtils.getInstance().put(KeyValueConst.LAST_STATUS, true);
        //清除当前保存的重启次数
        //SPUtils.getInstance().put(KeyValueConst.REBOOT_COUNT, 0);
        RxBus.getDefault().post(new TotalEntity());
        //将保存的重启的阀值周期清零
        SPUtils.getInstance().put(KeyValueConst.DYNAMIC_TIME, 0);
        //恢复默认的显示
        NetUtils.NetType netType = NetUtils.getAPNType(mContext);
        contentView.setTextViewText(R.id.tvNetType, "网络类型:" + netType.name());
        contentView.setTextViewText(R.id.tvNextRebootTime, "异常重启时间:3m后重置4G" + max4GResetCount + "次后无效");
        contentView.setTextViewText(R.id.tvNetErrCount, "网络总异常:0次");
        contentView.setTextViewText(R.id.tvExeuTime, "已执行时间:0m");
        manager.notify(12, builder.build());
    }

    /**
     * 4G网络异常时的操作
     */
    public void netConnFailed(int nowRecodeTime) {
        nowNetErrCount++;
        int totalCount = SPUtils.getInstance().getInt(KeyValueConst.TOTAL_COUNT, 0);
        int errCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_COUNT, 0);
        boolean isCountFrist = SPUtils.getInstance().getBoolean(KeyValueConst.IS_NET_ERR_FIRST_COUNT, true);
        KLog.d(TAG, "isCountFrist=" + isCountFrist);
        if (isCountFrist) {
            //第一次记录错误次数
            SPUtils.getInstance().put(KeyValueConst.IS_NET_ERR_FIRST_COUNT, false);
            SPUtils.getInstance().put(KeyValueConst.ERR_COUNT, errCount + 1);
            KLog.e(TAG, "网络检测的总次数为：" + totalCount + ",异常掉网的次数为：" + (errCount + 1));
        } else {
            if (SPUtils.getInstance().getBoolean(KeyValueConst.LAST_STATUS, false)) {
                SPUtils.getInstance().put(KeyValueConst.ERR_COUNT, errCount + 1);
                //KLog.e(TAG, "网络检测的总次数为：" + totalCount + ",异常掉网的次数为：" + (errCount + 1));
                SPUtils.getInstance().put(KeyValueConst.LAST_STATUS, false);
            } else {
                KLog.e(TAG, "由于一直处在掉网情况，不做统计次数");
                KLog.e(TAG, "网络检测的总次数为：" + totalCount + ",异常掉网的次数为：" + errCount);
            }
        }
        NetUtils.NetType netType = NetUtils.getAPNType(mContext);
        contentView.setTextViewText(R.id.tvNetType, "网络类型:" + netType.name());
        int dynamicTime = SPUtils.getInstance().getInt(KeyValueConst.DYNAMIC_TIME, 0);
        contentView.setTextViewText(R.id.tvNextRebootTime, "异常重启时间:" + ((nowRecodeTime == 0) ? "加载中..." : dynamicTime + "m后"));
        contentView.setTextViewText(R.id.tvNetErrCount, "网络总异常:" + (errCount + 1) + "次");
        contentView.setTextViewText(R.id.tvExeuTime, "已执行时间:" + nowRecodeTime + "m");
        manager.notify(12, builder.build());
        RxBus.getDefault().post(new TotalEntity());
    }

    /**
     * 关闭线程1
     */
    public void closeTimerTask1() {
        if (sDisposable1 != null) sDisposable1.dispose();
    }

    /**
     * 关闭线程2
     */
    public void closeTimerTask2() {
        if (sDisposable2 != null) sDisposable2.dispose();
    }

    /**
     * 关闭线程3
     */
    public void closeTimerTask3() {
        if (sDisposable3 != null) sDisposable3.dispose();
    }

    /**
     * 关闭所有线程
     */
    public void closeTask() {
        RxBus.getDefault().toObservable(ThreadNotice.class).subscribe(new Consumer<ThreadNotice>() {
            @Override
            public void accept(ThreadNotice threadNotice) throws Exception {
                closeAllTimerTask();
            }
        });
    }

    /**
     * 关闭相关Servie内容
     */
    public void stopService() {
        stopSelf();
        closeAllTimerTask();
        //关闭通知
        if (manager != null) {
            manager.cancel(11);
        }
    }

    /**
     * 关闭所有线程
     */
    public void closeAllTimerTask() {
        KLog.e(TAG, "关闭所有线程");
        closeTimerTask1();
        closeTimerTask2();
        closeTimerTask3();
    }

    /**
     * 销毁
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (manager != null) {
            manager.cancel(12);
        }
        closeAllTimerTask();
    }
}
