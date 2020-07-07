package com.gome.gmsecurityprogressview.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.gome.gmsecurityprogressview.util.SizeTransformer;

/**
 * @author Felix.Liang
 */
abstract class AnimatedSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final long MAX_UPDATE_DELAY = 20;
    private final SurfaceHolder mHolder;
    private final Paint mClearPaint;
    private UpdateThread mUpdateThread;
    private boolean mUpdating;
    private boolean mReady;

    public AnimatedSurfaceView(Context context) {
        this(context, null);
    }

    public AnimatedSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimatedSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AnimatedSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mUpdating = true;
        if (mUpdateThread == null) {
            mUpdateThread = new UpdateThread();
            mUpdateThread.start();
            mReady = true;
            onSurfaceReady();
        }
    }

    protected void onSurfaceReady() {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mUpdating = false;
        if (mUpdateThread != null) {
            mUpdateThread.interrupt();
            mReady = false;
            mUpdateThread = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mUpdating = false;
        if (mHolder != null) mHolder.removeCallback(this);
    }

    private class UpdateThread extends Thread {

        private boolean suspended = true;

        @Override
        public void run() {
            while (mUpdating) {
                try {
                    long start = System.currentTimeMillis();
                    drawSurface(true);
                    long duration = System.currentTimeMillis() - start;
                    final long sleepTime = Math.max(0, MAX_UPDATE_DELAY - duration);
                    Thread.sleep(sleepTime);
                    synchronized (this) {
                        while (suspended) {
                            wait();
                        }
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }

        synchronized void resumeUpdate() {
            notify();
        }

        private void setSuspended(boolean isSuspended) {
            if (suspended != isSuspended) {
                suspended = isSuspended;
                if (!suspended) {
                    resumeUpdate();
                }
            }
        }
    }

    protected void invalidateSurface() {
        invalidateSurface(true);
    }

    protected void invalidateSurface(boolean willUpdate) {
        try {
            drawSurface(willUpdate);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private synchronized void drawSurface(boolean willUpdate) throws IllegalStateException {
        if (mHolder != null) {
            Canvas canvas = mHolder.lockCanvas();
            if (canvas != null) {
                if (willUpdate) onUpdateFields();
                clearCanvas(canvas);
                onDrawContent(canvas);
            }
            mHolder.unlockCanvasAndPost(canvas);
        }
    }

    protected void onUpdateFields() {
    }

    private void clearCanvas(Canvas canvas) {
        canvas.drawPaint(mClearPaint);
    }

    protected void onDrawContent(Canvas canvas) {
    }

    protected void setUpdate(boolean update) {
        if (mUpdateThread != null) {
            mUpdateThread.setSuspended(!update);
        }
    }

    protected boolean isUpdating() {
        return mUpdateThread != null && !mUpdateThread.suspended;
    }

    protected int dp2px(float dpValue) {
        return SizeTransformer.dp2px(getContext(), dpValue);
    }

    protected boolean isReady() {
        return mReady;
    }
}
