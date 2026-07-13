package com.secretary.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;

/**
 * Utility class for switching app icon between normal and disguised icons.
 *
 * The AndroidManifest.xml defines <activity-alias> entries:
 *   .FakeCalculator  -> Calculator icon + label
 *   .FakeCalendar    -> Calendar icon + label
 *
 * By enabling one alias and disabling others (including the main activity),
 * the launcher icon changes without needing to reinstall.
 */
public class IconDisguiseUtil {

    /**
     * Switch to the normal "MV Secretary" icon.
     */
    public static void switchToNormal(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        String pkg = activity.getPackageName();

        // Enable main launcher activity
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".ui.SplashActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );

        // Disable fake aliases
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".FakeCalculator"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".FakeCalendar"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    /**
     * Switch to the Calculator disguised icon.
     */
    public static void switchToCalculator(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        String pkg = activity.getPackageName();

        // Enable calculator alias
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".FakeCalculator"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );

        // Disable original launcher
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".ui.SplashActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );

        // Disable the other alias to prevent duplicate icons
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".FakeCalendar"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    /**
     * Switch to the Calendar disguised icon.
     */
    public static void switchToCalendar(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        String pkg = activity.getPackageName();

        // Enable calendar alias
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".FakeCalendar"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );

        // Disable original launcher
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".ui.SplashActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );

        // Disable the other alias to prevent duplicate icons
        pm.setComponentEnabledSetting(
                new ComponentName(pkg, pkg + ".FakeCalculator"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }
}
