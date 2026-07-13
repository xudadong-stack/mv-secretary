package com.secretary.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.secretary.R;
import com.secretary.util.LockManager;

/**
 * LockActivity handles password entry and setup.
 * - If no password is set, shows setup mode (set + confirm).
 * - If password is set, shows unlock mode (enter password).
 */
public class LockActivity extends AppCompatActivity {

    private LockManager lockManager;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnConfirm;
    private TextView tvError;

    private boolean isSetupMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        lockManager = LockManager.getInstance(this);
        isSetupMode = !lockManager.hasPassword();

        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnConfirm = findViewById(R.id.btn_confirm);
        tvError = findViewById(R.id.tv_error);

        if (isSetupMode) {
            etPassword.setHint(R.string.lock_set_password);
            etConfirmPassword.setVisibility(View.VISIBLE);
        } else {
            etPassword.setHint(R.string.lock_hint);
            etConfirmPassword.setVisibility(View.GONE);
        }

        btnConfirm.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            if (password.isEmpty()) {
                showError(getString(R.string.settings_empty_password));
                return;
            }
            if (isSetupMode) {
                handleSetup(password);
            } else {
                handleUnlock(password);
            }
        });

        TextWatcher clearError = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etPassword.addTextChangedListener(clearError);
        if (etConfirmPassword != null) {
            etConfirmPassword.addTextChangedListener(clearError);
        }
    }

    private void handleSetup(String password) {
        String confirm = etConfirmPassword.getText().toString().trim();
        if (!password.equals(confirm)) {
            showError(getString(R.string.lock_password_mismatch));
            return;
        }
        lockManager.setPassword(password);
        lockManager.setLocked(false);
        navigateToMain();
    }

    private void handleUnlock(String password) {
        if (lockManager.verifyPassword(password)) {
            lockManager.setLocked(false);
            navigateToMain();
        } else {
            showError(getString(R.string.lock_password_wrong));
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
