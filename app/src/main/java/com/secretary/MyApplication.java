package com.secretary;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.qq.e.comm.managers.GDTADManager;
import com.secretary.room.AppDatabase;
import com.secretary.util.LockManager;

public class MyApplication extends Application {

    private static MyApplication instance;
    private static boolean isInForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化优量汇（腾讯广告）SDK
        GDTADManager.getInstance().initWith(getApplicationContext(), "1218848313");

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int activityCount = 0;

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {
                activityCount++;
                if (activityCount == 1) {
                    // App has come to foreground
                    isInForeground = true;
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                    // App has gone to background
                    isInForeground = false;
                    LockManager.getInstance(MyApplication.this).setLocked(true);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public static boolean isInForeground() {
        return isInForeground;
    }

    public AppDatabase getDatabase() {
        return AppDatabase.getInstance(this);
    }
}
