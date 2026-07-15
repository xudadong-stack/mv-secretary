package com.secretary.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.secretary.R;
import com.secretary.util.LockManager;

/**
 * LockActivity — 4-digit PIN entry.
 *
 * - Setup mode (no password set): enter a 4-digit PIN, then enter it again to confirm.
 * - Unlock mode: enter the 4-digit PIN. Auto-checks when 4 digits entered.
 *
 * Uses a numeric keypad (no EditText). Dots fill as digits are entered.
 */
public class LockActivity extends AppCompatActivity {

    // ─── UI ───
    private TextView tvTitle, tvHint, tvError;
    private View[] dotViews = new View[4];
    private View layoutPinDots;

    // ─── State ───
    private LockManager lockManager;
    private boolean isSetupMode;

    private StringBuilder pinBuffer = new StringBuilder(4);

    // Setup mode sub-states
    private boolean isFirstEntryDone = false;
    private String firstPin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        lockManager = LockManager.getInstance(this);
        isSetupMode = !lockManager.hasPassword();

        // ── Bind views ──
        tvTitle = findViewById(R.id.tv_title);
        tvHint = findViewById(R.id.tv_hint);
        tvError = findViewById(R.id.tv_error);
        layoutPinDots = findViewById(R.id.layout_pin_dots);
        dotViews[0] = findViewById(R.id.dot_0);
        dotViews[1] = findViewById(R.id.dot_1);
        dotViews[2] = findViewById(R.id.dot_2);
        dotViews[3] = findViewById(R.id.dot_3);

        // ── Configure for mode ──
        if (isSetupMode) {
            if (isFirstEntryDone) {
                tvTitle.setText("请再次输入密码");
                tvHint.setText("重复输入以确认");
            } else {
                tvTitle.setText("设置密码");
                tvHint.setText("请输入4位数字密码");
            }
        } else {
            tvTitle.setText("请输入密码");
            tvHint.setText("输入4位数字密码解锁");
        }

        // ── Number buttons ──
        int[] numIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };
        for (int id : numIds) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> onDigitClick(btn.getText().toString()));
        }

        // ── Backspace ──
        findViewById(R.id.btn_backspace).setOnClickListener(v -> onBackspaceClick());
    }

    // ─────────────────────────────────────────────────
    //  PIN input handlers
    // ─────────────────────────────────────────────────

    private void onDigitClick(String digit) {
        if (pinBuffer.length() >= 4) return;

        pinBuffer.append(digit);
        updateDots();

        if (pinBuffer.length() == 4) {
            // Auto-check on 4 digits
            onPinComplete();
        }
    }

    private void onBackspaceClick() {
        if (pinBuffer.length() == 0) return;
        pinBuffer.deleteCharAt(pinBuffer.length() - 1);
        updateDots();
        tvError.setVisibility(View.GONE);
    }

    /**
     * Called when 4 digits have been entered.
     */
    private void onPinComplete() {
        final String pin = pinBuffer.toString();
        pinBuffer.setLength(0);       // clear for next attempt
        updateDots();

        if (isSetupMode) {
            handleSetupPin(pin);
        } else {
            handleUnlockPin(pin);
        }
    }

    // ─────────────────────────────────────────────────
    //  Setup flow
    // ─────────────────────────────────────────────────

    private void handleSetupPin(String pin) {
        if (!isFirstEntryDone) {
            // First entry — store and ask for confirmation
            firstPin = pin;
            isFirstEntryDone = true;
            tvTitle.setText("请再次输入密码");
            tvHint.setText("重复输入以确认");
            flashDots();
        } else {
            // Second entry — compare
            if (pin.equals(firstPin)) {
                // Match! Save password
                lockManager.setPassword(pin);
                lockManager.setLocked(false);
                navigateToMain();
            } else {
                // Mismatch — show error, restart setup
                showError("两次密码不一致，请重新设置");
                isFirstEntryDone = false;
                firstPin = "";
                tvTitle.setText("设置密码");
                tvHint.setText("请输入4位数字密码");
            }
        }
    }

    // ─────────────────────────────────────────────────
    //  Unlock flow
    // ─────────────────────────────────────────────────

    private void handleUnlockPin(String pin) {
        if (lockManager.verifyPassword(pin)) {
            lockManager.setLocked(false);
            navigateToMain();
        } else {
            showError("密码错误");
        }
    }

    // ─────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────

    /** Update dot indicators to reflect pinBuffer length. */
    private void updateDots() {
        for (int i = 0; i < 4; i++) {
            dotViews[i].setBackgroundResource(
                    i < pinBuffer.length()
                            ? R.drawable.bg_pin_dot_filled
                            : R.drawable.bg_pin_dot_empty
            );
        }
    }

    /** Briefly fill all dots to indicate "got it, now confirm". */
    private void flashDots() {
        for (int i = 0; i < 4; i++) {
            dotViews[i].setBackgroundResource(R.drawable.bg_pin_dot_filled);
        }
        new Handler(getMainLooper()).postDelayed(() -> {
            for (int i = 0; i < 4; i++) {
                dotViews[i].setBackgroundResource(R.drawable.bg_pin_dot_empty);
            }
        }, 300);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
        // Auto-hide error after 2 seconds
        tvError.postDelayed(() -> {
            tvError.setVisibility(View.GONE);
        }, 2000);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
