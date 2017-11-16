package com.example.administrator.myapplication;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

public class MainActivity extends FragmentActivity {

    private MyHandler handler = new MyHandler(this);
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SplashFragment splashFragment = new SplashFragment();
        final ViewStub mainLayout = (ViewStub) findViewById(R.id.content_viewstub);

        //1.首先显示启动界面
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();


            if (fragmentTransaction != null) {
                fragmentTransaction.replace(R.id.container, splashFragment);
                fragmentTransaction.commit();
            }
        }


        //2.如果主页有网络耗时等操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(2000);
                handler.sendEmptyMessage(0);
            }
        }).start();


        //3.渲染结束后，立刻加载主页布局
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                Log.d("MainActivity", "getWindow().getDecorView().post()");
                View mainView = mainLayout.inflate();
                initView(mainView);
            }

        });


        //4.启动页面动画，延迟一下，播放完动画，执行remove
        getWindow().getDecorView().postDelayed(new DelayRunnableImpl(this,splashFragment),2000);


    }

    //初始化主页View
    private void initView(View mainView) {
        if (mainView != null) {
            progressBar = (ProgressBar) mainView.findViewById(R.id.progressbar);
            progressBar.setVisibility(View.VISIBLE);
        }
    }


    private static class MyHandler extends Handler {
        private WeakReference<MainActivity> wef;

        public MyHandler(MainActivity mainActivity) {
            wef = new WeakReference<MainActivity>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MainActivity mainActivity = wef.get();
            if (mainActivity != null) {
                mainActivity.progressBar.setVisibility(View.GONE);
            }
            switch (msg.what) {

            }
        }
    }


    private class DelayRunnableImpl implements Runnable {

        WeakReference<Context> contextWref;
        WeakReference<Fragment> fragmentWref;

        private DelayRunnableImpl(Context context, Fragment fragment) {
            contextWref = new WeakReference<Context>(context);
            fragmentWref = new WeakReference<Fragment>(fragment);
        }

        @Override
        public void run() {

            FragmentActivity context = (FragmentActivity) contextWref.get();
            if (context != null) {
                FragmentManager fragmentManager = context.getSupportFragmentManager();
                if (fragmentManager != null) {
                    SplashFragment splashFragment = (SplashFragment) fragmentWref.get();
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    if (ft != null) {
                        ft.remove(splashFragment);
                        ft.commit();
                    }
                }
            }

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
