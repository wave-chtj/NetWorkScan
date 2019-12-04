package com.face.ethlinstener.ui.viewmodel;

import android.app.Application;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.chtj.base_iotutils.keepservice.BaseIotUtils;
import com.face.ethlinstener.ui.service.EthLinstenerService;

import me.goldze.mvvmhabit.base.BaseViewModel;
import me.goldze.mvvmhabit.binding.command.BindingAction;
import me.goldze.mvvmhabit.binding.command.BindingCommand;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.utils.SPUtils;

/**
 * Created by goldze on 2018/6/21.
 */

public class EthLinstenerViewModel extends BaseViewModel {

    public ObservableInt cycleIntervalPosition = new ObservableInt(0);//网络检查间隔时间 对应的下标
    public ObservableInt cycleIntervalNum = new ObservableInt(1);//网络检查间隔的时间 分钟
    public EthLinstenerViewModel(@NonNull Application application) {
        super(application);
    }
    //回传参数
    public BindingCommand startServiceClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            SPUtils.getInstance().put("cycleInterval", cycleIntervalNum.get());
            SPUtils.getInstance().put("cycleIntervalPosition", cycleIntervalPosition.get());
            EthLinstenerService.stopService();
            //①初始化后台保活Service
            BaseIotUtils.initSerice(EthLinstenerService.class, BaseIotUtils.DEFAULT_WAKE_UP_INTERVAL);
            EthLinstenerService.sShouldStopService = false;
            BaseIotUtils.startServiceMayBind(EthLinstenerService.class);
        }
    });
}
