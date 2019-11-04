package com.goldze.main.ui.by_old;

import android.app.ActionBar;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.goldze.base.db.DbHelper;
import com.goldze.main.R;
import com.goldze.main.entity.ChangeDataEntity;
import com.goldze.base.entity.ModelTypeEntity;
import com.goldze.main.entity.NameVersionEntity;
import com.goldze.main.entity.NetTimerParamEntity;
import com.goldze.main.entity.ThreadNotice;
import com.goldze.main.utils.DevicesUtils;
import com.goldze.main.utils.KeyValueConst;
import com.goldze.main.utils.SystemInfoUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.goldze.mvvmhabit.base.BaseViewModel;
import me.goldze.mvvmhabit.binding.command.BindingAction;
import me.goldze.mvvmhabit.binding.command.BindingCommand;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;
import me.goldze.mvvmhabit.utils.ToastUtils;

public class MainAtyViewModule extends BaseViewModel {
    private static final String TAG = MainAtyViewModule.class.getSimpleName();
    public ObservableField<AppCompatActivity> mContext = new ObservableField<>();
    public ObservableInt cycleIntervalNum = new ObservableInt(3);//网络检查间隔的时间 分钟
    public ObservableInt errScanCountNum = new ObservableInt(2);//异常状态扫描的次数
    public ObservableInt cycleIntervalPosition = new ObservableInt(0);//网络检查间隔时间 对应的下标
    public ObservableInt errScanCountPosition = new ObservableInt(0);//网络异常判断次数   对应的下标
    public ObservableField<String> openCloseTv = new ObservableField<>();//当前btn状态为开启后台服务|关闭后台服务
    public ObservableField<String> nowSelectAddrTv = new ObservableField<>();//当前选择的访问地址
    public List<String> connadrList = new ArrayList<>();//数据库查询到的全部访问地址
    public List<ModelTypeEntity> modelTypeList = new ArrayList<>();//机型
    public ObservableField<String> typeNameTv = new ObservableField<>();//当前选中的机型名称
    public ObservableField<String> typeCommandTv = new ObservableField<>();//当前机型对应的shell命令
    public ObservableField<String> modelTypeNameByFirst = new ObservableField<>();//当前查询出来的机型
    DbHelper dbHelper = null;//数据库服务类
    PopupWindow popupWindow;//弹窗  用于新增机型|访问地址

    public MainAtyViewModule(@NonNull Application application) {
        super(application);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        openCloseTv.set(mContext.get().getResources().getString(R.string.main_off_service));
        initDaBase();
        initModelType();
        queryLinkAddress();
        queryAllModelType();
        // 根据isOpenService判断是否需要开启Service服务
        // 首次进来 默认情况下 需要开启
        // 第二次启动时按照 isOpenService 对应的value值进行判断
        boolean isOpenService = SPUtils.getInstance().getBoolean("isOpenService", true);
        if (isOpenService) {
            //1关闭服务
            RxBus.getDefault().post(new ThreadNotice());
            //2重新启动Service
            openService();
        } else {
            //关闭服务
            closeService();
        }
    }

    /**
     * 初始化数据库
     */
    public void initDaBase() {
        dbHelper = new DbHelper(mContext.get(), DbHelper.DB_NAME_CONN_READER_TYPE, null, 1);
        dbHelper.inItDb();
    }

    /**
     * 默认添加一些机型
     * 4.4.2  /sys/devices/platform/imx6q_sim/sim_sysfs/state        飞思卡尔1
     * 5.1.1  /sys/bus/platform/devices/sim-gpios.40/sim_sysfs/state 飞思卡尔2
     * 7.1.2 /sys/class/spi_sim_ctl/state rk3399
     */
    public void initModelType() {
        //获取机型
        String getModelTypeName = SystemInfoUtil.getModelType();
        String androidSystemVersion = android.os.Build.VERSION.RELEASE;
        KLog.e(TAG, "您当前获取到的机型为：" + getModelTypeName + ",当前的安卓系统版本为：" + androidSystemVersion);
        if (getModelTypeName != null) {
            if (getModelTypeName.equals(DevicesUtils.FEI_SI_KA_ER)) {
                if (androidSystemVersion.equals("4.2.2")) {
                    modelTypeNameByFirst.set("飞思卡尔1");
                } else if (androidSystemVersion.equals("5.1.1")) {
                    modelTypeNameByFirst.set("飞思卡尔2");
                }
            } else if (getModelTypeName.equals(DevicesUtils.RK_3399)) {
                modelTypeNameByFirst.set("rk3399");
            }
        }
        KLog.e(TAG, "添加一些默认机型:rk3399;飞思卡尔；以及添加一些默认访问地址。");
        LinkedHashMap<String, NameVersionEntity> modelTypeList = DevicesUtils.getModelTypeList();
        ContentValues values = null;
        for (Map.Entry<String, NameVersionEntity> entry : modelTypeList.entrySet()) {
            if (!dbHelper.checkExistTypeCommand(entry.getKey())) {//未找到相关命令
                values = new ContentValues();
                values.put("typeName", entry.getValue().getName());
                values.put("androidVersion", entry.getValue().getAndroidVersion());
                values.put("typeCommand", entry.getKey());
                dbHelper.insertConn(values, DbHelper.TABLE_READER_TYPE);
            }
        }
        //添加一些访问地址
        //该地址第一次加载会将DevicesUtils.getAddrList()数据全部添加
        //第一次之后则会查询是否存在重复的地址 再执行添加操作
        List<String> addrList = DevicesUtils.getAddrList();
        for (int i = 0; i < addrList.size(); i++) {
            if (!dbHelper.checkExistByAddr(addrList.get(i))) {
                values = new ContentValues();
                values.put("addr", addrList.get(i));
                dbHelper.insertConn(values, DbHelper.TABLE_CONN_ADDR);
            }
        }

        //是否为首次启动
        boolean isFristStart = SPUtils.getInstance().getBoolean("isFirst", true);
        if (isFristStart) {
            //如果首次进入 并且初始化机型后
            //需要去加载符合设备对应机型的选项 设置为默认
            if (modelTypeNameByFirst.get() != null && !modelTypeNameByFirst.get().equals("机型不详") && !modelTypeNameByFirst.get().equals("")) {
                SPUtils.getInstance().put("typeName", modelTypeNameByFirst.get());
                //查询当前的机型名称在集合中是否存在对应的机型
                for (Map.Entry<String, NameVersionEntity> entry : modelTypeList.entrySet()) {
                    //判断机型名称是否存在
                    if (entry.getValue().getName().equals(modelTypeNameByFirst.get())) {
                        //存在该机型名称 则 按照机型名称 去找 不同android系统版本下的节点
                        //rk3399不做重复查询，目前只有一个系统版本的
                        //而飞思卡尔则在5.1.1 ；4.2.2 android系统版本中的节点是不一样的 所以要做区分
                        String getCommand = "";
                        if (entry.getValue().getName().equals("飞思卡尔1") || entry.getValue().getName().equals("飞思卡尔2")) {
                            //当前的android系统版本与名称对应设备后才执行
                            if (androidSystemVersion.equals(entry.getValue().getAndroidVersion())) {
                                getCommand = entry.getKey();//根据机型获取命令
                            }
                        } else if (modelTypeNameByFirst.get().equals("rk3399")) {
                            //这里直接获取对应节点的命令
                            //因为该机型只有一种对应的节点
                            getCommand = entry.getKey();
                        } else {
                            KLog.e(TAG, "未找到机型，安卓版本对应的数据");
                        }
                        //这里执行系统默认加载机型 下 对应的android系统的节点命令
                        if (getCommand != null && !getCommand.equals("")) {
                            //根据机型获取到对应的adb命令后，去保存typeCommand
                            SPUtils.getInstance().put("typeCommand", getCommand);
                            KLog.e(TAG, "首次加载，根据机型获取到adb命令成功-->机型：" + modelTypeNameByFirst.get() + ",命令：" + getCommand + ",手动选择后失效");
                        } else {
                            //如果根据机型获取不到命令 则重置typeName机型
                            SPUtils.getInstance().put("typeName", "");
                            KLog.e(TAG, "首次加载，根据机型获取adb命令失败，重置机型和命令为空！");
                        }
                        break;
                    }
                }
            }
            //首次加载 设置默认的访问访问地址
            SPUtils.getInstance().put("addr","223.5.5.5");
            KLog.e(TAG,"首次加载，默认设置一个访问地址：223.5.5.5,手动选择后失效");

            SPUtils.getInstance().put("isFirst", false);
        }
    }

    /**
     * 查询数据库的全部访问地址
     * 获取上一次选择的访问地址下标
     */
    public void queryLinkAddress() {
        int defaultPositon = 0;
        KLog.e(TAG, "刷新Spinner Network check interval");
        String postionAddrtv = SPUtils.getInstance().getString("addr", "");
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
        KLog.e(TAG, "数据库一共存在的访问地址数量:" + connadrList.size());
        RxBus.getDefault().post(new ChangeDataEntity(-1, defaultPositon, connadrList, ChangeDataEntity.DATA_TYPE.TYPE_CONN_ADDR));
    }

    /**
     * 查询全部的机型
     */
    public void queryAllModelType() {
        KLog.e(TAG, "刷新Spinner modelType");
        int defaultPositon = 0;//默认加载的机型的下标
        String postionTypeName = SPUtils.getInstance().getString("typeName", "");
        modelTypeList = dbHelper.getModelTypeList();

        List<String> modelTypeNameList = new ArrayList<>();
        if (modelTypeList != null && modelTypeList.size() > 0) {
            for (int j = 0; j < modelTypeList.size(); j++) {//找到addr默认设置的下标
                if (postionTypeName.equals(modelTypeList.get(j).getTypeName())) {
                    defaultPositon = j;
                }
                modelTypeNameList.add(modelTypeList.get(j).getTypeName() + "   |    " + modelTypeList.get(j).getTypeCommand());
            }
        }
        KLog.e(TAG, "数据库一共存在的机型数量:" + modelTypeList.size());
        RxBus.getDefault().post(new ChangeDataEntity(-1, defaultPositon, modelTypeNameList, ChangeDataEntity.DATA_TYPE.TYPE_MODEL_TYPE));
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
                            values.put("addr", addrStr + "");
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
            View rootview = LayoutInflater.from(mContext.get()).inflate(R.layout.activity_main, null);
            popupWindow.showAtLocation(rootview, Gravity.BOTTOM, 0, 0);
        }
    });


    //添加机型
    public BindingCommand addTypeModelClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            //设置contentView
            View contentView = LayoutInflater.from(mContext.get()).inflate(R.layout.dialog_type_model, null);
            popupWindow = new PopupWindow(contentView,
                    ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setContentView(contentView);
            final EditText ettypeName = contentView.findViewById(R.id.et_typename);
            final EditText etcommand = contentView.findViewById(R.id.et_command);
            TextView tv_save = contentView.findViewById(R.id.tv_save);
            tv_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String typeName = ettypeName.getText().toString();
                    String command = etcommand.getText().toString();
                    if (typeName == null || typeName.equals("") || command == null || command.equals("")) {
                        ToastUtils.showLong(mContext.get().getResources().getString(R.string.main_input_ok_model));
                        popupWindow.dismiss();
                    } else {
                        if (!dbHelper.checkExistByTypeName(typeName)) {
                            ContentValues values = new ContentValues();
                            values.put("typeName", typeName);
                            values.put("typeCommand", command);
                            dbHelper.insertConn(values, DbHelper.TABLE_READER_TYPE);
                            queryAllModelType();
                            popupWindow.dismiss();
                        } else {
                            ToastUtils.showLong(mContext.get().getResources().getString(R.string.main_exist_model_type_rename));
                            KLog.e(TAG, "数据库存在相同的机型,请更换名称！");
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
            View rootview = LayoutInflater.from(mContext.get()).inflate(R.layout.activity_main, null);
            popupWindow.showAtLocation(rootview, Gravity.BOTTOM, 0, 0);
        }
    });


    //删除下标地址
    public BindingCommand delAddrClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            if (nowSelectAddrTv.get().equals("223.5.5.5") || nowSelectAddrTv.get().equals("www.google.cn")) {
                ToastUtils.showLong(mContext.get().getResources().getString(R.string.main_system_link));
                return;
            }
            if (connadrList != null && connadrList.size() > 0) {
                connadrList.remove(nowSelectAddrTv.get());
                dbHelper.deleteConn(nowSelectAddrTv.get(), DbHelper.TABLE_CONN_ADDR);
                KLog.e(TAG, "执行了删除的操作：" + nowSelectAddrTv.get() + ",剩余地址的数量为：" + connadrList.size());
                String postionAddrtv = SPUtils.getInstance().getString("addr", "");
                //如果删除的地址等于数据库中保存的地址 需要删除
                if (!postionAddrtv.equals("") && postionAddrtv.equals(nowSelectAddrTv.get())) {
                    SPUtils.getInstance().put("addr", "");
                    KLog.e(TAG, "保存的连接地址：" + nowSelectAddrTv.get() + ",已被删除！");
                }
                if (connadrList == null || connadrList.size() <= 0) {
                    nowSelectAddrTv.set("");//将当前选中的地址置为空
                    SPUtils.getInstance().put("addr", "");
                    KLog.e(TAG, "地址已被全部清空！");
                }
                queryLinkAddress();
            } else {
                nowSelectAddrTv.set("");//将当前选中的地址置为空
                SPUtils.getInstance().put("addr", "");
                KLog.e(TAG, "地址已被全部清空！");
            }

        }
    });
    //删除下标地址
    public BindingCommand delTypeModelClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            if (typeNameTv.get().equals("rk3399") || typeNameTv.get().equals("飞思卡尔1") || typeCommandTv.equals("飞思卡尔2")) {
                ToastUtils.showLong(mContext.get().getResources().getString(R.string.main_system_model_type));
                return;
            }
            if (modelTypeList != null && modelTypeList.size() > 0) {
                Iterator<ModelTypeEntity> iter = modelTypeList.iterator();
                while (iter.hasNext()) {
                    ModelTypeEntity typeModel = iter.next();
                    if (typeModel.getTypeName().equals(typeNameTv.get())) {
                        modelTypeList.remove(typeModel);
                        break;
                    }
                }
                dbHelper.deleteModelType(typeNameTv.get(), DbHelper.TABLE_READER_TYPE);
                KLog.e(TAG, "执行了删除的操作：" + typeNameTv.get() + ",剩余地址的数量为：" + modelTypeList.size());
                String postionAddrtv = SPUtils.getInstance().getString("typeName", "");
                //如果删除的地址等于数据库中保存的地址 需要删除
                if (!postionAddrtv.equals("") && postionAddrtv.equals(typeNameTv.get())) {
                    SPUtils.getInstance().put("typeName", "");
                    SPUtils.getInstance().put("typeCommand", "");
                    KLog.e(TAG, "保存的机型：" + typeNameTv.get() + ",已被删除！");
                }
                if (modelTypeList == null || modelTypeList.size() <= 0) {
                    typeNameTv.set("");//将当前选中的机型置为空
                    typeCommandTv.set("");//将当前选中的命令置为空
                    SPUtils.getInstance().put("typeName", "");
                    SPUtils.getInstance().put("typeCommand", "");
                    KLog.e(TAG, "机型已被全部清空！");
                }
                queryAllModelType();
            } else {
                typeNameTv.set("");//将当前选中的机型置为空
                typeCommandTv.set("");//将当前选中的命令置为空
                SPUtils.getInstance().put("typeName", "");
                SPUtils.getInstance().put("typeCommand", "");
                KLog.e(TAG, "机型已被全部清空！");
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
        KLog.e(TAG, "开启了服务>当前的状态为：" + SPUtils.getInstance().getBoolean("isOpenService", true));
        openCloseTv.set(mContext.get().getResources().getString(R.string.main_off_service));
        //开启服务
        mContext.get().startService(new Intent(mContext.get(), NetWorkService.class));
    }

    //关闭网络检测服务
    //关闭完成后 需要标识当前的状态为已关闭状态 也就是待开启后台服务的状态
    //isOpenService put 为 false的时候 标识界面的btn显示为 开启后台服务 此时Service已关闭
    //isOpenService put 为 true的时候 标识界面的btn显示为 关闭后台服务  此时Service已打开
    public void closeService() {
        //下次启动时候只能手动启动
        SPUtils.getInstance().put("isOpenService", false);
        KLog.e(TAG, "关闭了服务>当前的状态为：" + SPUtils.getInstance().getBoolean("isOpenService", true));
        openCloseTv.set(mContext.get().getResources().getString(R.string.main_on_service));
        //关闭服务
        RxBus.getDefault().post(new ThreadNotice());
    }

    //开启关闭网络检测服务
    public BindingCommand closeServiceClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            //默认情况下Service需要自动打开
            boolean isOpenService = SPUtils.getInstance().getBoolean("isOpenService", true);
            if (isOpenService) {
                closeService();
            } else {
                openService();
            }
        }
    });
    //设置参数
    public BindingCommand setParamClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            SPUtils.getInstance().put(KeyValueConst.CYCLE_INTERVAL, cycleIntervalNum.get());
            SPUtils.getInstance().put("cycleIntervalPosition", cycleIntervalPosition.get());
            SPUtils.getInstance().put(KeyValueConst.ERR_SCAN_COUNT, errScanCountNum.get());
            SPUtils.getInstance().put("errScanCountPosition", errScanCountPosition.get());
            SPUtils.getInstance().put("addr", nowSelectAddrTv.get());
            SPUtils.getInstance().put("typeName", typeNameTv.get());
            SPUtils.getInstance().put("typeCommand", typeCommandTv.get());
            KLog.e(TAG, "\n\r" +
                    "typeName--:" + typeNameTv.get() + "\n\r" +
                    "typeCommand--:" + typeCommandTv.get() + "\n\r" +
                    "cycleInterval--:" + cycleIntervalNum.get() + "\n\r" +
                    "cycleIntervalPosition--:" + cycleIntervalPosition.get() + "\n\r" +
                    "errScanCount--:" + errScanCountNum.get() + "\n\r" +
                    "errScanCountPosition--:" + errScanCountPosition.get() + "\n\r" +
                    "addr--:" + nowSelectAddrTv.get());
            //保存参数后重新启动Service
            RxBus.getDefault().post(new NetTimerParamEntity());
        }
    });
}
