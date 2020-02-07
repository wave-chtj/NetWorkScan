package com.face.lte_networkscanreboot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.face.lte_networkscanreboot.ui.NetWorkSecondService;

import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.SPUtils;

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
            Intent i = new Intent(context, NetWorkSecondService.class);
            KLog.e(TAG,"进来了 myreceiver");
            SPUtils.getInstance().put("isOpenService", true);
            context.startService(i);
        }else if(intent.getAction().equals(NetWorkSecondService.ACTION_CLOSE_ALL)){
            KLog.e(TAG,"点击了退出按钮");
            SPUtils.getInstance().put("isOpenService", false);
            NetWorkSecondService.getInstance().stopService();
            context.stopService(new Intent(context,NetWorkSecondService.class));
        }
    }
}
