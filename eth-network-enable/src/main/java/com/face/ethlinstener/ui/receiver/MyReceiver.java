package com.face.ethlinstener.ui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.face.ethlinstener.ui.service.EthLinstenerService;
import com.face_chtj.base_iotutils.keeplive.BaseIotUtils;
/**
 * 开机启动
 */
public class MyReceiver extends BroadcastReceiver {
    public static final String TAG=MyReceiver.class.getSimpleName();
    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.e(TAG,"EthLinstenerService start");
            //①初始化后台保活Service
            BaseIotUtils.initSerice(EthLinstenerService.class, BaseIotUtils.DEFAULT_WAKE_UP_INTERVAL);
            EthLinstenerService.sShouldStopService = false;
            BaseIotUtils.startServiceMayBind(EthLinstenerService.class);
        }
    }
}
