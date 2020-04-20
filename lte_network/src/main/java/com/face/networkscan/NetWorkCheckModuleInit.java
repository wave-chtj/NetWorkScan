package com.face.networkscan;

import android.app.Application;

import com.face.base.base.IModuleInit;

import me.goldze.mvvmhabit.utils.KLog;

/**
 * Created by goldze on 2018/6/21 0021.
 */

public class NetWorkCheckModuleInit implements IModuleInit {

    @Override
    public boolean onInitAhead(Application application) {
        KLog.d("主业务模块初始化 -- onInitAhead");
        return false;
    }

    @Override
    public boolean onInitLow(Application application) {
        KLog.d("主业务模块初始化 -- onInitLow");
        return false;
    }
}
