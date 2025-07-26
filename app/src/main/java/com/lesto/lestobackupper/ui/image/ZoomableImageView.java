package com.lesto.lestobackupper.ui.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;

public class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView {
    private final Matrix matrix = new Matrix();
    private ScaleGestureDetector zoomDetector;
    private GestureDetector doubleClickDetector;
    private float lastTouchX;
    private float lastTouchY;
    private boolean is_zooming = false;


    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        zoomDetector = new ScaleGestureDetector(context, new ScaleListener());
        doubleClickDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                resetZoom();
                return true;
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetZoom();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        resetZoom(); // make the image fit
    }

    @Override
    public void setImageBitmap(Bitmap drawable) {
        super.setImageBitmap(drawable);
        resetZoom(); // make the image fit
    }

    private void resetZoom() {

        Drawable drawable = getDrawable();
        float scale = 1;
        int imageWidth = getWidth();
        int imageHeight = getHeight();
        if (drawable != null) {
            imageWidth = drawable.getIntrinsicWidth();
            imageHeight = drawable.getIntrinsicHeight();
            float scaleX = (float) getWidth() / imageWidth;
            float scaleY = (float) getHeight() / imageHeight;
            scale = Math.min(scaleX, scaleY);
        }

        float dx = (getWidth() - imageWidth) / 2.0f;
        float dy = (getHeight() - imageHeight) / 2.0f;

        matrix.reset();
        matrix.postTranslate(dx, dy);
        matrix.postScale(scale, scale, getWidth() / 2f, getHeight() / 2f);

        Log.d("ZOOM", "imageWidth is : "+imageWidth + " "+imageHeight + " " + getWidth() + " " + getHeight()+  " scale "+ scale + " d " +dx + " " +dy );
        setImageMatrix(matrix);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                Log.d("ZOOM", "ACTION_DOWN is : "+lastTouchX + " "+lastTouchY);
                break;
            case MotionEvent.ACTION_UP:
                Log.d("ZOOM", "ACTION_UP");
                is_zooming = false;
                performClick();  // Important!
                break;
            case MotionEvent.ACTION_MOVE:

                if (!is_zooming && !zoomDetector.isInProgress()) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    Log.d("ZOOM", "ACTION_MOVE is : "+dx + " "+dy);

                    matrix.postTranslate(dx, dy);
                    setImageMatrix(matrix);

                    float[] values = new float[9];
                    matrix.getValues(values);
                    float finalx = values[Matrix.MTRANS_X];
                    float finaly = values[Matrix.MTRANS_Y];
                    Log.d("ZOOM", "ACTION_MOVE is : "+dx + " "+dy +" final" +finalx + " " +finaly);

                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }else{
                    is_zooming = true;
                }
                break;
            default:
                Log.d("ZOOM", "UNKNOWN: "+event.getActionMasked());
                break;
        }
        zoomDetector.onTouchEvent(event);
        doubleClickDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            Log.d("ZOOM", "scale factor is : "+scaleFactor + " "+getWidth() + " "+getHeight());
            //scale = Math.max(1f, Math.min(scale, 5f));
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            matrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
            setImageMatrix(matrix);
            return true;
        }
    }
}

