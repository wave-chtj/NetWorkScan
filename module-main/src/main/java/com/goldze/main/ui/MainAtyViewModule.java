package com.goldze.main.ui;

import android.app.ActionBar;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import com.goldze.main.entity.ModelTypeEntity;
import com.goldze.main.entity.NetTimerParamEntity;
import com.goldze.main.entity.ServiceAboutEntity;
import com.goldze.main.service.NetWorkService;
import com.goldze.main.service.NetWorkServiceTest;
import com.goldze.main.utils.ModelType;
import com.goldze.main.utils.SystemInfoUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    public ObservableField<String> addrConnTv = new ObservableField<>("");//待添加的访问地址 用于新增的是否重复|新增
    public ObservableField<String> addrModelTypeTv = new ObservableField<>("");//待添加的机型 用于新增的是否重复|新增
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
        queryAllConn();
        queryAllModelType();
        // 根据isOpenService判断是否需要开启Service服务
        // 首次进来 默认情况下 需要开启
        // 第二次启动时按照 isOpenService 对应的value值进行判断
        boolean isOpenService = SPUtils.getInstance().getBoolean("isOpenService", true);
        if (isOpenService) {
            //1关闭服务
            RxBus.getDefault().post(new ServiceAboutEntity());
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
     */
    public void initModelType() {
        //获取机型
        String getModelTypeName = SystemInfoUtil.getModelType();
        KLog.e(TAG, "您当前获取到的机型为：" + getModelTypeName);
        if (getModelTypeName != null) {
            if (getModelTypeName.equals(ModelType.FEI_SI_KA_ER)) {
                modelTypeNameByFirst.set("飞思卡尔");
            } else if (getModelTypeName.equals(ModelType.RK_3399)) {
                modelTypeNameByFirst.set("rk3399");
            }
        }
        //是否为首次启动
        boolean isFristStart = SPUtils.getInstance().getBoolean("isFirst", true);
        if (isFristStart) {
            KLog.e(TAG, "添加一些默认机型:rk3399;飞思卡尔");
            ContentValues values = new ContentValues();
            values.put("typeName", "rk3399");
            values.put("typeCommand", "echo 1 > /sys/class/spi_sim_ctl/state");
            dbHelper.insertConn(values, DbHelper.TABLE_READER_TYPE);
            values = new ContentValues();
            values.put("typeName", "飞思卡尔");
            values.put("typeCommand", "echo 1 > /sys/devices/platform/imx6q_sim/sim_sysfs/state");
            dbHelper.insertConn(values, DbHelper.TABLE_READER_TYPE);

            KLog.e(TAG, "添加一些默认访问地址:");
            values = new ContentValues();
            values.put("addr", "www.google.cn");
            dbHelper.insertConn(values, DbHelper.TABLE_CONN_ADDR);
            values = new ContentValues();
            values.put("addr", "223.5.5.5");//阿里公共DNS
            dbHelper.insertConn(values, DbHelper.TABLE_CONN_ADDR);

            //如果首次进入 并且初始化机型后
            //需要去加载符合设备对应机型的选项 设置为默认
            if (modelTypeNameByFirst.get() != null && !modelTypeNameByFirst.get().equals("机型不详") && !modelTypeNameByFirst.get().equals("")) {
                SPUtils.getInstance().put("typeName", modelTypeNameByFirst.get());
                String getCommand=queryByTypeCommand();//根据机型获取命令
                if(getCommand!=null&&!getCommand.equals("")){
                    //根据机型获取到对应的adb命令后，去保存typeCommand
                    SPUtils.getInstance().put("typeCommand", getCommand);
                    KLog.e(TAG,"首次加载，根据机型获取到adb命令成功-->机型："+modelTypeNameByFirst.get()+",命令："+getCommand+",手动选择机型后将不在生效！");
                }else{
                    //如果根据机型获取不到命令 则重置typeName机型
                    SPUtils.getInstance().put("typeName","");
                    KLog.e(TAG,"根据机型获取adb命令失败，重置机型和命令为空！");
                }
            }
            //首次加载 设置默认的访问访问地址
            SPUtils.getInstance().put("addr","223.5.5.5");
            KLog.e(TAG,"首次加载，默认设置一个访问地址：223.5.5.5");
            SPUtils.getInstance().put("isFirst", false);
        } else {
            KLog.e(TAG, "不是首次启动,不执行机型添加操作");
        }

    }

    /**
     * 查询数据库
     *
     * @return
     */
    public void queryAllConn() {
        int defaultPositon = 0;
        KLog.e(TAG, "刷新Spinner Network check interval");
        String postionAddrtv = SPUtils.getInstance().getString("addr", "");
        connadrList = new ArrayList<>();
        // 解析游标
        Cursor cursor = dbHelper.query(DbHelper.TABLE_CONN_ADDR);
        int i = 0;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String addrContent = cursor.getString(cursor.getColumnIndex("addr"));
                //找到addr默认设置的下标
                if (addrContent != null && !addrContent.equals("") && !postionAddrtv.equals("")) {
                    if (postionAddrtv.equals(addrContent)) {
                        defaultPositon = i;
                    }
                }
                connadrList.add(addrContent);
                KLog.e(TAG, "获取一些内置存储访问地址-->list value:" + addrContent);
                cursor.moveToNext();
                i++;
            }
        }
        if (connadrList == null || connadrList.size() <= 0) {
            KLog.e(TAG, "connadrList size:0");
        } else {
            KLog.e(TAG, "connadrList size:" + connadrList.size());
        }
        RxBus.getDefault().post(new ChangeDataEntity(-1, defaultPositon, connadrList, ChangeDataEntity.DATA_TYPE.TYPE_CONN_ADDR));
    }

    /**
     * 查询全部的机型
     */
    public void queryAllModelType() {
        KLog.e(TAG, "刷新Spinner modelType");
        modelTypeList = new ArrayList<>();
        int defaultPositon = 0;//默认加载的机型的下标
        String postionTypeName = SPUtils.getInstance().getString("typeName", "");
        // 解析游标
        Cursor cursor = dbHelper.query(DbHelper.TABLE_READER_TYPE);
        int i = 0;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String id = cursor.getString(cursor.getColumnIndex("id"));
                String typeName = cursor.getString(cursor.getColumnIndex("typeName"));
                String command = cursor.getString(cursor.getColumnIndex("typeCommand"));
                //找到addr默认设置的下标
                if (typeName != null && !typeName.equals("") && !postionTypeName.equals("")) {
                    if (postionTypeName.equals(typeName)) {
                        defaultPositon = i;
                    }
                }
                //找到addr默认设置的下标
                modelTypeList.add(new ModelTypeEntity(id, typeName, command));
                KLog.e(TAG, "获取一些内置存储机型-->list value:" + typeName);
                cursor.moveToNext();
                i++;
            }
        }
        List<String> modelTypeNameList = new ArrayList<>();
        for (int j = 0; j < modelTypeList.size(); j++) {
            modelTypeNameList.add(modelTypeList.get(j).getTypeName() + "   |    " + modelTypeList.get(j).getTypeCommand());
        }
        if (modelTypeList == null || modelTypeList.size() <= 0) {
            KLog.e(TAG, "modelTypeList size:0");
        } else {
            KLog.e(TAG, "modelTypeList size:" + modelTypeList.size());
        }
        RxBus.getDefault().post(new ChangeDataEntity(-1, defaultPositon, modelTypeNameList, ChangeDataEntity.DATA_TYPE.TYPE_MODEL_TYPE));
    }

    /**
     * 查询数据库是否存在相同的访问地址
     *
     * @return
     */
    public boolean queryByAddr() {
        boolean isExist = false;//是否在数据库找到了对应的地址
        List<String> byAddrList = new ArrayList<>();
        // 解析游标
        Cursor cursor = dbHelper.queryByAddr(addrConnTv.get());
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                byAddrList.add(cursor.getString(cursor.getColumnIndex("addr")));
                cursor.moveToNext();
            }
        }
        if (byAddrList == null || byAddrList.size() <= 0) {
            isExist = false;//未找到
        } else {
            isExist = true;
        }
        return isExist;
    }


    /**
     * 查询数据库是否存在相同的机型
     *
     * @return
     */
    public boolean queryByModelType() {
        boolean isExist = false;//是否在数据库找到了对应的机型
        List<String> byAddrList = new ArrayList<>();
        // 解析游标
        Cursor cursor = dbHelper.queryByModelType(addrModelTypeTv.get());
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                byAddrList.add(cursor.getString(cursor.getColumnIndex("typeName")));
                cursor.moveToNext();
            }
        }
        if (byAddrList == null || byAddrList.size() <= 0) {
            isExist = false;//未找到
        } else {
            isExist = true;
        }
        return isExist;
    }

    //根据typeName获取typeCommand
    public String queryByTypeCommand() {
        List<String> byAddrList = new ArrayList<>();
        // 解析游标
        Cursor cursor = dbHelper.queryByModelType(modelTypeNameByFirst.get());
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                byAddrList.add(cursor.getString(cursor.getColumnIndex("typeCommand")));
                cursor.moveToNext();
            }
        }
        return byAddrList.get(0);
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
                        addrConnTv.set(addrStr);
                        if (!queryByAddr()) {
                            //添加数据
                            ContentValues values = new ContentValues();
                            values.put("addr", addrConnTv.get() + "");
                            dbHelper.insertConn(values, DbHelper.TABLE_CONN_ADDR);
                            KLog.e(TAG, "添加的地址为：" + addrConnTv.get());
                            //添加完成后更新数据
                            queryAllConn();
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
                        addrModelTypeTv.set(typeName);
                        if (!queryByModelType()) {
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
//        mPopWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
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
                queryAllConn();
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
            if (typeNameTv.get().equals("rk3399") || typeNameTv.get().equals("飞思卡尔")) {
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
        mContext.get().startService(new Intent(mContext.get(), /*NetWorkService.class*/NetWorkServiceTest.class));
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
        RxBus.getDefault().post(new ServiceAboutEntity());
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
            SPUtils.getInstance().put("cycleInterval", cycleIntervalNum.get());
            SPUtils.getInstance().put("cycleIntervalPosition", cycleIntervalPosition.get());
            SPUtils.getInstance().put("errScanCount", errScanCountNum.get());
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
