package com.secretary.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.secretary.R;
import com.secretary.room.AppDatabase;
import com.secretary.room.FileDao;

import io.reactivex.disposables.CompositeDisposable;

/**
 * Main screen: Android desktop-style category grid launcher.
 * Each category opens a FileListActivity scoped to that type.
 */
public class MainActivity extends BaseLockActivity {

    private static final int[] CATEGORY_IDS = {
            R.id.cat_all, R.id.cat_image, R.id.cat_video,
            R.id.cat_audio, R.id.cat_other
    };
    private static final String[] CATEGORY_TYPES = {
            "all", "image", "video", "audio", "other"
    };

    private FileDao fileDao;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileDao = AppDatabase.getInstance(this).fileDao();

        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // Setup category grid clicks
        for (int i = 0; i < CATEGORY_IDS.length; i++) {
            final String type = CATEGORY_TYPES[i];
            findViewById(CATEGORY_IDS[i]).setOnClickListener(v -> {
                Intent intent = new Intent(this, FileListActivity.class);
                intent.putExtra("category_type", type);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        disposables.dispose();
        super.onDestroy();
    }
}
