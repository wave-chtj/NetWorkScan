package com.goldze.main.ui.by_old;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.goldze.base.router.RouterActivityPath;
import com.goldze.main.R;
import com.goldze.main.BR;
import com.goldze.main.databinding.ActivityMainBinding;
import com.goldze.main.entity.ChangeDataEntity;
import com.goldze.main.ui.TestArrayAdapter;
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
@Route(path = RouterActivityPath.Main.PAGER_MAIN)
public class MainActivity extends BaseActivity<ActivityMainBinding, MainAtyViewModule> {
    public static final String TAG = MainActivity.class.getSimpleName();
    String[] cycleIntervalInfo = Utils.getContext().getResources().getStringArray(R.array.main_cycleInterval);//网络检测间隔时间
    String[] errScanCountInfo =  Utils.getContext().getResources().getStringArray(R.array.main_errScanCount);//异常扫描的次数

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
        //ShellUtils.CommandResult resetCommand = ShellUtils.execCommand("echo 1 > /sys/devices/soc0/sim-gpios.40/sim_sysfs/state", false);
        //Log.e(TAG,"result="+resetCommand.result+",successMeg="+resetCommand.successMsg+",errMeg="+resetCommand.errorMsg);
        TestArrayAdapter testArrayAdapter1=new TestArrayAdapter(MainActivity.this, Arrays.asList(cycleIntervalInfo));
        binding.spCycleInterval.setAdapter(testArrayAdapter1);
        TestArrayAdapter testArrayAdapter2=new TestArrayAdapter(MainActivity.this, Arrays.asList(errScanCountInfo));
        binding.spErrScanCount.setAdapter(testArrayAdapter2);


        int cycleIntervalPosition = SPUtils.getInstance().getInt("cycleIntervalPosition", 0);
        int netErrCountPosition = SPUtils.getInstance().getInt("errScanCountPosition", 0);
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
                KLog.e(TAG, "选中了:" + viewModel.nowSelectAddrTv.get());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //机型
        binding.spModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.typeNameTv.set(viewModel.modelTypeList.get(position).getTypeName());
                viewModel.typeCommandTv.set(viewModel.modelTypeList.get(position).getTypeCommand());
                KLog.e(TAG, "选中了typeNameTv:" + viewModel.typeNameTv.get()+",typeCommandTv:"+viewModel.typeCommandTv.get());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        changeDataListener();
    }

    //数据改变时刷新Spinner
    public void changeDataListener(){
        RxBus.getDefault().toObservable(ChangeDataEntity.class).subscribe(new Consumer<ChangeDataEntity>() {
            @Override
            public void accept(ChangeDataEntity changeDataEntity) throws Exception {
               // ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainSecondAty.this, android.R.layout.simple_dropdown_item_1line, android.R.id.text1, changeDataEntity.getData());
                TestArrayAdapter adapter=new TestArrayAdapter(MainActivity.this,changeDataEntity.getData());
                if(changeDataEntity.getData_type()== ChangeDataEntity.DATA_TYPE.TYPE_CONN_ADDR){
                    binding.spNetConnect.setAdapter(adapter);
                    binding.spNetConnect.setSelection(changeDataEntity.getDefaultPositon());
                }else if(changeDataEntity.getData_type()== ChangeDataEntity.DATA_TYPE.TYPE_MODEL_TYPE){
                    binding.spModel.setAdapter(adapter);
                    binding.spModel.setSelection(changeDataEntity.getDefaultPositon());
                }
            }
        });
    }
}
