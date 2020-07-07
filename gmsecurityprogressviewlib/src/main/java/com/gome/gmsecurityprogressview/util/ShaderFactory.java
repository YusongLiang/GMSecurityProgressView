package com.gome.gmsecurityprogressview.util;

import android.animation.ArgbEvaluator;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Felix.Liang
 */
@SuppressWarnings("WeakerAccess")
public class ShaderFactory {

    public static final int BLUE = 0;
    public static final int GREEN = 1;
    public static final int ORANGE = 2;
    public static final int RED = 3;
    private static final int[] sBgColorsStart = {0xFF0482BE, 0xFF009EAE, 0xFFF05623, 0xFFD42A3B};
    private static final int[] sBgColorsEnd = {0xFF01AACE, 0xFF3EC091, 0xFFF59B55, 0xFFE46B5D};
    private static ArgbEvaluator sArgbEvaluator = new ArgbEvaluator();
    private static int[] sLineColors = {0x00FFFFFF, 0xFFFFFFFF, 0x00FFFFFF};
    private static float[] sLineColorPos = {0.1f, 0.42f, 0.74f};
    private static Matrix sMatrix = new Matrix();
    private static SweepGradient sSweepGradient = new SweepGradient(0, 0, sLineColors, sLineColorPos);

    public static LinearGradient getBgShader(@Type int type, float length) {
        return new LinearGradient(0, 0, 0, length, sBgColorsStart[type], sBgColorsEnd[type], Shader.TileMode.CLAMP);
    }

    public static synchronized LinearGradient getBgSwitchShader(int from, int to, float fraction, float length) {
        final int start = (int) sArgbEvaluator.evaluate(fraction, sBgColorsStart[from], sBgColorsStart[to]);
        final int end = (int) sArgbEvaluator.evaluate(fraction, sBgColorsEnd[from], sBgColorsEnd[to]);
        return new LinearGradient(0, 0, 0, length, start, end, Shader.TileMode.CLAMP);
    }

    public static SweepGradient getLinesShader(float degree) {
        sMatrix.reset();
        sMatrix.setRotate(degree);
        sSweepGradient.setLocalMatrix(sMatrix);
        return sSweepGradient;
    }

    @IntDef({RED, ORANGE, BLUE, GREEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}
}
