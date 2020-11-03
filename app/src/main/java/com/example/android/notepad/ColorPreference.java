// ColorPreference.java
package com.example.android.notepad;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Color;

public class ColorPreference extends DialogPreference {

    private int mColor = Color.BLUE;
    private ColorPickerView mColorPickerView;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.color_picker_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mColorPickerView = (ColorPickerView) view.findViewById(R.id.color_picker);
        mColorPickerView.setColor(mColor);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mColor = mColorPickerView.getColor();
            persistInt(mColor);
            notifyChanged();
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mColor = getPersistedInt(mColor);
        } else {
            mColor = (Integer) defaultValue;
            persistInt(mColor);
        }
    }
}
