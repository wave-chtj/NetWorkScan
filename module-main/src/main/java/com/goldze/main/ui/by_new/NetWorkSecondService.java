package com.goldze.main.ui.by_new;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.chtj.base_iotutils.ShellUtils;
import com.goldze.main.entity.NetTimerParamEntity;
import com.goldze.main.entity.ThreadNotice;
import com.goldze.main.entity.TotalEntity;
import com.goldze.main.utils.KeyValueConst;

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
 * <p>
 * "/dev/lte_state" 所有4G复位都用这个
 */
public class NetWorkSecondService extends Service {
    public static final String TAG = NetWorkSecondService.class.getSimpleName();
    private int nowNetErrCount = 0;//记录当前网络异常的次数
    private int netUserSetErrCount = 2;//用户设置 网络异常时需要扫描的次数
    private int net4GResetCount = 0;//4G网络模块重置的次数 临时变量 网络正常时重置为0
    private int moduleResetCount = 3;//重启设备的条件：当模块重置3次过后
    private int netErrCycleTime=3;//网络异常时的周期
    private int cycleInterval;//按设定的周期检查网络
    private String urlAddr = "";//默认ping的地址
    private String commandToReset = "echo 1 > /dev/lte_state";
    private String commandReboot = "reboot";
    private Context mContext;
    private Disposable sDisposable1;
    private Disposable sDisposable2;
    //isRunningTask1 网络正常时检测的线程
    private boolean isRunningTask1 = false;
    //isRunningTask2 网络异常时检测的线程
    private boolean isRunningTask2 = false;
    /**
     * 网络观察者
     */
    protected NetChangeObserver mNetChangeObserver = null;

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
        // 网络改变的一个回调类
        mNetChangeObserver = new NetChangeObserver() {
            @Override
            public void onNetConnected(NetUtils.NetType type) {
                NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(mContext, urlAddr);
                if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {
                    //如果网络已连接 并且能够正常访问
                    closeTimerTask2();
                    if (!isRunningTask1) {
                        //清除网络异常时重置4G模块的次数
                        net4GResetCount = 0;
                        KLog.e(TAG, "网络已连接 恢复正常 正在刷新线程状态 " + type.name());
                        startTimerTask1();
                    }
                } else {
                    if (!isRunningTask2) {
                        KLog.e(TAG, "网络已连接 但未能正常访问  正在刷新线程状态 " + type.name());
                        startTimerTask2();
                    }
                    closeTimerTask1();
                }
            }

            @Override
            public void onNetDisConnect() {
                if (!isRunningTask2) {
                    KLog.e(TAG, "网络已断开 正在刷新线程状态");
                    startTimerTask2();
                }
                closeTimerTask1();
            }
        };
        //开启广播去监听 网络 改变事件
        NetStateReceiver.registerObserver(mNetChangeObserver);

        initSomeParam();//初始化基本参数

        startTimerTask1();//开启网络检测

        reSetAndReboot();//4G模块复位

        listenerParamSet();//监听参数设置

        closeTask();//执行关闭线程
    }


    /**
     * 初始化一些基本参数
     */
    private void initSomeParam() {
        urlAddr = SPUtils.getInstance().getString(KeyValueConst.ADDR, NetworkUtil.url);
        KLog.e(TAG, "您当前设置的访问地址为：" + urlAddr);
        cycleInterval = SPUtils.getInstance().getInt(KeyValueConst.CYCLE_INTERVAL, 1);
        KLog.e(TAG, "循环判断网络的间隔时间为：" + cycleInterval + " 分钟");
        netUserSetErrCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_SCAN_COUNT, 2);
        KLog.e(TAG, "设置的异常扫描次数为:" + netUserSetErrCount + ",间隔时间为："+netErrCycleTime+"分钟");
    }

    /**
     * 4g模块复位，并且重启
     */
    public void reSetAndReboot() {
        RxBus.getDefault().toObservable(String.class).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                if (s.equals("reset")) {
                    KLog.e(TAG, "执行4G模块复位 命令为：command1=" + commandToReset);
                    ShellUtils.CommandResult resetCommand = ShellUtils.execCommand(commandToReset, false);
                    KLog.e(TAG, resetCommand.result == 0 ? "复位成功" : "复位失败 errMeg=" + resetCommand.errorMsg);
                    //4G模块复位后就+1
                    net4GResetCount++;
                    if (net4GResetCount == moduleResetCount) {
                        KLog.e(TAG, "当前已重置过4G模块" + moduleResetCount + "次,网络还是没有恢复正常，现在重启...");
                        //如果重置4G模块的次数到达三次
                        //网络还未恢复正常 则重启
                        //如果重启的次数达到一定次数
                        KLog.e(TAG, "执行重启设备 命令为：command2=" + commandReboot);
                        ShellUtils.CommandResult rebootResult = ShellUtils.execCommand(commandReboot, true);
                        KLog.e(TAG, rebootResult.result == 0 ? "复位成功" : "复位失败 errMeg=" + rebootResult.errorMsg);
                    }
                    nowNetErrCount = 0; //当前网络异常的次数清为0
                }
            }
        });
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
                startTimerTask1();
            }
        });
    }


    /**
     * 网络正常时按设定的时间周期检查网络是否会出现异常
     */
    public void startTimerTask1() {
        isRunningTask1 = true;
        sDisposable1 = Observable
                .interval(0, cycleInterval, TimeUnit.MINUTES)
                //取消任务时取消定时唤醒
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        isRunningTask1 = false;
                        KLog.e(TAG, "startTimerTask1 closed");
                        open4GNetWork(0);
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
                        if (isMobileConn) {//如果当前网络为4G
                            //获取当前网络状态
                            NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(mContext, urlAddr);
                            if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {//如果网络正常 能够正常访问网络
                                KLog.e(TAG, "1能够正常访问网络 是否为4G:" + isMobileConn);
                                netConnSuccess();
                            } else {
                                KLog.e(TAG, "1当前网络状态：" + netStatus.name());
                                closeTimerTask1();
                                startTimerTask2();
                                KLog.e(TAG, "1当前网络异常 执行下一步");
                            }
                        } else {
                            KLog.e(TAG, "1当前网络非4G网络 请检查");
                            open4GNetWork(1);
                        }
                    }
                });
    }

    /**
     * 开启4G网络
     */
    public void open4GNetWork(int type) {
        ShellUtils.CommandResult networkBy4GOpenResult = ShellUtils.execCommand("svc data enable", true);
        KLog.e(TAG, networkBy4GOpenResult.result == 0 ? "开启4G网络成功！command=svc data enable" : "开启4G网络失败！command=svc data enable");
        KLog.e(TAG,"type="+type);
    }

    /**
     * 网络异常时按设定的次数每隔{@link #netErrCycleTime}分钟检测一次的线程
     */
    public void startTimerTask2() {
        isRunningTask2 = true;
        sDisposable2 = Observable
                .interval(0, netErrCycleTime, TimeUnit.MINUTES)
                //取消任务时取消定时唤醒
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        KLog.e(TAG, "startTimerTask2 closed");
                        isRunningTask2 = false;
                        nowNetErrCount = 0;//清除次数 重新再次计算次数
                        startTimerTask1();
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
                        if (isMobileConn) {
                            NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(mContext, urlAddr);
                            KLog.e(TAG, "2当前网络状态：" + netStatus.name());
                            if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {
                                netConnSuccess();
                                //执行终止命令
                                //继续执行第一个线程 检查网络是否会存在异常
                                closeTimerTask2();
                                startTimerTask1();
                                KLog.e(TAG, "2任务需要终止>isRunningTask2:" + false + ",isRunningTask1:" + true);
                            } else {
                                netConnFailed();
                                KLog.e(TAG, "2网络异常，网络异常需要检测总次数为：" + netUserSetErrCount + ", 当前网络异常次数为：" + nowNetErrCount);
                                //例如：
                                //如果遇到恢复不了的情况，要重复扫描5次。也就是10分钟。
                                //netErrCount次是不能连上网络的话，就要给4G模块复位。重新断电上电。
                                if (nowNetErrCount == netUserSetErrCount) {
                                    KLog.e(TAG, "2网络无法恢复,即将执行4G复位操作！");
                                    //4G模块复位
                                    RxBus.getDefault().post("reset");
                                }
                            }
                        } else {
                            KLog.e(TAG, "2 当前网络非4G 任务需要终止>>>");
                            //当前网络非4G情况下
                            //执行终止命令
                            //继续执行第一个线程 检查网络是否会存在异常
                            closeTimerTask2();
                        }
                    }
                });
    }

    /**
     * 4G网络正常时的操作
     * @see #nowNetErrCount 记录当前网络异常的次数
     * @see #net4GResetCount 4G网络模块重置的次数 临时变量 网络正常时重置为0
     */
    public void netConnSuccess() {
        nowNetErrCount = 0;
        net4GResetCount = 0;
        SPUtils.getInstance().put(KeyValueConst.LAST_STATUS, true);
        RxBus.getDefault().post(new TotalEntity());
    }

    /**
     * 4G网络异常时的操作
     */
    public void netConnFailed() {
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
                KLog.e(TAG, "网络检测的总次数为：" + totalCount + ",异常掉网的次数为：" + (errCount + 1));
                SPUtils.getInstance().put(KeyValueConst.LAST_STATUS, false);
            } else {
                KLog.e(TAG, "由于一直处在掉网情况，所以不做统计，等待下一次连接正常之后的异常才做异常次数加一");
                KLog.e(TAG, "网络检测的总次数为：" + totalCount + ",异常掉网的次数为：" + errCount);
            }
        }
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
     * 关闭所有线程
     */
    public void closeAllTimerTask() {
        KLog.e(TAG, "关闭所有线程");
        closeTimerTask1();
        closeTimerTask2();
    }

    /**
     * 销毁
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAllTimerTask();
    }
}
