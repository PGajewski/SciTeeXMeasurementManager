package com.sciteex.ssip.sciteexmeasurementmanager.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.sciteex.ssip.sciteexmeasurementmanager.R;

public class ButtonActionService extends Service {
    public ButtonActionService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LinearLayout mLinear = new LinearLayout(getApplicationContext()) {

            //home or recent button
            public void onCloseSystemDialogs(String reason) {
                if ("globalactions".equals(reason)) {
                    Log.i("Key", "Long press on power button");
                } else if ("homekey".equals(reason)) {
                    //home key pressed
                } else if ("recentapps".equals(reason)) {
                    // recent apps button clicked
                }
            }

            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if(event.getKeyCode() == KeyEvent.KEYCODE_POWER)
                {
                    Intent serviceIntent = new Intent(ButtonActionService.this, UserInactiveService.class);
                    serviceIntent.putExtra("ACTION", UserInactiveService.RESET_TIMER);
                    serviceIntent.putExtra("SCREEN_SHUTDOWN", true);
                    stopService(serviceIntent);
                    startService(serviceIntent);
                }
                return super.dispatchKeyEvent(event);
            }

        };

        mLinear.setFocusable(true);

        View mView = LayoutInflater.from(this).inflate(R.layout.service_layout, mLinear);

        /*
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        //params
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                100,
                100,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                ,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        wm.addView(mView, params);
        */
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
