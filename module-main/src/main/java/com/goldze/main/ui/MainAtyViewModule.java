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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.goldze.base.db.DbHelper;
import com.goldze.main.R;
import com.goldze.main.entity.FormEntity;
import com.goldze.main.entity.NetTimerParamEntity;
import com.goldze.main.entity.ParamEntity;
import com.goldze.main.entity.ServiceAboutEntity;
import com.goldze.main.entity.SpinnerItemData;
import com.goldze.main.service.NetWorkService;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.base.BaseViewModel;
import me.goldze.mvvmhabit.binding.command.BindingAction;
import me.goldze.mvvmhabit.binding.command.BindingCommand;
import me.goldze.mvvmhabit.binding.command.BindingConsumer;
import me.goldze.mvvmhabit.binding.viewadapter.spinner.IKeyAndValue;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;
import me.goldze.mvvmhabit.utils.ToastUtils;

public class MainAtyViewModule extends BaseViewModel {
    private static final String TAG = MainAtyViewModule.class.getSimpleName();
    public ObservableField<AppCompatActivity> mContext = new ObservableField<>();
    public ObservableInt cycleIntervalNum = new ObservableInt(3);
    public ObservableInt errScanCountNum = new ObservableInt(2);
    public ObservableInt cycleIntervalPosition = new ObservableInt(0);
    public ObservableInt errScanCountPosition = new ObservableInt(0);
    public ObservableField<String> addrConnTv = new ObservableField<>("");
    public ObservableField<String> openCloseTv = new ObservableField<>();
    public List<String> connadrList = new ArrayList<>();
    public ObservableField<String> nowSelectAddrTv = new ObservableField<>();
    public int defaultPositon=0;
    DbHelper dbHelper = null;
    PopupWindow popupWindow;
    ArrayAdapter<String> adapter;

    public MainAtyViewModule(@NonNull Application application) {
        super(application);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        openCloseTv.set(mContext.get().getResources().getString(R.string.main_off_service));
        MainActivity.sp_net_connect = mContext.get().findViewById(R.id.sp_net_connect);
        MainActivity.sp_net_connect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nowSelectAddrTv.set(connadrList.get(position));
                KLog.e(TAG,"选中了:"+nowSelectAddrTv.get());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        dbHelper = new DbHelper(mContext.get(), "connDB", null, 1);
        dbHelper.inItDb(dbHelper.getOther("connadr"));
        queryAll();//查询保存的ping 地址
        RxBus.getDefault().toObservable(ParamEntity.class).subscribe(new Consumer<ParamEntity>() {
            @Override
            public void accept(ParamEntity paramEntity) throws Exception {
                KLog.d(TAG, "ParamEntity: > type=" + paramEntity.getType() + ",param=" + paramEntity.getParam());
                if (paramEntity.getType() == 1) {//
                    cycleIntervalNum.set(Integer.parseInt(paramEntity.getParam().toString()));
                    cycleIntervalPosition.set(paramEntity.getPosition());
                } else {
                    errScanCountNum.set(Integer.parseInt(paramEntity.getParam().toString()));
                    errScanCountPosition.set(paramEntity.getPosition());
                }
            }
        });
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
     * 查询数据库
     *
     * @return
     */
    public void queryAll() {
        KLog.e(TAG, "刷新Spinner");
        String postionAddrtv =SPUtils.getInstance().getString("addr","");
        connadrList = new ArrayList<>();
        // 解析游标
        Cursor cursor = dbHelper.query("connadr");
        int i=0;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String addrContent = cursor.getString(cursor.getColumnIndex("addr"));
                //找到addr默认设置的下标
                if(addrContent!=null&&!addrContent.equals("")&&!postionAddrtv.equals("")){
                    if(postionAddrtv.equals(addrContent)){
                        defaultPositon=i;
                    }
                }
                connadrList.add(addrContent);
                KLog.e(TAG, "list value:" + addrContent);
                cursor.moveToNext();
                i++;
            }
        }
        adapter = new ArrayAdapter<String>(mContext.get(), android.R.layout.simple_dropdown_item_1line, android.R.id.text1, connadrList);
        MainActivity.sp_net_connect.setAdapter(adapter);
        if (connadrList == null || connadrList.size() <= 0) {
            KLog.e(TAG, "connadrList size:0");
        } else {
            KLog.e(TAG, "connadrList size:" + connadrList.size());
        }
        MainActivity.sp_net_connect.setSelection(defaultPositon);
    }

    /**
     * 查询数据库是否存在相同的数据
     *
     * @return
     */
    public boolean queryByAddr() {
        boolean isExist = false;//是否在数据库找到了对应的地址
        List<String> byAddrList = new ArrayList<>();
        // 解析游标
        Cursor cursor = dbHelper.queryByAddr(addrConnTv.get(), "connadr");
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


    //addr 监听
    public BindingCommand<IKeyAndValue> onaddrSelectorCommand = new BindingCommand<>(new BindingConsumer<IKeyAndValue>() {
        @Override
        public void call(IKeyAndValue iKeyAndValue) {
            KLog.e(TAG, "当前选择的地址为：key:" + iKeyAndValue.getKey() + ",value:" + iKeyAndValue.getValue());
            nowSelectAddrTv.set(iKeyAndValue.getKey());
        }
    });
    //设置参数
    public BindingCommand setParamClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            KLog.d("cycleInterval:" + cycleIntervalNum.get());
            SPUtils.getInstance().put("cycleInterval", cycleIntervalNum.get());
            SPUtils.getInstance().put("cycleIntervalPosition", cycleIntervalPosition.get());

            KLog.d("errScanCount:" + errScanCountNum.get());
            SPUtils.getInstance().put("errScanCount", errScanCountNum.get());
            SPUtils.getInstance().put("errScanCountPosition", errScanCountPosition.get());
            SPUtils.getInstance().put("addr",nowSelectAddrTv.get());
            RxBus.getDefault().post(new NetTimerParamEntity());
        }
    });
    //添加路由地址
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
                        ToastUtils.showLong("请输入正确的地址!");
                    } else {
                        addrConnTv.set(addrStr);
                        addNetWrok();
                    }
                    popupWindow.dismiss();
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

    //向数据库添加地址
    public void addNetWrok() {
        if (!queryByAddr()) {
            //添加数据
            ContentValues values = new ContentValues();
            values.put("addr", addrConnTv.get() + "");
            dbHelper.insert(values, "connadr");
            KLog.e(TAG, "添加的地址为：" + addrConnTv.get());
            //添加完成后更新数据
            queryAll();
        } else {
            ToastUtils.showLong("数据库存在相同的地址,请更换！");
            KLog.e(TAG, "数据库存在相同的地址,请更换！");
        }
    }

    //删除下标地址
    public BindingCommand delAddrClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            if(connadrList!=null&&connadrList.size()>0){
                connadrList.remove(nowSelectAddrTv.get());
                dbHelper.delete(nowSelectAddrTv.get(), "connadr");
                KLog.e(TAG,"执行了删除的操作："+nowSelectAddrTv.get()+",剩余地址的数量为："+connadrList.size());
                String postionAddrtv =SPUtils.getInstance().getString("addr","");
                //如果删除的地址等于数据库中保存的地址 需要删除
                if(!postionAddrtv.equals("")&&postionAddrtv.equals(nowSelectAddrTv.get())){
                    defaultPositon=0;
                    SPUtils.getInstance().put("addr","");
                    KLog.e(TAG,"保存的连接地址："+nowSelectAddrTv.get()+",已被删除！");
                }
                if(connadrList==null||connadrList.size()<=0){
                    nowSelectAddrTv.set("");//将当前选中的地址置为空
                    SPUtils.getInstance().put("addr","");
                    KLog.e(TAG,"地址已被全部清空！");
                }
                queryAll();
            }else{
                nowSelectAddrTv.set("");//将当前选中的地址置为空
                SPUtils.getInstance().put("addr","");
                KLog.e(TAG,"地址已被全部清空！");
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
}
