package com.goldze.main;

import android.app.Application;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.log.CustomLogger;
import com.goldze.base.base.IModuleInit;

import me.goldze.mvvmhabit.base.BaseApplication;
import me.goldze.mvvmhabit.utils.KLog;

/**
 * Created by goldze on 2018/6/21 0021.
 */

public class MainModuleInit implements IModuleInit {

    public JobManager jobManager;
    private static MainModuleInit mainModuleInit;
    public Application instance;

    @Override
    public boolean onInitAhead(Application application) {
        KLog.d("主业务模块初始化 -- onInitAhead");
        return false;
    }

    @Override
    public boolean onInitLow(Application application) {
        KLog.d("主业务模块初始化 -- onInitLow");
        //mainModuleInit = this;
        //instance = application;//1. Application的实例
        //configureJobManager();//2. 配置JobMananger
        return false;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public static MainModuleInit getInstance() {
        return mainModuleInit;
    }

    private void configureJobManager() {
        //3. JobManager的配置器，利用Builder模式
        Configuration configuration = new Configuration.Builder(instance.getApplicationContext())
                .customLogger(new CustomLogger() {
                    private static final String TAG = "JOBS";

                    @Override
                    public boolean isDebugEnabled() {
                        return true;
                    }

                    @Override
                    public void d(String text, Object... args) {
                        KLog.d(TAG, String.format(text, args));
                    }

                    @Override
                    public void e(Throwable t, String text, Object... args) {
                        KLog.e(TAG, String.format(text, args)+t.getMessage());
                    }

                    @Override
                    public void e(String text, Object... args) {
                        KLog.e(TAG, String.format(text, args));
                    }

                    @Override
                    public void v(String text, Object... args) {
                        KLog.e(TAG, String.format(text, args));
                    }
                })
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(3)//up to 3 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120)//wait 2 minute
                .build();
        jobManager = new JobManager(configuration);
    }
}
