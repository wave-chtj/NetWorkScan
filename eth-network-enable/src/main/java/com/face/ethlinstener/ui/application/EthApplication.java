package com.face.ethlinstener.ui.application;

import android.content.Context;
import android.support.multidex.MultiDex;

import com.chtj.base_iotutils.KLog;
import com.chtj.base_iotutils.keepservice.BaseIotUtils;
import com.face.base.debug.DebugApplication;

import me.goldze.mvvmhabit.utils.Utils;

/**
 * Create on 2019/11/7
 * author chtj
 * desc
 */
public class EthApplication extends DebugApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        KLog.init(true);
        Utils.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }
}
