package com.goldze.main.ui.by_old;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.chtj.base_iotutils.ShellUtils;
import com.goldze.main.entity.NetTimerParamEntity;
import com.goldze.main.entity.ThreadNotice;
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
 * 现有机型
 * "echo 1 > /sys/class/spi_sim_ctl/state",//rk3399
 * "echo 1 > /sys/devices/platform/imx6q_sim/sim_sysfs/state"//飞思卡尔
 *
 * "/dev/lte_state"
 */
public class NetWorkService extends Service {
    public static final String TAG = NetWorkService.class.getSimpleName();
    private int initialCount = 0;//网络异常时的次数累加
    int netUserSetErrCount = 5;//用户设置 网络异常时需要扫描的次数
    int cycleInterval;//按设定的周期检查网络
    private String urlAddr = "";//默认ping的地址
    private String commandToReset = "echo 1 > /sys/devices/platform/imx6q_sim/sim_sysfs/state";
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
    public void onCreate() {
        super.onCreate();
        // 网络改变的一个回调类
        mNetChangeObserver = new NetChangeObserver() {
            @Override
            public void onNetConnected(NetUtils.NetType type) {
                NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(NetWorkService.this, urlAddr);
                if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {
                    //如果网络已连接 并且能够正常访问
                    closeTimerTask2();
                    if(!isRunningTask1){
                        KLog.e(TAG, "网络已连接 恢复正常 正在刷新线程状态 " + type.name());
                        startTimerTask1();
                    }
                } else {
                    if(!isRunningTask2){
                        KLog.e(TAG, "网络已连接 但未能正常访问  正在刷新线程状态 " + type.name());
                        startTimerTask2();
                    }
                    closeTimerTask1();
                }
            }

            @Override
            public void onNetDisConnect() {
                if(!isRunningTask2){
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

        listenerCloseService();//监听是否需要关闭服务
    }

    /**
     * 初始化一些基本参数
     */
    private void initSomeParam() {
        urlAddr = SPUtils.getInstance().getString(KeyValueConst.ADDR, NetworkUtil.url);
        KLog.e(TAG, "您当前设置的访问地址为：" + urlAddr);
        cycleInterval = SPUtils.getInstance().getInt(KeyValueConst.CYCLE_INTERVAL, 3);
        KLog.e(TAG, "循环判断网络的间隔时间为：" + cycleInterval + " 分钟");
        netUserSetErrCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_SCAN_COUNT, 5);
        KLog.e(TAG, "设置的异常扫描次数为:" + netUserSetErrCount + ",间隔时间为：2分钟");
    }

    /**
     * 4g模块复位，并且重启
     */
    public void reSetAndReboot() {
        RxBus.getDefault().toObservable(String.class).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                if (s.equals("reset")) {
                    KLog.e(TAG, "执行4G模块复位");
                    String command = SPUtils.getInstance().getString("typeCommand", commandToReset);
                    ShellUtils.CommandResult resetCommand = ShellUtils.execCommand(command, false);
                    KLog.e("resetCommand result:" + resetCommand.result + ",successMeg:" + resetCommand.successMsg + ",errMeg:" + resetCommand.errorMsg);
                    KLog.e(TAG, resetCommand.result==0?"复位成功":"复位失败,请留意是否机型选择错误！");
                    initialCount = 0;//清除次数 重新再次计算次数
                } else {

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
     * 监听是否需要关闭服务
     */
    public void listenerCloseService() {
        RxBus.getDefault().toObservable(ThreadNotice.class).subscribe(new Consumer<ThreadNotice>() {
            @Override
            public void accept(ThreadNotice threadNotice) throws Exception {
                stopSelf();
                //调用onDestory
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAllTimerTask();
    }


    public Disposable sDisposable1;
    public Disposable sDisposable2;
    //防止方法重复调用
    //当网络变化时，监听网络的广播会出现重复的广播
    //所以要防止网络检测的方法重复调用
    //isRunningTask1 网络检测的线程
    //isRunningTask2 网络异常时检测
    private boolean isRunningTask1 = false;
    private boolean isRunningTask2 = false;

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
                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long count) throws Exception {
                        //判断当前线程是否需要运行
                        //一般情况下 两个线程不会并行
                        //判断当前是否是手机网络连接  并且网络为4G
                        boolean isMobileConn = NetUtils.isNet4GConnted(NetWorkService.this);
                        if (isMobileConn) {//如果当前网络为4G
                            //获取当前网络状态
                            NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(NetWorkService.this, urlAddr);
                            if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {//如果网络正常 能够正常访问网络
                                KLog.e(TAG, "1能够正常访问网络 是否为4G:" + isMobileConn);
                            } else {
                                String message = "";
                                if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_BAIDU_TIMEOUT) {
                                    message = "网络连接超时";
                                } else if (netStatus == NetworkUtil.NET_TYPE.NET_NOT_PREPARE) {
                                    message = "网络未准备好";
                                } else {
                                    message = "网络异常";
                                }
                                KLog.e(TAG, "1当前网络状态：" + message);
                                closeTimerTask1();
                                startTimerTask2();
                                KLog.e(TAG, "1当前网络异常 执行下一步");
                            }
                        } else {
                            KLog.e(TAG, "1当前网络非4G网络 请检查");
                        }
                    }
                });
    }

    /**
     * 网络异常时按设定的次数每隔两分钟检测一次的线程
     */
    public void startTimerTask2() {
        isRunningTask2 = true;
        sDisposable2 = Observable
                .interval(0,2, TimeUnit.MINUTES)
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
                        NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(NetWorkService.this, urlAddr);
                        String message = "";
                        if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_BAIDU_TIMEOUT) {
                            message = "网络连接超时";
                        } else if (netStatus == NetworkUtil.NET_TYPE.NET_NOT_PREPARE) {
                            message = "网络未准备好";
                        } else {
                            message = "网络异常";
                        }
                        KLog.e(TAG, "2当前网络状态：" + message);
                        if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {
                            initialCount = 0;
                            //执行终止命令
                            //继续执行第一个线程 检查网络是否会存在异常
                            closeTimerTask2();
                            startTimerTask1();
                            KLog.e(TAG, "2任务需要终止>isRunningTask2:" + false + ",isRunningTask1:" + true);
                        } else {
                            initialCount++;
                            KLog.e(TAG, "2网络异常，继续检测 总次数为：" + netUserSetErrCount + ", 当前检测的次数为：" + initialCount);
                            //例如：
                            //如果遇到恢复不了的情况，要重复扫描5次。也就是10分钟。
                            //netErrCount次是不能连上网络的话，就要给4G模块复位。重新断电上电。
                            if (initialCount == netUserSetErrCount) {
                                KLog.e(TAG, "网络无法恢复,即将执行4G复位操作！");
                                //4G模块复位
                                RxBus.getDefault().post("reset");
                            }
                        }
                    }
                });
    }

    public void closeTimerTask1() {
        if (sDisposable1 != null) sDisposable1.dispose();
    }

    public void closeTimerTask2() {
        if (sDisposable2 != null) sDisposable2.dispose();
    }
    /**
     * 关闭所有线程
     */
    public void closeAllTimerTask() {
        KLog.e(TAG, "关闭所有线程");
        closeTimerTask1();
        closeTimerTask2();
    }


}
