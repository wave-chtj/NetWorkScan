package com.face.lte_networkscanreboot;

import android.content.Context;
import android.support.multidex.MultiDex;

import com.face.base.debug.DebugApplication;

/**
 * Create on 2019/12/26
 * author chtj
 * desc
 */
public class MyApplication extends DebugApplication {
    /**
     * 分割 Dex 支持
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
