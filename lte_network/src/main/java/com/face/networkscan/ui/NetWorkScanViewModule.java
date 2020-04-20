package com.face.networkscan.ui;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.face.base.db.DbHelper;
import com.face.networkscan.R;
import com.face.networkscan.entity.ChangeDataEntity;
import com.face.networkscan.entity.ThreadNotice;
import com.face.networkscan.entity.TotalEntity;
import com.face.networkscan.utils.DateUtil;
import com.face.networkscan.utils.DevicesUtils;
import com.face.networkscan.utils.KeyValueConst;
import com.face.networkscan.utils.SystemInfoUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.base.BaseViewModel;
import me.goldze.mvvmhabit.binding.command.BindingAction;
import me.goldze.mvvmhabit.binding.command.BindingCommand;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;
import me.goldze.mvvmhabit.utils.ToastUtils;

public class NetWorkScanViewModule extends BaseViewModel {
    private static final String TAG = NetWorkScanViewModule.class.getSimpleName();
    public ObservableField<AppCompatActivity> mContext = new ObservableField<>();//上下文
    public ObservableInt cycleIntervalNum = new ObservableInt(1);//网络检查间隔的时间 分钟
    public ObservableInt errScanCountNum = new ObservableInt(2);//异常状态扫描的次数
    public ObservableInt cycleIntervalPosition = new ObservableInt(0);//网络检查间隔时间 对应的下标
    public ObservableInt errScanCountPosition = new ObservableInt(0);//网络异常判断次数   对应的下标
    public ObservableField<String> openCloseTv = new ObservableField<>();//当前btn状态为开启后台服务|关闭后台服务
    public ObservableField<String> nowSelectAddrTv = new ObservableField<>();//当前选择的访问地址
    public List<String> connadrList = new ArrayList<>();//数据库查询到的全部访问地址
    public ObservableField<String> modelTypeName = new ObservableField<>();//当前查询出来的机型

    //public ObservableField<String> totalCountTv = new ObservableField<>("检测总次数：加载中...");
    public ObservableField<String> errCountTv = new ObservableField<>("异常次数：加载中...");
    public ObservableField<String> startTimeTv = new ObservableField<>("开始时间：加载中...");
    public ObservableField<String> exeuTimeTv = new ObservableField<>("已执行时间：加载中...");
    DbHelper dbHelper = null;//数据库服务类
    PopupWindow popupWindow;//弹窗  用于新增机型|访问地址

    public NetWorkScanViewModule(@NonNull Application application) {
        super(application);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        openCloseTv.set(mContext.get().getResources().getString(R.string.main_off_service));
        initDaBase();//初始化数据库
        initDataInfo();//初始化一些基础数据
        queryLinkAddress();//查询当前数据库的访问地址
        setReuslt();
        // 根据isOpenService判断是否需要开启Service服务
        // 首次进来 默认情况下 需要开启
        // 第二次启动时按照 isOpenService 对应的value值进行判断
        /*boolean isOpenService = SPUtils.getInstance().getBoolean("isOpenService", true);
        if (isOpenService) {
            //1关闭服务
            mContext.get().stopService(new Intent(mContext.get(), NetWorkListenerService.class));
            //2重新启动Service
            openService();
        } else {
            //关闭服务
            closeService();
        }*/
        if (!isWorked("com.face.lte_networkscan.ui.NetWorkListenerService")) {
            //未启动
            openService();
            Log.e(TAG, "服务未启动！！,正在启动中....");
        } else {
            //已启动
            Log.e(TAG, "服务已经启动了！！");
        }
    }
    private boolean isWorked(String className) {
        ActivityManager myManager = (ActivityManager) mContext.get()
                .getApplicationContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(className)) {
                return true;
            }
        }
        return false;
    }
    /**
     * 初始化数据库
     */
    public void initDaBase() {
        dbHelper = new DbHelper(mContext.get(), DbHelper.DB_NAME_CONN_READER_TYPE, null, 2);
        dbHelper.inItDb();
    }


    /**
     * 初始化一些基础数据
     */
    public void initDataInfo() {
        String timeStr=SPUtils.getInstance().getString("startTime","");
        if(timeStr.equals("")){
            Date date=new Date();
            SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            timeStr=simpleDateFormat.format(date);
            SPUtils.getInstance().put("startTime",timeStr);
        }
        int totalCount = SPUtils.getInstance().getInt(KeyValueConst.TOTAL_COUNT, 0);
        int errCount = SPUtils.getInstance().getInt(KeyValueConst.ERR_COUNT, 0);
        startTimeTv.set("开始时间："+timeStr);
        //totalCountTv.set("检测总次数："+totalCount+"次");
        errCountTv.set("异常次数："+errCount+"次");

        //获取机型
        String getModelTypeName = SystemInfoUtil.getModelType();
        String androidSystemVersion = android.os.Build.VERSION.RELEASE;
        KLog.e(TAG, "您当前获取到的机型为：" + getModelTypeName + ",当前的安卓系统版本为：" + androidSystemVersion);
        if (getModelTypeName != null) {
            if (getModelTypeName.equals(DevicesUtils.FEI_SI_KA_ER)) {
                modelTypeName.set("飞思卡尔");
            } else if (getModelTypeName.equals(DevicesUtils.RK_3399)) {
                modelTypeName.set("rk3399");
            } else if (getModelTypeName.equals(DevicesUtils.RK_3288)) {
                modelTypeName.set("rk3288");
            } else {
                modelTypeName.set(getModelTypeName);
            }
        }
        KLog.d(TAG, "添加一些默认访问地址。");
        //添加一些访问地址
        //该地址第一次加载会将DevicesUtils.getAddrList()数据全部添加
        //第一次之后则会查询是否存在重复的地址 再执行添加操作
        ContentValues values = null;
        List<String> addrList = DevicesUtils.getAddrList();
        for (int i = 0; i < addrList.size(); i++) {
            if (!dbHelper.checkExistByAddr(addrList.get(i))) {
                values = new ContentValues();
                values.put(KeyValueConst.ADDR, addrList.get(i));
                dbHelper.insertConn(values, DbHelper.TABLE_CONN_ADDR);
            }
        }

        //是否为首次启动
        boolean isFristStart = SPUtils.getInstance().getBoolean("isFirst", true);
        if (isFristStart) {
            //如果首次进入 并且初始化机型后
            //需要去加载符合设备对应机型的选项 设置为默认
            //首次加载 设置默认的访问访问地址
            SPUtils.getInstance().put(KeyValueConst.ADDR, "www.baidu.com");
            KLog.e(TAG, "首次加载，默认设置一个访问地址：223.5.5.5,手动选择后失效");
            SPUtils.getInstance().put("isFirst", false);
        }
    }

    /**
     * 查询数据库的全部访问地址
     * 获取上一次选择的访问地址下标
     */
    public void queryLinkAddress() {
        int defaultPositon = 0;
        KLog.d(TAG, "刷新Spinner Network check interval");
        String postionAddrtv = SPUtils.getInstance().getString(KeyValueConst.ADDR, "");
        connadrList = dbHelper.getAddrList();
        if (connadrList != null && connadrList.size() > 0) {
            for (int i = 0; i < connadrList.size(); i++) {
                //找到addr默认设置的下标
                if (postionAddrtv.equals(connadrList.get(i))) {
                    defaultPositon = i;
                    break;
                }
            }
        }
        KLog.d(TAG, "数据库一共存在的访问地址数量:" + connadrList.size());
        RxBus.getDefault().post(new ChangeDataEntity(-1, defaultPositon, connadrList, ChangeDataEntity.DATA_TYPE.TYPE_CONN_ADDR));
    }

    //添加访问地址
    public BindingCommand addAddrClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            //设置contentView
            View contentView = LayoutInflater.from(mContext.get()).inflate(R.layout.dialog_addr, null);
            popupWindow = new PopupWindow(contentView,
                    ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setContentView(contentView);
            final EditText et_addr = contentView.findViewById(R.id.et_addr);
            TextView tv_save = contentView.findViewById(R.id.tv_save);
            tv_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String addrStr = et_addr.getText().toString();
                    if (addrStr == null || addrStr.equals("")) {
                        ToastUtils.showLong(mContext.get().getResources().getString(R.string.main_input_ok_addr));
                        popupWindow.dismiss();
                    } else {
                        if (!dbHelper.checkExistByAddr(addrStr)) {
                            //添加数据
                            ContentValues values = new ContentValues();
                            values.put(KeyValueConst.ADDR, addrStr + "");
                            dbHelper.insertConn(values, DbHelper.TABLE_CONN_ADDR);
                            KLog.e(TAG, "添加的地址为：" + addrStr);
                            //添加完成后更新数据
                            queryLinkAddress();
                            popupWindow.dismiss();
                        } else {
                            ToastUtils.showLong(mContext.get().getResources().getString(R.string.main_exist_link_rename));
                            KLog.e(TAG, "数据库存在相同的访问地址,请更换！");
                        }
                    }
                }
            });
            //防止PopupWindow被软件盘挡住（可能只要下面一句，可能需要这两句）
            //mPopWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
            popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            //设置软键盘弹出
            InputMethodManager inputMethodManager = (InputMethodManager) mContext.get().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(1000, InputMethodManager.HIDE_NOT_ALWAYS);//这里给它设置了弹出的时间
            //设置外部点击关闭效果需要设置setBackgroundDrawable
            popupWindow.setBackgroundDrawable(new BitmapDrawable());
            //是否具有获取焦点的能力
            popupWindow.setFocusable(true);
            //是否允许点击外部
            popupWindow.setOutsideTouchable(true);
            //显示PopupWindow
            View rootview = LayoutInflater.from(mContext.get()).inflate(R.layout.activity_networkscan, null);
            popupWindow.showAtLocation(rootview, Gravity.BOTTOM, 0, 0);
        }
    });


    //删除下标地址
    public BindingCommand delAddrClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            if (nowSelectAddrTv.get().equals("223.5.5.5") || nowSelectAddrTv.get().equals("www.google.cn")||nowSelectAddrTv.get().equals("8.8.8.8")) {
                ToastUtils.showLong(mContext.get().getResources().getString(R.string.main_system_link));
                return;
            }
            if (connadrList != null && connadrList.size() > 0) {
                connadrList.remove(nowSelectAddrTv.get());
                dbHelper.deleteConn(nowSelectAddrTv.get(), DbHelper.TABLE_CONN_ADDR);
                KLog.e(TAG, "执行了删除的操作：" + nowSelectAddrTv.get() + ",剩余地址的数量为：" + connadrList.size());
                String postionAddrtv = SPUtils.getInstance().getString(KeyValueConst.ADDR, "");
                //如果删除的地址等于数据库中保存的地址 需要删除
                if (!postionAddrtv.equals("") && postionAddrtv.equals(nowSelectAddrTv.get())) {
                    SPUtils.getInstance().put(KeyValueConst.ADDR, "");
                    KLog.e(TAG, "保存的连接地址：" + nowSelectAddrTv.get() + ",已被删除！");
                }
                if (connadrList == null || connadrList.size() <= 0) {
                    nowSelectAddrTv.set("");//将当前选中的地址置为空
                    SPUtils.getInstance().put(KeyValueConst.ADDR, "");
                    KLog.e(TAG, "地址已被全部清空！");
                }
                queryLinkAddress();
            } else {
                nowSelectAddrTv.set("");//将当前选中的地址置为空
                SPUtils.getInstance().put(KeyValueConst.ADDR, "");
                KLog.e(TAG, "地址已被全部清空！");
            }
        }
    });

    //开启网络检测服务
    //打开完成后 需要标识当前的状态为已打开状态 也就是待关闭后台服务的状态
    //isOpenService put 为 false的时候 标识界面的btn显示为 开启后台服务 此时Service已关闭
    //isOpenService put 为 true的时候 标识界面的btn显示为 关闭后台服务  此时Service已打开
    public void openService() {
        //下次启动时候需要自动重启
        SPUtils.getInstance().put("isOpenService", true);
        //KLog.e(TAG, "开启了服务>当前的状态为：" + SPUtils.getInstance().getBoolean("isOpenService", true));
        openCloseTv.set(mContext.get().getResources().getString(R.string.main_off_service));
        //开启服务
        mContext.get().startService(new Intent(mContext.get(), NetWorkListenerService.class));
    }

    //关闭网络检测服务
    //关闭完成后 需要标识当前的状态为已关闭状态 也就是待开启后台服务的状态
    //isOpenService put 为 false的时候 标识界面的btn显示为 开启后台服务 此时Service已关闭
    //isOpenService put 为 true的时候 标识界面的btn显示为 关闭后台服务  此时Service已打开
    public void closeService() {
        RxBus.getDefault().post(new ThreadNotice());
        //下次启动时候只能手动启动
        SPUtils.getInstance().put("isOpenService", false);
        //KLog.e(TAG, "关闭了服务>当前的状态为：" + SPUtils.getInstance().getBoolean("isOpenService", true));
        openCloseTv.set(mContext.get().getResources().getString(R.string.main_on_service));
        //关闭服务
        mContext.get().stopService(new Intent(mContext.get(), NetWorkListenerService.class));
        ToastUtils.showShort("已关闭网络检测服务");
    }

    //开启关闭网络检测服务
    public BindingCommand closeServiceClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            if (!isWorked("com.face.networkscan.ui.NetWorkListenerService")) {
                //未启动
                openService();
            } else {
                //已启动
                closeService();
            }
        }
    });
    //设置参数
    public BindingCommand setParamClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            closeService();
            SPUtils.getInstance().put(KeyValueConst.CYCLE_INTERVAL, cycleIntervalNum.get());
            SPUtils.getInstance().put("cycleIntervalPosition", cycleIntervalPosition.get());
            SPUtils.getInstance().put(KeyValueConst.ERR_SCAN_COUNT, errScanCountNum.get());
            SPUtils.getInstance().put("errScanCountPosition", errScanCountPosition.get());
            SPUtils.getInstance().put(KeyValueConst.ADDR, nowSelectAddrTv.get());
            KLog.d(TAG, "\n\r" +
                    "cycleInterval--:" + cycleIntervalNum.get() + "\n\r" +
                    "cycleIntervalPosition--:" + cycleIntervalPosition.get() + "\n\r" +
                    "errScanCount--:" + errScanCountNum.get() + "\n\r" +
                    "errScanCountPosition--:" + errScanCountPosition.get() + "\n\r" +
                    "addr--:" + nowSelectAddrTv.get());
            //保存参数后重新启动Service
            /*RxBus.getDefault().post(new NetTimerParamEntity());*/
            openService();
            ToastUtils.showShort("已保存并启用");
        }
    });

    public BindingCommand clearCacheClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            ToastUtils.showShort("清除成功！");
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    try{
                        /*RxBus.getDefault().post(new ThreadNotice());
                        mContext.get().stopService(new Intent(mContext.get(), NetWorkListenerService.class));*/
                        closeService();
                        SPUtils.getInstance().put(KeyValueConst.TOTAL_COUNT, 0);
                        SPUtils.getInstance().put(KeyValueConst.ERR_COUNT, 0);
                        SPUtils.getInstance().put(KeyValueConst.IS_NET_ERR_FIRST_COUNT, true);
                        SPUtils.getInstance().put(KeyValueConst.LAST_STATUS, false);
                        Date date=new Date();
                        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String timeStr=simpleDateFormat.format(date);
                        SPUtils.getInstance().put("startTime",timeStr );
                        startTimeTv.set("开始时间："+timeStr);
                        //totalCountTv.set("检测总次数：0次");
                        exeuTimeTv.set("已执行时间：加载中...");
                        errCountTv.set("异常次数：0次");
                        Thread.sleep(1500);
                        mContext.get().startService(new Intent(mContext.get(), NetWorkListenerService.class));
                    }catch(Exception e){
                        e.printStackTrace();
                        Log.e(TAG,"errMeg:"+e.getMessage());
                    }
                }
            }.start();

        }
    });


    public void setReuslt() {
        RxBus.getDefault().toObservable(TotalEntity.class).subscribe(new Consumer<TotalEntity>() {
            @Override
            public void accept(TotalEntity totalEntity) throws Exception {
                KLog.d(TAG, "返回了数据");
                //totalCountTv.set("检测总次数：" + SPUtils.getInstance().getInt(KeyValueConst.TOTAL_COUNT, 0) + "次");
                errCountTv.set("异常次数：" + SPUtils.getInstance().getInt(KeyValueConst.ERR_COUNT, 0) + "次");
                getExeuTime();
            }
        });

    }

    /**
     * 获取已执行时间
     */
    public void getExeuTime(){
        try{
            Calendar saveCalendar, nowCalendar = null;
            String getSaveDateTime = SPUtils.getInstance().getString("startTime", "");
            long nowTime = -1, saveTime = -1, cTime = -1, sTime = -1, mTime = -1, hTime = -1, dTime = -1;
            if (!getSaveDateTime.equals("")) {
                //只有当前KeyGlobalValue.KEY_PAST_TIME的值不为“未重启”时
                //才能执行上次重启的时间与当前时间的比较差
                //（获得保存的时间|当前的时间）的具体年月日 时分秒
                saveCalendar = DateUtil.getCalendarByTime(getSaveDateTime);
                nowCalendar = DateUtil.getCurrentCalendar();
                //先判断天数的相差是否一致 再比较时间差是否大于一个小时
                nowTime = nowCalendar.getTimeInMillis();
                saveTime = saveCalendar.getTimeInMillis();
                cTime = nowTime - saveTime;
                sTime = cTime / 1000;//时间差，单位：秒
                mTime = sTime / 60;
                hTime = mTime / 60;
                dTime = hTime / 24;
            }
            exeuTimeTv.set("已执行时间:" + dTime + "天" + hTime % 24 + "时" + mTime % 60 + "分" + sTime % 60 + "秒");
        }catch(Exception e){
            e.printStackTrace();
            Log.e(TAG,"errMeg:"+e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
