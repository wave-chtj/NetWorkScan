package com.face.ethlinstener.ui.activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import com.face.ethlinstener.R;
import com.face.ethlinstener.ui.service.EthLinstenerService;
import com.face_chtj.base_iotutils.SPUtils;
import com.face_chtj.base_iotutils.keeplive.BaseIotUtils;
import java.util.Arrays;

/**
 * Created by goldze on 2018/6/21.
 */
public class EthLinstenerActivity extends AppCompatActivity {
    String[] cycleIntervalInfo = BaseIotUtils.getContext().getResources().getStringArray(R.array.ethlinstener_cycleInterval);//网络检测间隔时间
    int cycleIntervalPosition=0;
    int cycleIntervalNum=1;
    Spinner spCycleInterval;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethlinstener);
        spCycleInterval=findViewById(R.id.sp_cycle_interval);
        initData();
    }
    public void initData(){
        TestArrayAdapter testArrayAdapter1 = new TestArrayAdapter(this, Arrays.asList(cycleIntervalInfo));
        spCycleInterval.setAdapter(testArrayAdapter1);
        cycleIntervalPosition = SPUtils.getInt("cycleIntervalPosition", 0);
        spCycleInterval.setSelection(cycleIntervalPosition);

        //循环间隔选择
        spCycleInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cycleIntervalNum=Integer.parseInt(cycleIntervalInfo[position]);
                cycleIntervalPosition=position;
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

    public void startServiceClick(View view){
        SPUtils.putInt("cycleInterval", cycleIntervalNum);
        SPUtils.putInt("cycleIntervalPosition", cycleIntervalPosition);
        EthLinstenerService.stopService();
        //①初始化后台保活Service
        BaseIotUtils.initSerice(EthLinstenerService.class, BaseIotUtils.DEFAULT_WAKE_UP_INTERVAL);
        EthLinstenerService.sShouldStopService = false;
        BaseIotUtils.startServiceMayBind(EthLinstenerService.class);
    }

}

