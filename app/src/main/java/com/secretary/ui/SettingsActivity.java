package com.secretary.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.secretary.R;
import com.secretary.util.IconDisguiseUtil;
import com.secretary.util.LockManager;

/**
 * Settings activity for:
 * - Changing the app password
 * - Switching the app icon disguise (normal / calculator / calendar)
 */
public class SettingsActivity extends BaseLockActivity {

    private LockManager lockManager;

    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmNewPassword;
    private Button btnChangePassword;

    private Button btnDisguiseNormal;
    private Button btnDisguiseCalculator;
    private Button btnDisguiseCalendar;
    private TextView tvFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        lockManager = LockManager.getInstance(this);

        // Top bar back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Password fields
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);
        btnChangePassword = findViewById(R.id.btn_change_password);

        // Disguise buttons
        btnDisguiseNormal = findViewById(R.id.btn_disguise_normal);
        btnDisguiseCalculator = findViewById(R.id.btn_disguise_calculator);
        btnDisguiseCalendar = findViewById(R.id.btn_disguise_calendar);

        // Feedback text
        tvFeedback = findViewById(R.id.tv_feedback);

        // Change password
        btnChangePassword.setOnClickListener(v -> {
            String oldPw = etOldPassword.getText().toString().trim();
            String newPw = etNewPassword.getText().toString().trim();
            String confirmPw = etConfirmNewPassword.getText().toString().trim();

            if (oldPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                showFeedback(getString(R.string.settings_empty_password), false);
                return;
            }

            if (!lockManager.verifyPassword(oldPw)) {
                showFeedback(getString(R.string.settings_old_password_wrong), false);
                return;
            }

            if (!newPw.equals(confirmPw)) {
                showFeedback(getString(R.string.lock_password_mismatch), false);
                return;
            }

            lockManager.setPassword(newPw);
            showFeedback(getString(R.string.settings_password_changed), true);
            etOldPassword.setText("");
            etNewPassword.setText("");
            etConfirmNewPassword.setText("");
        });

        // Icon disguise
        btnDisguiseNormal.setOnClickListener(v -> {
            try {
                IconDisguiseUtil.switchToNormal(this);
                showFeedback(getString(R.string.settings_switch_success), true);
            } catch (Exception e) {
                showFeedback("切换失败: " + e.getMessage(), false);
            }
        });

        btnDisguiseCalculator.setOnClickListener(v -> {
            try {
                IconDisguiseUtil.switchToCalculator(this);
                showFeedback(getString(R.string.settings_switch_success), true);
            } catch (Exception e) {
                showFeedback("切换失败: " + e.getMessage(), false);
            }
        });

        btnDisguiseCalendar.setOnClickListener(v -> {
            try {
                IconDisguiseUtil.switchToCalendar(this);
                showFeedback(getString(R.string.settings_switch_success), true);
            } catch (Exception e) {
                showFeedback("切换失败: " + e.getMessage(), false);
            }
        });
    }

    private void showFeedback(String message, boolean success) {
        tvFeedback.setText(message);
        tvFeedback.setTextColor(success ?
                getColor(R.color.accent_green) :
                getColor(R.color.accent_red));
        tvFeedback.setVisibility(View.VISIBLE);
    }
}
