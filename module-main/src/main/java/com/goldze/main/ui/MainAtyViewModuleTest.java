package com.goldze.main.ui;

import android.app.Application;
import android.content.Intent;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.goldze.main.entity.NetTimerParamEntity;
import com.goldze.main.entity.ParamEntity;
import com.goldze.main.entity.ServiceAboutEntity;
import com.goldze.main.service.NetWorkService;

import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.base.BaseViewModel;
import me.goldze.mvvmhabit.binding.command.BindingAction;
import me.goldze.mvvmhabit.binding.command.BindingCommand;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;

public class MainAtyViewModuleTest extends BaseViewModel {
    private static final String TAG = MainAtyViewModuleTest.class.getSimpleName() ;
    public ObservableField<AppCompatActivity> mContext = new ObservableField<>();
    public ObservableInt cycleIntervalNum = new ObservableInt(3);
    public ObservableInt errScanCountNum = new ObservableInt(2);
    public ObservableInt cycleIntervalPosition = new ObservableInt(0);
    public ObservableInt errScanCountPosition = new ObservableInt(0);
    public ObservableField<String> openCloseTv = new ObservableField<>("关闭后台服务");

    public MainAtyViewModuleTest(@NonNull Application application) {
        super(application);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        RxBus.getDefault().toObservable(ParamEntity.class).subscribe(new Consumer<ParamEntity>() {
            @Override
            public void accept(ParamEntity paramEntity) throws Exception {
                KLog.d(TAG,"ParamEntity: > type=" + paramEntity.getType() + ",param=" + paramEntity.getParam());
                if (paramEntity.getType() == 1) {//
                    cycleIntervalNum.set(Integer.parseInt(paramEntity.getParam().toString()));
                    cycleIntervalPosition.set(paramEntity.getPosition());
                } else {
                    errScanCountNum.set(Integer.parseInt(paramEntity.getParam().toString()));
                    errScanCountPosition.set(paramEntity.getPosition());
                }
            }
        });
        //是否需要开启Service服务 默认情况下 需要开启
        boolean isOpenService = SPUtils.getInstance().getBoolean("isOpenService", true);
        if(isOpenService){
            //1关闭服务
            RxBus.getDefault().post(new ServiceAboutEntity());
            //2重新启动Service
            openService();
        }else{
            //关闭服务
            RxBus.getDefault().post(new ServiceAboutEntity());
            openCloseTv.set("开启后台服务");
        }
    }


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
            RxBus.getDefault().post(new NetTimerParamEntity());
        }
    });


    //开启网络检测服务
    //打开完成后 需要标识当前的状态为已打开状态 也就是待关闭后台服务的状态
    //openOrclose put 为 close的时候 标识界面的btn显示为 开启后台服务 此时Service已关闭
    //openOrclose put 为 open的时候 标识界面的btn显示为 关闭后台服务  此时Service已打开
    public void openService() {
        //下次启动时候需要自动重启
        SPUtils.getInstance().put("isOpenService",true);
        //标识当前的状态 为|关闭后台服务
        SPUtils.getInstance().put("openOrclose", "close");
        KLog.e(TAG,"开启了服务>当前的状态为：" + SPUtils.getInstance().getString("openOrclose", ""));
        openCloseTv.set("关闭后台服务");
        //开启服务
        mContext.get().startService(new Intent(mContext.get(), NetWorkService.class));
    }

    //关闭网络检测服务
    //关闭完成后 需要标识当前的状态为已关闭状态 也就是待开启后台服务的状态
    //openOrclose put 为 close的时候 标识界面的btn显示为 开启后台服务 此时Service已关闭
    //openOrclose put 为 open的时候 标识界面的btn显示为 关闭后台服务  此时Service已打开
    public void closeService() {
        //下次启动时候只能手动启动
        SPUtils.getInstance().put("isOpenService",false);
        //标识当前的状态 为|开启后台服务
        SPUtils.getInstance().put("openOrclose", "open");
        KLog.e(TAG,"关闭了服务>当前的状态为：" + SPUtils.getInstance().getString("openOrclose", ""));
        openCloseTv.set("开启后台服务");
        //关闭服务
        RxBus.getDefault().post(new ServiceAboutEntity());
    }

    //关闭网络检测服务
    public BindingCommand closeServiceClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            //默认情况下Service需要自动打开
            String openOrHide = SPUtils.getInstance().getString("openOrclose", "open");
            if (openOrHide.equals("open")) {
                openService();
            } else {
                closeService();
            }
        }
    });
}
