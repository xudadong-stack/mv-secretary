package com.secretary.ui;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.secretary.R;

import java.io.File;

/**
 * Full-screen image preview with pinch-to-zoom support.
 * Uses Android's built-in Matrix gesture scaling (no external library needed).
 */
public class ImagePreviewActivity extends BaseLockActivity {

    private ImageView ivPreview;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    private PointF startPoint = new PointF();
    private PointF midPoint = new PointF();
    private float oldDist = 1f;

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private float minScale = 0.5f;
    private float maxScale = 5.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        String filePath = getIntent().getStringExtra("file_path");
        ivPreview = findViewById(R.id.iv_preview);
        ImageView btnClose = findViewById(R.id.btn_close);

        if (filePath != null) {
            Glide.with(this)
                    .load(new File(filePath))
                    .apply(new RequestOptions().fitCenter())
                    .into(ivPreview);
        }

        btnClose.setOnClickListener(v -> finish());

        ivPreview.setOnTouchListener((v, event) -> {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    startPoint.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(midPoint, event);
                        mode = ZOOM;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - startPoint.x,
                                event.getY() - startPoint.y);
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
            }
            ivPreview.setImageMatrix(matrix);
            ivPreview.setScaleType(ImageView.ScaleType.MATRIX);
            return true;
        });

        ivPreview.setScaleType(ImageView.ScaleType.MATRIX);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
