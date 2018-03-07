package com.sciteex.ssip.sciteexmeasurementmanager;

import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by Gajos on 2/26/2018.
 */

public class DisplayColorChanger {

    SharedPreferences prefs;

    private static final Paint paint = new Paint();

    private static final float redArray[] =  { 1, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 1, 0};

    private static final float greenArray[] =  { 0, 0, 0, 0, 0,
            0, 1, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 1, 0};
    private static ColorMatrix redArrayCM = new ColorMatrix(redArray);
    private static ColorMatrix greenArrayCM = new ColorMatrix(greenArray);

    public static void setNoColor(View view)
    {
        view.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    public static void setRed(View view)
    {
        paint.setColorFilter(new ColorMatrixColorFilter(redArrayCM));
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    public static void setGreen(View view)
    {
        paint.setColorFilter(new ColorMatrixColorFilter(greenArrayCM));
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }
}
