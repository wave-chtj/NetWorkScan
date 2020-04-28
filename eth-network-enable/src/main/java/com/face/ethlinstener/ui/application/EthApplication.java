package com.face.ethlinstener.ui.application;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.face.ethlinstener.ui.crash.CrashHandler;
import com.face_chtj.base_iotutils.KLog;
import com.face_chtj.base_iotutils.keeplive.BaseIotUtils;


/**
 * Create on 2019/11/7
 * author chtj
 * desc
 */
public class EthApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        KLog.init(true);
        CrashHandler.getInstance().init(this);
        BaseIotUtils.instance().create(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }
}
