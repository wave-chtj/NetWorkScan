package com.face.ethlinstener.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.face.base.router.RouterActivityPath;
import com.face.ethlinstener.BR;
import com.face.ethlinstener.R;
import com.face.ethlinstener.databinding.ActivityEthlinstenerBinding;
import com.face.ethlinstener.ui.service.EthLinstenerService;
import com.face.ethlinstener.ui.viewmodel.EthLinstenerViewModel;
import com.face_chtj.base_iotutils.keeplive.BaseIotUtils;

import java.util.Arrays;

import me.goldze.mvvmhabit.base.BaseActivity;
import me.goldze.mvvmhabit.utils.SPUtils;
import me.goldze.mvvmhabit.utils.Utils;

/**
 * Created by goldze on 2018/6/21.
 */
@Route(path = RouterActivityPath.User.PAGER_USERDETAIL)
public class EthLinstenerActivity extends BaseActivity<ActivityEthlinstenerBinding, EthLinstenerViewModel> {
    String[] cycleIntervalInfo = Utils.getContext().getResources().getStringArray(R.array.ethlinstener_cycleInterval);//网络检测间隔时间

    @Override
    public void initParam() {
    }

    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_ethlinstener;
    }

    @Override
    public int initVariableId() {
        return BR.viewModel;
    }

    @Override
    public void initData() {
    }

    @Override
    public void initViewObservable() {
        super.initViewObservable();
        TestArrayAdapter testArrayAdapter1 = new TestArrayAdapter(this, Arrays.asList(cycleIntervalInfo));
        binding.spCycleInterval.setAdapter(testArrayAdapter1);
        int cycleIntervalPosition = SPUtils.getInstance().getInt("cycleIntervalPosition", 0);
        binding.spCycleInterval.setSelection(cycleIntervalPosition);

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

        //①初始化后台保活Service
        BaseIotUtils.initSerice(EthLinstenerService.class, BaseIotUtils.DEFAULT_WAKE_UP_INTERVAL);
        EthLinstenerService.sShouldStopService = false;
        BaseIotUtils.startServiceMayBind(EthLinstenerService.class);

    }
}

