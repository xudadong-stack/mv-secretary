package com.secretary;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;
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

        // 确保所有 activity / alias 不被旧的 PackageManager 状态禁用
        resetComponentStates();

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

    /**
     * 重置所有被 PackageManager 缓存为 disabled 的组件状态。
     * 防止之前伪装模式切换残留下的禁用设置导致 Activity not found 崩溃。
     */
    private void resetComponentStates() {
        PackageManager pm = getPackageManager();
        String pkg = getPackageName();

        // 全部设置为默认状态（跟随 manifest 中的 enabled= 属性）
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".ui.SplashActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
        );
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".FakeCalculator"),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
        );
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".FakeCalendar"),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
        );
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
