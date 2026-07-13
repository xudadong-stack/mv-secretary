package com.secretary.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.qq.e.ads.splash.SplashAD;
import com.qq.e.ads.splash.SplashADListener;
import com.qq.e.comm.util.AdError;
import com.secretary.R;
import com.secretary.util.LockManager;

/**
 * SplashActivity handles 优量汇（腾讯广告）splash ad loading,
 * then routes to LockActivity or MainActivity based on password state.
 */
public class SplashActivity extends AppCompatActivity {

    private FrameLayout mSplashContainer;
    private SplashAD mSplashAd;
    private boolean mHasNavigated = false; // 防止重复跳转

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mSplashContainer = findViewById(R.id.splash_container);

        // 开始请求开屏广告
        loadSplashAd();
    }

    private void loadSplashAd() {
        // 旧版 API：不传容器，等 onADLoaded 后再调用 showAd 展示
        mSplashAd = new SplashAD(this, "2383496646200391", new SplashADListener() {
            @Override
            public void onADLoaded(long duration) {
                // 广告素材加载成功，展示广告
                if (mSplashAd != null) {
                    mSplashAd.showAd(mSplashContainer);
                }
            }

            @Override
            public void onADTick(long millisUntilFinished) {
                // 倒计时回调，可在此更新自定义跳过按钮文字
            }

            @Override
            public void onADDismissed() {
                // 广告关闭（用户点击跳过 或 倒计时结束）
                goToNextPage();
            }

            @Override
            public void onADPresent() {
                // 广告成功展示
            }

            @Override
            public void onADClicked() {
                // 广告被点击
            }

            @Override
            public void onNoAD(AdError adError) {
                // 广告加载失败（网络问题或无广告填充），绝不能卡住用户
                goToNextPage();
            }

            @Override
            public void onADExposure() {
                // 广告曝光
            }
        });

        // 兜底保护：如果请求被意外卡住，15 秒后强制进入 App
        mSplashContainer.postDelayed(() -> {
            if (!mHasNavigated) {
                goToNextPage();
            }
        }, 15000);
    }

    /**
     * 跳转到下一页面（密码锁或主页）。
     * 通过 mHasNavigated 防止重复跳转。
     */
    private void goToNextPage() {
        if (mHasNavigated) return;
        mHasNavigated = true;

        // 销毁广告资源
        if (mSplashAd != null) {
            mSplashAd = null;
        }

        // 判断密码状态并跳转
        LockManager lockManager = LockManager.getInstance(this);

        Intent intent;
        if (!lockManager.hasPassword()) {
            // 尚未设置密码 → 去设置密码
            intent = new Intent(this, LockActivity.class);
        } else if (lockManager.isLocked()) {
            // 已设置密码且当前锁定 → 去解锁
            intent = new Intent(this, LockActivity.class);
        } else {
            // 已解锁 → 直接进主页
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish(); // 销毁开屏页
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 防止 Activity 退出时广告仍在加载/播放导致崩溃
        if (mSplashAd != null) {
            mSplashAd = null;
        }
    }
}
