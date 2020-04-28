package com.face.networkscan.ui;

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

import com.face.networkscan.R;
import com.face.networkscan.entity.ThreadNotice;
import com.face.networkscan.entity.TotalEntity;
import com.face.networkscan.utils.KeyValueConst;
import com.face_chtj.base_iotutils.ShellUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.http.NetworkUtil;
import me.goldze.mvvmhabit.netbus.NetUtils;
import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;

/**
 * 网络检测 服务类
 * <p>
 * "/dev/lte_state" 硬复位
 * 开机3分钟后自启动
 * 2分钟扫描网络一次。
 * 如果出现4G断网的情况，那就进行5次扫描。也就是10分钟之后给模块复位。
 * 4G恢复连网状态。从新开始判断。
 * 不做重启主控的操作
 */
public class NetWorkListenerService extends Service {
    public static final String TAG = NetWorkListenerService.class.getSimpleName();
    //监听停止该服务的广播
    public static final String ACTION_CLOSE_ALL = "com.close.service.and.notification";
    private int nowNetErrCount = 0;//记录当前网络异常的次数 临时变量
    private int netUserSetErrCount = 10;//用户设置 网络异常时需要扫描的次数
    private int now4GResetCount = 0;//4G网络模块重置的次数 临时变量 网络正常时重置为0
    private int max4GResetCount = 3;//重启设备的条件：当模块重置3次过后
    private int delayStartTime = 3;//service延迟启动的时间
    private int netErrCycleTime = 3;//网络异常时的周期
    private int cycleInterval = 2;//按设定的周期检查网络
    private String urlAddr = "";//默认ping的地址
    private String commandToReset1 = "echo 1 > /dev/lte_state";//硬复位
    private String commandToReset2 = "echo 0 > /sys/devices/platform/imx6q_sim/sim_sysfs/state";//硬复位
    private String commandReboot = "reboot";//不执行重启
    private Context mContext;
    private Disposable sDisposable1;
    private Disposable sDisposable2;
    //isRunningTask1 网络正常时检测的线程
    private boolean isRunningTask1 = false;
    //isRunningTask2 网络异常时检测的线程
    private boolean isRunningTask2 = false;
    //系统通知
    private NotificationManager manager = null;
    private Notification.Builder builder = null;
    //自定义的系统通知视图
    private RemoteViews contentView = null;
    static NetWorkListenerService netWorkMonitorService;
    //单例
    public static NetWorkListenerService getInstance() {
        if (netWorkMonitorService == null) {
            netWorkMonitorService = new NetWorkListenerService();
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
        KLog.e(TAG, "Service onCreate()");
        NetUtils.NetType netType = NetUtils.getAPNType(mContext);
        int errCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_COUNT, 0);
        manager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        builder = new Notification.Builder(mContext);
        contentView = new RemoteViews(getApplication().getPackageName(), R.layout.activity_notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 1, new Intent(ACTION_CLOSE_ALL), PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setTextViewText(R.id.tvNetType, "网络类型:" + netType.name());
        contentView.setTextViewText(R.id.tvNextRebootTime, "异常重启时间:3m后重置" + max4GResetCount + "次后无效");
        contentView.setTextViewText(R.id.tvNetErrCount, "网络总异常:" + errCount + "次");
        //contentView.setTextViewText(R.id.tvExeuTime, "已执行时间:" + nowRecodeTime + "m");
        contentView.setOnClickPendingIntent(R.id.btnClose, pendingIntent);
        builder.setContent(contentView);
        builder.setSmallIcon(R.mipmap.network);  //小图标，在大图标右下角
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.network)); //大图标，没有设置时小图标就是大图标
        builder.setOngoing(true);//滑动不能清除
        builder.setAutoCancel(false);   //点击的时候消失
        manager.notify(13, builder.build());  //参数一为ID，用来区分不同APP的Notification

        initSomeParam();//初始化基本参数

        startTimerTask1();//开启网络检测

        closeTask();//执行关闭线程
    }


    /**
     * 初始化一些基本参数
     */
    private void initSomeParam() {
        urlAddr = SPUtils.getInstance().getString(KeyValueConst.ADDR, NetworkUtil.url);
        cycleInterval = SPUtils.getInstance().getInt(KeyValueConst.CYCLE_INTERVAL, 1);
        netUserSetErrCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_SCAN_COUNT, 10);
    }


    /**
     * 重置4G模块
     */
    private void reset4G() {
        //软链接
        File file1=new File("/dev/lte_state");
        //早期模块中的链接
        File file2=new File("/sys/devices/platform/imx6q_sim/sim_sysfs/state");
        if(file1.exists()&&file1.isFile()){
            ShellUtils.CommandResult resetCommand1 = ShellUtils.execCommand(commandToReset1, false);
            KLog.e(TAG, resetCommand1.result == 0 ? "resetCommand1 复位成功" : "resetCommand1 复位失败 errMeg=" + resetCommand1.errorMsg);
        }else if(file2.exists()&&file2.isFile()){
            ShellUtils.CommandResult resetCommand2 = ShellUtils.execCommand(commandToReset2, false);
            KLog.e(TAG, resetCommand2.result == 0 ? "resetCommand2 复位成功" : "resetCommand2 复位失败 errMeg=" + resetCommand2.errorMsg);
        }else{
            KLog.e(TAG, "未成功执行4G模块复位 当前未找到路径 /dev/lte_state | /sys/devices/platform/imx6q_sim/sim_sysfs/state");
        }
        //4G模块复位后就+1
        now4GResetCount++;
        KLog.e(TAG, "当前4G模块已重置的次数为→now4GResetCount=" + now4GResetCount);
    }

    /**
     * 重启设备
     */
    public void reboot() {
        ShellUtils.CommandResult rebootResult = ShellUtils.execCommand(commandReboot, true);
        KLog.e(TAG, rebootResult.result == 0 ? "重启成功" : "重启失败 errMeg=" + rebootResult.errorMsg);
    }

    /**
     * 网络正常时按设定的时间周期检查网络是否会出现异常
     */
    public void startTimerTask1() {
        isRunningTask1 = true;
        sDisposable1 = Observable
                .interval(delayStartTime, cycleInterval, TimeUnit.MINUTES)
                //取消任务时取消定时唤醒
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        isRunningTask1 = false;
                        KLog.d(TAG, "startTimerTask1 closed");
                        startTimerTask2();
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
                        NetUtils.NetType netType = NetUtils.getAPNType(mContext);
                        if (isMobileConn || ((netType != NetUtils.NetType.WIFI) && (netType != NetUtils.NetType.ETH))) {//如果当前网络为4G
                            //获取当前网络状态
                            NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(mContext, urlAddr);
                            if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {//如果网络正常 能够正常访问网络
                                KLog.e(TAG, "1能够正常访问网络 是否为4G:" + isMobileConn);
                                netConnSuccess();
                            } else {
                                KLog.e(TAG, "1当前网络状态：" + netStatus.name() + ",当前网络异常 执行下一步");
                                closeTimerTask1();
                            }
                        } else {
                            KLog.e(TAG, "1当前网络非4G网络 请检查天线和SIM卡是否正常接入");
                            open4GNetWork();
                        }
                    }
                });
    }

    /**
     * 开启4G网络
     */
    public void open4GNetWork() {
        ShellUtils.execCommand(new String[]{"svc data enable", "start ril-daemon"}, true);
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
                        KLog.d(TAG, "startTimerTask2 closed");
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
                        NetUtils.NetType netType = NetUtils.getAPNType(mContext);
                        if (isMobileConn || ((netType != NetUtils.NetType.WIFI) && (netType != NetUtils.NetType.ETH))) {
                            NetworkUtil.NET_TYPE netStatus = NetworkUtil.getNetState(mContext, urlAddr);
                            KLog.e(TAG, "2当前网络状态：" + netStatus.name());
                            if (netStatus == NetworkUtil.NET_TYPE.NET_CNNT_OK) {
                                KLog.e(TAG, "2 网络正常了，这里需要重置一些数据！");
                                netConnSuccess();
                                closeTimerTask2();
                            } else {
                                open4GNetWork();
                                netConnFailed();
                                KLog.e(TAG, "2网络异常 记录次数，网络异常时检测总次数为：" + netUserSetErrCount + ",用于做4G模块复位前的判断,当前网络异常次数为：" + nowNetErrCount);
                                if (nowNetErrCount == netUserSetErrCount) {
                                    KLog.e(TAG, "exeu reboot！");
                                    //4G模块复位
                                    reboot();
                                }
                            }
                        } else {
                            KLog.e(TAG, "2 当前网络非4G 请检查天线和SIM卡是否正常接入 任务需要终止>>>");
                            //当前网络非4G情况下
                            //执行终止命令
                            //继续执行第一个线程 检查网络是否会存在异常
                            closeTimerTask2();
                            open4GNetWork();
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
        //清除记录的时间
        nowNetErrCount = 0;
        now4GResetCount = 0;
        SPUtils.getInstance().put(KeyValueConst.LAST_STATUS, true);
        RxBus.getDefault().post(new TotalEntity());
        //恢复默认的显示
        NetUtils.NetType netType = NetUtils.getAPNType(mContext);
        contentView.setTextViewText(R.id.tvNetType, "网络类型:" + netType.name());
        contentView.setTextViewText(R.id.tvNextRebootTime, "异常重启时间:3m后重置4G" + max4GResetCount + "次后无效");
        contentView.setTextViewText(R.id.tvNetErrCount, "网络总异常:0次");
        contentView.setTextViewText(R.id.tvExeuTime, "已执行时间:0m");
        manager.notify(13, builder.build());
    }

    /**
     * 4G网络异常时的操作
     */
    public void netConnFailed() {
        nowNetErrCount++;
        //int totalCount = SPUtils.getInstance().getInt(KeyValueConst.TOTAL_COUNT, 0);
        int errCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_COUNT, 0);
        boolean isCountFrist = SPUtils.getInstance().getBoolean(KeyValueConst.IS_NET_ERR_FIRST_COUNT, true);
        if (isCountFrist) {
            //第一次记录错误次数
            SPUtils.getInstance().put(KeyValueConst.IS_NET_ERR_FIRST_COUNT, false);
            SPUtils.getInstance().put(KeyValueConst.ERR_COUNT, errCount + 1);
            //KLog.e(TAG, "网络检测的总次数为：" + totalCount + ",异常掉网的次数为：" + (errCount + 1));
        } else {
            if (SPUtils.getInstance().getBoolean(KeyValueConst.LAST_STATUS, false)) {
                SPUtils.getInstance().put(KeyValueConst.ERR_COUNT, errCount + 1);
                //KLog.e(TAG, "网络检测的总次数为：" + totalCount + ",异常掉网的次数为：" + (errCount + 1));
                SPUtils.getInstance().put(KeyValueConst.LAST_STATUS, false);
            } else {
                //KLog.e(TAG, "由于一直处在掉网情况，不做统计次数");
                //KLog.e(TAG, "网络检测的总次数为：" + totalCount + ",异常掉网的次数为：" + errCount);
            }
        }
        NetUtils.NetType netType = NetUtils.getAPNType(mContext);
        contentView.setTextViewText(R.id.tvNetType, "网络类型:" + netType.name());
        contentView.setTextViewText(R.id.tvNetErrCount, "网络总异常:" + (errCount + 1) + "次");
        manager.notify(13, builder.build());
        RxBus.getDefault().post(new TotalEntity());
        //执行重置4G模块的操作
        reset4G();
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
     * 关闭相关Servie内容
     */
    public void stopService() {
        stopSelf();
        closeAllTimerTask();
        //关闭通知
        if (manager != null) {
            manager.cancel(13);
        }
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
        netConnSuccess();
        if (manager != null) {
            manager.cancel(13);
        }
        closeAllTimerTask();
    }
}
