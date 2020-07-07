package com.gome.gmsecurityprogressview.util;

/**
 * @author Felix.Liang
 */
public interface Progressive {

    void start();

    void animatedStop(Callback callback);

    void stop();

    void reset();

    void switchBgType(@ShaderFactory.Type int type);

    void setBgType(@ShaderFactory.Type int type);

    interface Callback {

        void onEnd();
    }
}
