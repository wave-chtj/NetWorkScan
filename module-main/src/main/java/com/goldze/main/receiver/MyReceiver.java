package com.goldze.main.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.goldze.main.service.NetWorkService;
import com.goldze.main.ui.MainActivity;

import me.goldze.mvvmhabit.utils.KLog;

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
            Intent i = new Intent(context, NetWorkService.class);
            KLog.e(TAG,"进来了 myreceiver");
            context.startService(i);
        }
    }
}
