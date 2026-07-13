package com.secretary.ui;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.secretary.R;

import java.io.File;

/**
 * Video player using ExoPlayer with fullscreen toggle.
 *
 * - Portrait: shows close button + fullscreen button overlay
 * - Landscape (fullscreen): hides system bars, hides close button,
 *   shows exit-fullscreen button overlay
 * - Physical rotation is always detected and syncs the fullscreen state
 */
public class VideoPlayerActivity extends BaseLockActivity {

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private ImageButton btnFullscreen;
    private ImageButton btnClose;

    private boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.player_view);
        btnClose = findViewById(R.id.btn_close);
        btnFullscreen = findViewById(R.id.btn_fullscreen);

        String filePath = getIntent().getStringExtra("file_path");

        btnClose.setOnClickListener(v -> finish());

        btnFullscreen.setOnClickListener(v -> toggleFullscreen());

        if (filePath != null) {
            initializePlayer(Uri.fromFile(new File(filePath)));
        }

        // Initial UI: portrait
        updateUiForOrientation();
    }

    private void initializePlayer(Uri uri) {
        TrackSelection.Factory adaptiveTrackSelection = new AdaptiveTrackSelection.Factory();
        player = ExoPlayerFactory.newSimpleInstance(this,
                new DefaultTrackSelector(adaptiveTrackSelection));
        playerView.setPlayer(player);

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        String userAgent = Util.getUserAgent(this, "MVSecretary");
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, userAgent, bandwidthMeter);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);

        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
    }

    // ========== Fullscreen toggle ==========

    private void toggleFullscreen() {
        if (isFullscreen) {
            setPortraitMode();
        } else {
            setLandscapeFullscreen();
        }
    }

    private void setLandscapeFullscreen() {
        isFullscreen = true;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        enterImmersiveMode();
        updateUiForOrientation();
    }

    private void setPortraitMode() {
        isFullscreen = false;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        exitImmersiveMode();
        updateUiForOrientation();
    }

    // ========== Orientation ==========

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Sync fullscreen state with physical orientation
        boolean isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape && !isFullscreen) {
            // User physically rotated to landscape → enter fullscreen
            isFullscreen = true;
            enterImmersiveMode();
            updateUiForOrientation();
        } else if (!isLandscape && isFullscreen) {
            // User physically rotated back to portrait → exit fullscreen
            isFullscreen = false;
            exitImmersiveMode();
            updateUiForOrientation();
        }
    }

    // ========== System UI (compatible with API 21+) ==========

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void exitImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Re-apply immersive when user swipes to show bars and they auto-hide
        if (hasFocus && isFullscreen) {
            // Use IMMERSIVE_STICKY so that swiping shows bars temporarily
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    // ========== UI helpers ==========

    private void updateUiForOrientation() {
        if (isFullscreen) {
            btnClose.setVisibility(View.GONE);
            btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);
            btnFullscreen.setVisibility(View.VISIBLE);
        } else {
            btnClose.setVisibility(View.VISIBLE);
            btnFullscreen.setImageResource(R.drawable.ic_fullscreen);
            btnFullscreen.setVisibility(View.VISIBLE);
        }
    }

    // ========== Lifecycle ==========

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
