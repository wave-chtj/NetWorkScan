package com.face.lte_networkscan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.goldze.mvvmhabit.utils.KLog;
import com.face.lte_networkscan.ui.NetWorkListenerService;
import com.face.lte_networkscan.ui.NetWorkScanAty;

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
            Intent i = new Intent(context, NetWorkListenerService.class);
            KLog.e(TAG,"进来了 myreceiver");
            context.startService(i);
        }else if(intent.getAction().equals(NetWorkListenerService.ACTION_CLOSE_ALL)){
            KLog.e(TAG,"点击了退出按钮");
            NetWorkListenerService.getInstance().stopService();
            context.stopService(new Intent(context, NetWorkListenerService.class));
        }
    }
}
