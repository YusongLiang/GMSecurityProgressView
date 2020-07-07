package com.gome.gmsecurityprogressview.sample;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import com.gome.gmsecurityprogressview.util.Progressive;
import com.gome.gmsecurityprogressview.widget.GMSecurityProgressView;

public class MainActivity extends BaseActivity {

    private Button btStartOrPause;
    private GMSecurityProgressView progressView;
    private NumberPicker pickerSetColor;
    private String[] mColors = {"BLUE", "GREEN", "ORANGE", "RED"};
    private NumberPicker pickerSwitchColor;
    private int mCheckedValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btStartOrPause = (Button) findViewById(R.id.bt_start_or_pause);
        progressView = (GMSecurityProgressView) findViewById(R.id.progress_view);
        pickerSetColor = (NumberPicker) findViewById(R.id.picker_set_color);
        pickerSwitchColor = (NumberPicker) findViewById(R.id.picker_switch_color);
        initPicker();
        btStartOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (progressView.isStarted()) {
                    progressView.animatedStop(new Progressive.Callback() {
                        @Override
                        public void onEnd() {
                            btStartOrPause.setText(R.string.start);
                        }
                    });
                } else {
                    progressView.reset();
                    progressView.start();
                    btStartOrPause.setText(R.string.stop);
                }
            }
        });
        btStartOrPause.callOnClick();
    }

    private void initPicker() {
        pickerSetColor.setMinValue(0);
        pickerSetColor.setMaxValue(3);
        pickerSetColor.setDisplayedValues(mColors);
        pickerSetColor.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setCheckedValue(newVal);
            }
        });
        pickerSwitchColor.setMinValue(0);
        pickerSwitchColor.setMaxValue(3);
        pickerSwitchColor.setDisplayedValues(mColors);
        pickerSwitchColor.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setCheckedValue(newVal);
            }
        });
    }

    public void setCheckedValue(int value) {
        if (mCheckedValue != value) {
            mCheckedValue = value;
            if (pickerSwitchColor.getValue() != value) {
                progressView.setBgType(value);
                pickerSwitchColor.setValue(value);
            }
            if (pickerSetColor.getValue() != value) {
                progressView.switchBgType(value);
                pickerSetColor.setValue(value);
            }
        }
    }
}
