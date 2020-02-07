package com.face.lte_networkscan.ui;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.chtj.base_iotutils.ShellUtils;
import com.face.base.router.RouterActivityPath;
import com.face.lte_networkscan.BR;
import com.face.lte_networkscan.R;
import com.face.lte_networkscan.databinding.ActivityNetworkscanBinding;
import com.face.lte_networkscan.entity.ChangeDataEntity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.Arrays;

import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.base.BaseActivity;
import me.goldze.mvvmhabit.bus.RxBus;
import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;
import me.goldze.mvvmhabit.utils.Utils;

/**
 * Created by goldze on 2018/6/21
 */
@Route(path = RouterActivityPath.Main.PAGER_MAIN_SECOND)
public class NetWorkScanAty extends BaseActivity<ActivityNetworkscanBinding, NetWorkScanViewModule> {
    public static final String TAG = NetWorkScanAty.class.getSimpleName();
    String[] cycleIntervalInfo = Utils.getContext().getResources().getStringArray(R.array.main_cycleInterval);//网络检测间隔时间
    String[] errScanCountInfo = Utils.getContext().getResources().getStringArray(R.array.main_errScanCount);//异常扫描的次数

    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_networkscan;
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
                //KLog.e("网络权限是否申请成功:" + aBoolean);
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

        TestArrayAdapter testArrayAdapter1 = new TestArrayAdapter(NetWorkScanAty.this, Arrays.asList(cycleIntervalInfo));
        binding.spCycleInterval.setAdapter(testArrayAdapter1);

        TestArrayAdapter testArrayAdapter2 = new TestArrayAdapter(NetWorkScanAty.this, Arrays.asList(errScanCountInfo));
        binding.spErrScanCount.setAdapter(testArrayAdapter2);


        int cycleIntervalPosition = SPUtils.getInstance().getInt("cycleIntervalPosition", 1);
        int netErrCountPosition = SPUtils.getInstance().getInt("errScanCountPosition", 2);
        KLog.e("cycleIntervalPosition:" + cycleIntervalPosition + ",netErrCountPosition:" + netErrCountPosition);

        binding.spCycleInterval.setSelection(cycleIntervalPosition);
        binding.spErrScanCount.setSelection(netErrCountPosition);

        //循环间隔选择
        binding.spCycleInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.cycleIntervalNum.set(Integer.parseInt(cycleIntervalInfo[position]));
                viewModel.cycleIntervalPosition.set(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //异常重复扫描次数
        binding.spErrScanCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.errScanCountNum.set(Integer.parseInt(errScanCountInfo[position]));
                viewModel.errScanCountPosition.set(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //访问地址
        binding.spNetConnect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.nowSelectAddrTv.set(viewModel.connadrList.get(position));
                //KLog.e(TAG, "选中了:" + viewModel.nowSelectAddrTv.get());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        changeDataListener();
    }

    //数据改变时刷新Spinner
    public void changeDataListener() {
        RxBus.getDefault().toObservable(ChangeDataEntity.class).subscribe(new Consumer<ChangeDataEntity>() {
            @Override
            public void accept(ChangeDataEntity changeDataEntity) throws Exception {
                TestArrayAdapter adapter = new TestArrayAdapter(NetWorkScanAty.this, changeDataEntity.getData());
                if (changeDataEntity.getData_type() == ChangeDataEntity.DATA_TYPE.TYPE_CONN_ADDR) {
                    binding.spNetConnect.setAdapter(adapter);
                    binding.spNetConnect.setSelection(changeDataEntity.getDefaultPositon());
                }
            }
        });
    }
}
