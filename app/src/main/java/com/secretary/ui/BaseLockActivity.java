package com.secretary.ui;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.secretary.util.LockManager;

/**
 * Base activity for all content-displaying activities.
 * Automatically redirects to LockActivity if the app is locked when resuming.
 */
public abstract class BaseLockActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();

        LockManager lockManager = LockManager.getInstance(this);
        if (lockManager.hasPassword() && lockManager.isLocked()) {
            Intent intent = new Intent(this, LockActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
