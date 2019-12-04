package com.face.ethlinstener;

import android.app.Application;

import com.chtj.base_iotutils.keepservice.BaseIotUtils;
import com.face.base.base.IModuleInit;

import me.goldze.mvvmhabit.utils.KLog;
import me.goldze.mvvmhabit.utils.Utils;

/**
 * Created by goldze on 2018/6/21 0021.
 */

public class EthLinstenerModuleInit implements IModuleInit {
    @Override
    public boolean onInitAhead(Application application) {
        KLog.e("EthLinstener init -- onInitAhead");
        return false;
    }

    @Override
    public boolean onInitLow(Application application) {
        KLog.e("EthLinstener init -- onInitLow");
        return false;
    }
}
