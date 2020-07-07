package com.gome.gmsecurityprogressview.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.gome.gmsecurityprogressview.R;
import com.gome.gmsecurityprogressview.util.Progressive;
import com.gome.gmsecurityprogressview.util.ShaderFactory;

/**
 * @author Felix.Liang
 */
public class GMSecurityProgressView extends AnimatedSurfaceView implements Progressive {

    private int mLineWidth;
    private int mAlphaLine;
    private int mAlphaBorderLine;
    private int mAlphaShadow;
    private int mRadius;
    // all the paints are below this line ==========================================================
    private Paint mLinesPaint;
    private Paint mLines2Paint;
    private Paint mLineBorderPaint;
    private Paint mLineShadowPaint;
    private Paint mBgPaint;
    // other fields ================================================================================
    private float mDeltaDistance;
    private int[] mShadowColors = {0x00FFFFFF, Color.WHITE};
    private float[] mShadowColorPos = {0.7f, 1};
    private int mVerticalOffset;
    private int mHorizontalOffset;
    private RectF[] mRectArray;
    private float mInscribedDegree = -1;
    private float mInCircleRadius;
    private Path mLinesPath = new Path();
    private Matrix mMatrix = new Matrix();
    private float mLinesDegree;
    private boolean mStarted;
    private boolean mIsFirst = true;
    private boolean mVisible;
    private boolean mRunning;
    private int mBgType = ShaderFactory.BLUE;
    private Shader mBgShader;
    private int mTargetType;
    private ValueAnimator mColorSwitchAnim;
    private ValueAnimator mCompleteAnim;
    private Path mDstLinePath = new Path();
    private float mShaderDegree;
    private boolean mCallStop;
    private Callback mCallback;
    private float mInitShaderDegree;
    private final Runnable mCompleteRunnable = new Runnable() {
        @Override
        public void run() {
            startCompleteAnim();
        }
    };
    private boolean mStartWhenReady;

    public GMSecurityProgressView(Context context) {
        this(context, null);
    }

    public GMSecurityProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs);
        initPaint();
        initRectF();
    }

    private void initRectF() {
        for (int i = 0; i < mRectArray.length; i++) {
            mRectArray[i] = new RectF();
        }
    }

    private void initFromAttributes(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.GMSecurityProgressView);
        int numberOfLines = array.getInteger(R.styleable.GMSecurityProgressView_numberOfLines, 6);
        numberOfLines = numberOfLines < 0 ? 0 : numberOfLines;
        mRectArray = new RectF[numberOfLines];
        mLineWidth = array.getDimensionPixelSize(R.styleable.GMSecurityProgressView_lineWidth, dp2px(0.5f));
        mAlphaLine = generateLegalAlpha(array.getInt(R.styleable.GMSecurityProgressView_alphaLine, 255));
        mAlphaBorderLine = generateLegalAlpha(array.getInt(R.styleable.GMSecurityProgressView_alphaBorderLine, 40));
        mAlphaShadow = generateLegalAlpha(array.getInt(R.styleable.GMSecurityProgressView_alphaShadow, 5));
        mRadius = array.getDimensionPixelSize(R.styleable.GMSecurityProgressView_radius, dp2px(103));
        array.recycle();
    }

    private void initPaint() {
        mLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinesPaint.setStyle(Paint.Style.STROKE);
        mLinesPaint.setColor(Color.WHITE);
        mLinesPaint.setAlpha(mAlphaLine);
        mLinesPaint.setStrokeWidth(mLineWidth);
        mLineBorderPaint = new Paint();
        mLineBorderPaint.set(mLinesPaint);
        mLineBorderPaint.setAlpha(mAlphaBorderLine);
        mLineShadowPaint = new Paint();
        mLineShadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mLineShadowPaint.setAlpha(mAlphaShadow);
        Shader shadowShader = new RadialGradient(0, 0, mRadius, mShadowColors, mShadowColorPos, Shader.TileMode.CLAMP);
        mLineShadowPaint.setShader(shadowShader);
        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mBgPaint.setStyle(Paint.Style.FILL);
        mLinesPaint.setShader(ShaderFactory.getLinesShader(0));
        mLines2Paint = new Paint();
        mLines2Paint.set(mLinesPaint);
    }

    private void updateLines() {
        mLinesPath.reset();
        final float degree = (float) Math.toRadians(mInscribedDegree);
        final float sin = (float) Math.sin(degree);
        final float cos = (float) Math.cos(degree);
        float inCircle = mInCircleRadius;
        for (int i = 0; i < mRectArray.length; i++) {
            RectF rectF = mRectArray[i];
            float w = (mRadius - inCircle) * sin + inCircle;
            float h = (mRadius - inCircle) * cos + inCircle;
            rectF.left = -w;
            rectF.right = w;
            rectF.top = -h;
            rectF.bottom = h;
            inCircle -= mDeltaDistance;
            final int r = (int) (mInCircleRadius - mDeltaDistance * i);
            mLinesPath.addRoundRect(rectF, r, r, Path.Direction.CCW);
        }
        rotateLinesTo(mLinesDegree);
    }

    private int generateLegalAlpha(int srcAlpha) {
        if (srcAlpha < 0) return 0;
        if (srcAlpha > 255) return 255;
        return srcAlpha;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mHorizontalOffset = width / 2;
        verifySize(width);
        mVerticalOffset = getPaddingTop() + mRadius + mLineWidth / 2;
        mBgShader = ShaderFactory.getBgShader(mBgType, height);
        mBgPaint.setShader(mBgShader);
    }

    private void verifySize(int width) {
        int viewWidth = getWidth();
        if (viewWidth > 0 && width > 0) {
            if (Math.abs(viewWidth - width) > 20) {
                Log.e("GMSecurityProgressView", "onWrongSize width = " + width);
                if (mOnErrorListener != null) {
                    mOnErrorListener.onWrongSize();
                }
            }
        }
    }

    @Override
    protected void onUpdateFields() {
        if (mIsFirst) {
            setInscribedDegree(0);
            mIsFirst = false;
        }
        if (mStarted) {
            if (mInscribedDegree < 45) {
                if (mInscribedDegree == 0 && mLinesDegree > -90) {
                    rotateLinesBy(-10);
                } else {
                    setInscribedDegree(mInscribedDegree + 3);
                    rotateLinesBy(-5);
                }
            } else {
                if (mCallStop && mLinesDegree % 45 == 0 && mLinesDegree % 90 != 0 && isUpdating()) {
                    setUpdate(false);
                    post(mCompleteRunnable);
                } else {
                    final int alpha = mLines2Paint.getAlpha();
                    if (alpha > 0) {
                        mLines2Paint.setAlpha(alpha - 5);
                    }
                    rotateLinesShaderBy(-3);
                    rotateLinesBy(1);
                }
            }
        }
    }

    private void rotateLinesShaderBy(float degree) {
        rotateLinesShaderTo(mShaderDegree + degree);
    }

    private void rotateLinesShaderTo(float degree) {
        if (degree < 0) degree += 360;
        degree %= 360;
        if (mShaderDegree != degree) {
            mShaderDegree = degree;
            mLinesPaint.setShader(ShaderFactory.getLinesShader(degree));
        }
    }

    public void setInscribedDegree(float degree) {
        if (mInscribedDegree != degree) {
            mInscribedDegree = degree;
            final float initRadiusPer = 0.7f;
            mInCircleRadius = ((1 - initRadiusPer) / 45 * mInscribedDegree + initRadiusPer) * mRadius;
            mDeltaDistance = (float) (mRadius * 0.03f / Math.cos(Math.toRadians(mInscribedDegree) + Math.PI / 6));
            updateLines();
        }
    }

    private void rotateLinesBy(float degree) {
        rotateLinesTo(mLinesDegree + degree);
    }

    private void rotateLinesTo(float degree) {
        degree %= 360;
        if (mLinesDegree != degree) {
            mLinesDegree = degree;
            mMatrix.reset();
            mMatrix.setRotate(degree);
            mLinesPath.transform(mMatrix, mDstLinePath);
        }
        if (mDstLinePath.isEmpty()) mDstLinePath.set(mLinesPath);
    }

    @Override
    protected void onDrawContent(Canvas canvas) {
        drawBackground(canvas);
        canvas.translate(mHorizontalOffset, mVerticalOffset);
        drawProgressBar(canvas);
        drawBorder(canvas);
    }

    private void drawBorder(Canvas canvas) {
        canvas.save();
        canvas.rotate(mLinesDegree);
        drawBorderLine(canvas);
        drawShadow(canvas);
        canvas.restore();
    }

    private void drawBorderLine(Canvas canvas) {
        canvas.drawRoundRect(mRectArray[0], mInCircleRadius, mInCircleRadius, mLineBorderPaint);
    }

    private void drawProgressBar(Canvas canvas) {
        canvas.drawPath(mDstLinePath, mLinesPaint);
        canvas.rotate(-mInscribedDegree + 180);
        if (mLines2Paint.getAlpha() > 0) canvas.drawPath(mDstLinePath, mLines2Paint);
    }

    private void drawShadow(Canvas canvas) {
        canvas.drawRoundRect(mRectArray[0], mInCircleRadius, mInCircleRadius, mLineShadowPaint);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawPaint(mBgPaint);
    }

    @Override
    public void start() {
        if (!isStarted()) {
            if (isReady()) setStarted(true);
            else setStartedWhenReady(true);
        }
    }

    private void setStartedWhenReady(boolean startedWhenReady) {
        if (mStartWhenReady != startedWhenReady) {
            mStartWhenReady = true;
        }
    }

    @Override
    protected void onSurfaceReady() {
        if (mStartWhenReady) {
            setStarted(true);
            mStartWhenReady = false;
        }
    }

    @Override
    public void animatedStop(Callback callback) {
        if (isStarted()) {
            if (mLines2Paint.getAlpha() != 0) {
                stop();
                if (callback != null) callback.onEnd();
            } else {
                mCallback = callback;
                setCallStop(true);
            }
        }
    }

    public void setCallStop(boolean callStop) {
        if (mCallStop != callStop) {
            mCallStop = callStop;
        }
    }

    private void startCompleteAnim() {
        initCompleteAnim();
        if (!mCompleteAnim.isRunning()) {
            mInitShaderDegree = mShaderDegree;
            mCompleteAnim.start();
        }
    }

    private void initCompleteAnim() {
        if (mCompleteAnim == null) {
            mCompleteAnim = ValueAnimator.ofFloat(0, 1);
            mCompleteAnim.setDuration(500);
            mCompleteAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            mCompleteAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    setCallStop(false);
                    if (mCallback != null) mCallback.onEnd();
                    setStarted(false);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    setCallStop(false);
                    if (mCallback != null) mCallback.onEnd();
                    setStarted(false);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            mCompleteAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = (float) animation.getAnimatedValue();
                    float shaderDegree = (-mInitShaderDegree + 450) * fraction + mInitShaderDegree;
                    rotateLinesShaderTo(shaderDegree);
                    invalidateSurface(false);
                }
            });
        }
    }

    @Override
    public void stop() {
        if (isStarted()) {
            setStarted(false);
            switchToComplete();
        }
    }

    @Override
    public void reset() {
        stop();
        switchToNormal();
    }

    @Override
    public void switchBgType(@ShaderFactory.Type int type) {
        if (mBgType != type) {
            initColorSwitchAnim();
            if (mColorSwitchAnim.isRunning()) mColorSwitchAnim.cancel();
            mTargetType = type;
            mColorSwitchAnim.start();
        }
    }

    private void initColorSwitchAnim() {
        if (mColorSwitchAnim == null) {
            mColorSwitchAnim = ValueAnimator.ofFloat(0, 1);
            mColorSwitchAnim.setDuration(500);
            mColorSwitchAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    setBgType(mTargetType);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            mColorSwitchAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    final int h = getHeight();
                    if (h != 0) {
                        mBgShader = ShaderFactory.getBgSwitchShader(mBgType, mTargetType, value, h);
                        mBgPaint.setShader(mBgShader);
                        invalidateSurface();
                    }
                }
            });
        }
    }

    @Override
    public void setBgType(@ShaderFactory.Type int type) {
        if (mBgType != type) {
            mBgType = type;
            int h = getHeight();
            if (h != 0) {
                mBgShader = ShaderFactory.getBgShader(type, h);
                mBgPaint.setShader(mBgShader);
                invalidateSurface();
            }
        }
    }

    public boolean isStarted() {
        return mStarted;
    }

    private void setStarted(boolean started) {
        if (mStarted != started) {
            mStarted = started;
            updateRunning();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = visibility == VISIBLE;
        updateRunning();
    }

    private void updateRunning() {
        boolean running = mStarted && mVisible;
        if (mRunning != running) {
            if (running) {
                setUpdate(true);
            } else {
                setUpdate(false);
            }
            mRunning = running;
        }
    }

    /**
     * Switch to complete state
     */
    private void switchToComplete() {
        mLines2Paint.setAlpha(0);
        setInscribedDegree(45);
        rotateLinesTo(45);
        rotateLinesShaderTo(90);
        invalidateSurface();
    }

    /**
     * Switch to normal state
     */
    private void switchToNormal() {
        mLines2Paint.setAlpha(255);
        setInscribedDegree(0);
        rotateLinesTo(0);
        rotateLinesShaderTo(0);
        invalidateSurface();
    }

    private OnErrorListener mOnErrorListener;

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public interface OnErrorListener {

        void onWrongSize();
    }
}
