package com.goldze.main.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.goldze.base.router.RouterActivityPath;
import com.goldze.main.R;
import com.goldze.main.BR;
import com.goldze.main.databinding.ActivityMainBinding;
import com.goldze.main.entity.ParamEntity;
import com.goldze.main.entity.PositionEntity;
import com.goldze.main.service.NetWorkService;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.base.BaseActivity;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.http.NetworkUtil;
import me.goldze.mvvmhabit.netbus.NetUtils;
import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;

/**
 * Created by goldze on 2018/6/21
 */
@Route(path = RouterActivityPath.Main.PAGER_MAIN)
public class MainActivity extends BaseActivity<ActivityMainBinding, MainAtyViewModule> {
    public static Spinner sp_net_connect;

    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_main;
    }

    @Override
    public int initVariableId() {
        return BR.viewModel;
    }

    @Override
    public void initData() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                KLog.e("aBoolean:" + aBoolean);
            }
        });
    }

    @Override
    public void initParam() {
        super.initParam();
    }

    @Override
    public void initViewObservable() {
        super.initViewObservable();
        viewModel.mContext.set(this);

        final String[] cycleIntervalInfo = getResources().getStringArray(R.array.main_cycleInterval);
        final String[] errScanCountInfo = getResources().getStringArray(R.array.main_errScanCount);

        int cycleIntervalPosition = SPUtils.getInstance().getInt("cycleIntervalPosition", 0);
        int netErrCountPosition = SPUtils.getInstance().getInt("errScanCountPosition", 0);
        KLog.e("cycleIntervalPosition:" + cycleIntervalPosition + ",netErrCountPosition:" + netErrCountPosition);

        binding.spCycleInterval.setSelection(cycleIntervalPosition);
        binding.spErrScanCount.setSelection(netErrCountPosition);

        //循环间隔选择
        binding.spCycleInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ParamEntity paramEntity = new ParamEntity(1, cycleIntervalInfo[position], position);
                RxBus.getDefault().post(paramEntity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //异常重复扫描次数
        binding.spErrScanCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ParamEntity paramEntity = new ParamEntity(2, errScanCountInfo[position], position);
                RxBus.getDefault().post(paramEntity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
