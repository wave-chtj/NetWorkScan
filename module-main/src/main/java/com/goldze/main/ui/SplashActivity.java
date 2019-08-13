package com.goldze.main.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;
import me.goldze.mvvmhabit.utils.KLog;

/**
 * Created by goldze on 2017/8/17 0017.
 * 冷启动
 */

public class SplashActivity extends FragmentActivity {

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                inMain();
            }
        }, 2 * 1000);
    }

    /**
     * 进入主页面
     */
    private void inMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
