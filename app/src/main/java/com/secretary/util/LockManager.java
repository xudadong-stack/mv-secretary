package com.secretary.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for password encryption and lock state management.
 * Uses SHA-256 hashing with EncryptedSharedPreferences for secure storage.
 */
public class LockManager {

    private static final String PREFS_NAME = "lock_prefs";
    private static final String KEY_PASSWORD_HASH = "password_hash";
    private static final String KEY_IS_LOCKED = "is_locked";

    private SharedPreferences prefs;
    private static LockManager instance;

    private LockManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            prefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to regular SharedPreferences if encryption fails
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public static synchronized LockManager getInstance(Context context) {
        if (instance == null) {
            instance = new LockManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Hash a password string using SHA-256.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Check if a password has been set.
     */
    public boolean hasPassword() {
        return prefs.contains(KEY_PASSWORD_HASH);
    }

    /**
     * Set the application password (stores SHA-256 hash).
     */
    public void setPassword(String password) {
        String hash = hashPassword(password);
        prefs.edit().putString(KEY_PASSWORD_HASH, hash).apply();
    }

    /**
     * Verify the entered password against the stored hash.
     */
    public boolean verifyPassword(String password) {
        String storedHash = prefs.getString(KEY_PASSWORD_HASH, null);
        if (storedHash == null) return false;
        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
    }

    /**
     * Get whether the app is currently locked.
     */
    public boolean isLocked() {
        return prefs.getBoolean(KEY_IS_LOCKED, true);
    }

    /**
     * Set the locked state.
     */
    public void setLocked(boolean locked) {
        prefs.edit().putBoolean(KEY_IS_LOCKED, locked).apply();
    }
}
