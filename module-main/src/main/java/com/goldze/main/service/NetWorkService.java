package com.goldze.main.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.chtj.base_iotutils.ShellUtils;
import com.goldze.main.entity.NetTimerParamEntity;
import com.goldze.main.entity.ServiceAboutEntity;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.disposables.Disposable;
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
 */
public class NetWorkService extends Service {
    public static final String TAG=NetWorkService.class.getSimpleName();
    private int initialCount = 0;//网络异常时的次数累加
    boolean isRunningTask1 = false, isRunningTask2 = false;
    int netUserSetErrCount=5;//用户设置 网络异常是需要扫描的次数

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
        // 网络改变的一个回掉类
        mNetChangeObserver = new NetChangeObserver() {
            @Override
            public void onNetConnected(NetUtils.NetType type) {
                int netStatus = NetworkUtil.getNetState(NetWorkService.this);
                if(netStatus==NetworkUtil.NET_CNNT_BAIDU_OK){
                    //如果网络已连接 并且能够正常访问
                    KLog.e(TAG,"网络已连接 恢复正常 正在刷新线程状态 " + type.name());
                    isRunningTask2 = false;//关闭线程2
                    isRunningTask1 = true;//开启线程1
                }else{
                    KLog.e(TAG,"网络已连接 但未能正常访问  正在刷新线程状态 " + type.name());
                   isRunningTask2=true;
                   isRunningTask1=false;
                }

            }

            @Override
            public void onNetDisConnect() {
                KLog.e(TAG,"网络已断开 正在刷新线程状态");
                isRunningTask2=true;
                isRunningTask1=false;
            }
        };
        //开启广播去监听 网络 改变事件
        NetStateReceiver.registerObserver(mNetChangeObserver);
        //初始化线程
        initTimerTask();

        reSetAndReboot();//4G模块复位

        listenerParamSet();//监听参数设置

        listenerCloseService();//监听是否需要关闭服务
    }


    Timer timer1, timer2;
    TimerTask timerTask1, timerTask2;

    /**
     * 开机判断当前网络是否是4G。是的话要
     * 按设置的(ps:3)分钟去判断一次是否网络正常
     */
    public void initTimerTask() {
        timerTask1 = new TimerTask() {
            @Override
            public void run() {
                //判断当前线程是否需要运行
                //一般情况下 两个线程不会并行
                //判断当前是否是手机网络连接  并且网络为4G
                boolean isMobileConn=NetUtils.isNet4GConnted(NetWorkService.this);
                if (isRunningTask1&&isMobileConn) {
                    //获取当前网络状态
                    int netStatus = NetworkUtil.getNetState(NetWorkService.this);
                    if (netStatus == NetworkUtil.NET_CNNT_BAIDU_OK) {//如果网络正常 能够正常访问网络
                        KLog.e(TAG,"1能够正常访问网络 是否为4G:"+isMobileConn);
                    } else {
                        String message="";
                        if (netStatus == NetworkUtil.NET_CNNT_BAIDU_TIMEOUT) {
                            message="1网络连接超时";
                        } else if (netStatus == NetworkUtil.NET_NOT_PREPARE) {
                            message="1网络未准备好";
                        } else {
                            message="1网络异常";
                        }
                        KLog.e(TAG,message);
                        isRunningTask1 = false;//关闭第一个线程
                        isRunningTask2 = true;//开启第二个线程
                        KLog.e(TAG,"1当前网络异常 执行下一步");
                    }
                }

            }
        };
        timerTask2 = new TimerTask() {
            @Override
            public void run() {
                if (isRunningTask2) {
                    int netStatus = NetworkUtil.getNetState(NetWorkService.this);
                    String message="";
                    if (netStatus == NetworkUtil.NET_CNNT_BAIDU_TIMEOUT) {
                        message="1网络连接超时";
                    } else if (netStatus == NetworkUtil.NET_NOT_PREPARE) {
                        message="1网络未准备好";
                    } else {
                        message="1网络异常";
                    }
                    KLog.e(TAG,"2当前网络状态：" + message);
                    if (netStatus == NetworkUtil.NET_CNNT_BAIDU_OK) {
                        initialCount = 0;
                        //执行终止命令
                        isRunningTask2 = false;
                        //继续执行第一个线程 检查网络是否会存在异常
                        isRunningTask1 = true;
                        KLog.e(TAG,"2任务需要终止>isRunningTask2:" + isRunningTask2 + ",isRunningTask1:" + isRunningTask1);
                    } else {
                        initialCount++;
                        KLog.e(TAG,"2网络异常，继续检测 当前检测的次数为："+initialCount);
                        //例如：
                        //如果遇到恢复不了的情况，要重复扫描5次。也就是10分钟。
                        //netErrCount次是不能连上网络的话，就要给4G模块复位。重新断电上电。
                        if (initialCount == netUserSetErrCount) {
                            //4G模块复位
                            RxBus.getDefault().post("reset");
                        }
                    }
                }
            }
        };
        int cycleInterval = SPUtils.getInstance().getInt("cycleInterval", 3);
        KLog.e(TAG,"初始化循环判断网络的间隔时间为：" + cycleInterval);
        isRunningTask1 = true;
        timer1 = new Timer();
        timer1.schedule(timerTask1, 1000, cycleInterval* 60* 1000);

        netUserSetErrCount= SPUtils.getInstance().getInt("errScanCount", 2);
        KLog.e(TAG,"2设置的扫描扫描次数为:" + netUserSetErrCount);
        isRunningTask2 = false;
        timer2 = new Timer();
        timer2.schedule(timerTask2, 1000, 2 *60 * 1000);
    }

    /**
     * 4g模块复位，并且重启
     */
    public void reSetAndReboot() {
        Disposable disposable = RxBus.getDefault().toObservable(String.class).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                if (s.equals("reset")) {
                    KLog.e(TAG,"执行4G模块复位");
                    ShellUtils.CommandResult resetCommand = ShellUtils.execCommand(/*"echo 1 > /sys/class/spi_sim_ctl/state"*/"echo 1 > /sys/devices/platform/imx6q_sim/sim_sysfs/state", false);
                    KLog.e("resetCommand result:" + resetCommand.result + ",successMeg:" + resetCommand.successMsg + ",errMeg:" + resetCommand.errorMsg);

                    String message="";
                    if (resetCommand.result == 0) {
                        message="复位成功";
                    } else {
                        message="复位失败";
                    }
                    KLog.e(TAG,message);
                    initialCount=0;//清除次数 重新再次计算次数
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
                KLog.e(TAG,"执行线程重启的方法");
                cancelAllTask();//先关闭线程
                initTimerTask();//重启线程
            }
        });
    }

    /**
     * 监听是否需要关闭服务
     */
    public void listenerCloseService(){
        RxBus.getDefault().toObservable(ServiceAboutEntity.class).subscribe(new Consumer<ServiceAboutEntity>() {
            @Override
            public void accept(ServiceAboutEntity serviceAboutEntity) throws Exception {
                stopSelf();
                //调用onDestory
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelAllTask();
    }

    /**
     * 关闭所有线程
     */
    public void cancelAllTask() {
        KLog.e(TAG,"关闭所有线程");
        isRunningTask2=false;
        isRunningTask1=false;
        if (timer1 != null) {
            timer1.cancel();
            timer1 = null;
        }
        if (timer2 != null) {
            timer2.cancel();
            timer2 = null;
        }
        if (timerTask1 != null) {
            timerTask1.cancel();
            timerTask1 = null;
        }
        if (timerTask2 != null) {
            timerTask2.cancel();
            timerTask2 = null;
        }
    }
}
