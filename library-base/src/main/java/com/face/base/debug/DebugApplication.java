package com.face.base.debug;

import android.content.IntentFilter;
import android.os.Build;

import com.chtj.base_iotutils.keepservice.BaseIotUtils;
import com.face.base.config.ModuleLifecycleConfig;

import me.goldze.mvvmhabit.base.BaseApplication;
import me.goldze.mvvmhabit.netbus.NetStateReceiver;

/**
 * Created by goldze on 2018/6/25 0025.
 * debug包下的代码不参与编译，仅作为独立模块运行时初始化数据
 */

public class DebugApplication extends BaseApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        BaseIotUtils.instance().create(this);
        //初始化组件(靠前)
        ModuleLifecycleConfig.getInstance().initModuleAhead(this);
        //....
        //初始化组件(靠后)
        ModuleLifecycleConfig.getInstance().initModuleLow(this);
        // 尽可能早的进行这一步操作, 建议在 Application 中完成初始化操作
        //动态注册网络变化广播
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //实例化IntentFilter对象
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            NetStateReceiver netBroadcastReceiver = new NetStateReceiver();
            //注册广播接收
            registerReceiver(netBroadcastReceiver, filter);
        }
        /*开启网络广播监听*/
        NetStateReceiver.registerNetworkStateReceiver(this);
    }
}
