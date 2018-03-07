package com.sciteex.ssip.sciteexmeasurementmanager.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import com.sciteex.ssip.sciteexmeasurementmanager.DisplayColorChanger;
import com.sciteex.ssip.sciteexmeasurementmanager.JNIOpcUaClient;
import com.sciteex.ssip.sciteexmeasurementmanager.MeasurementManager;
import com.sciteex.ssip.sciteexmeasurementmanager.R;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;


/**
 * Created by Gajos on 2/27/2018.
 */

public class EventFilterService extends IntentService {

    public static View actualView;

    public final static String RUN_ALERT_SERVICE = "run_alert_filter";

    public final static String RUN_INFO_SERVICE = "run_info_filter";

    public final static String TEST_ALERT = "test_alert";

    public final static String RUN_ALERT_AND_INFO_SERVICE = "run_alert_and_info_filters";

    public final static int VIBRATE_TIME = 200;

    public final static int PULSE_TIME = 500;

    public final static int CHECKING_INTERVAL = 500;

    public final static int ACTIVE_BRIGHTNESS = 100;

    public final static int INACTIVE_BRIGHTNESS = 50;

    public static boolean alertConfigIsChanged = true;

    public static boolean infoConfigIsChanged = true;

    private JNIOpcUaClient opcUaClient = JNIOpcUaClient.getSingletonInstance();

    private Thread alertThread = null;

    private Thread infoThread = null;

    private final static int ALERT_NOTIFICATION_NUMBER = 900;

    private final static int INFO_NOTIFICATION_NUMBER = 901;

    private Vibrator vibration = null;

    private static boolean isAlert = false;

    private Ringtone ring = null;

    @Override
    public void onStart(Intent intent, int startId) {
    }

    public EventFilterService() {
        super("EventFilterService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String actionString = intent.getStringExtra("ACTION");
        if(vibration == null)
            vibration = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if(ring == null)
            try {
                Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                ring = RingtoneManager.getRingtone(getApplicationContext(), alert);
            } catch (Exception e) {
                e.printStackTrace();
            }
        switch (actionString) {
            case RUN_ALERT_SERVICE:
                startAlertFilterUaClient(); break;
            case RUN_INFO_SERVICE:
                startInfoFilterUaClient(); break;
            case RUN_ALERT_AND_INFO_SERVICE:
                startAlertFilterUaClient();
                startInfoFilterUaClient(); break;
            case TEST_ALERT:
                testAlert(); break;
        }
        return START_STICKY;
    }

    @Override
    public void onLowMemory() {
        if(alertThread != null)
        {
            alertThread.interrupt();
        }

        if(infoThread != null)
        {
            infoThread.interrupt();
        }
    }

    @Override
    public void onDestroy() {
        if(alertThread != null)
        {
            alertThread.interrupt();
        }

        if(infoThread != null)
        {
            infoThread.interrupt();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    private void startAlertFilterUaClient() {
        //Get from preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        int alertNode = Integer.parseInt(sharedPreferences.getString("alert_node", getString(R.string.default_alert_node)));
        String alertNodePath= sharedPreferences.getString("alert_path", getString(R.string.default_alert_path));
        String alertMessage = sharedPreferences.getString("alert_message", getString(R.string.default_alert_message));

        if(alertThread != null)
        {
            alertThread.interrupt();
        }

        //Init alert variable.

            alertThread = new Thread() {
                @Override
                public void run() {

                    //Prepare vibrations.
                    boolean lastEventWasAlert = false;
                    //Init alert variable.
                    if(opcUaClient.initAlertVariable(alertNode, alertNodePath))
                    {
                        while (!isInterrupted()) {
                            int actualBrightness = getBrightness();
                            if(opcUaClient.readAlert() && opcUaClient.checkAlertValue())
                            {
                                if(lastEventWasAlert == false)
                                {
                                    lastEventWasAlert = true;
                                    makeNotification(getString(R.string.alert),alertMessage, ALERT_NOTIFICATION_NUMBER);
                                }
                                MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        DisplayColorChanger.setRed(actualView);
                                        changeBrightness(ACTIVE_BRIGHTNESS);
                                    }
                                });
                                if(vibration != null)
                                {
                                    vibration.vibrate(VIBRATE_TIME);
                                }
                                if(ring != null)
                                {
                                    ring.play();
                                }
                                try {
                                    Thread.sleep(PULSE_TIME);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        DisplayColorChanger.setNoColor(actualView);
                                        changeBrightness(INACTIVE_BRIGHTNESS);
                                    }
                                });
                                try {
                                    Thread.sleep(PULSE_TIME);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                            {
                                dismissNotification(ALERT_NOTIFICATION_NUMBER);
                                MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        changeBrightness(actualBrightness);
                                    }
                                });
                            }
                            try {
                                Thread.sleep(CHECKING_INTERVAL);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        }
                    else
                    {
                        makeNotification(getString(R.string.alert_error),getString(R.string.cannot_load_variable),ALERT_NOTIFICATION_NUMBER);
                    }
                }
            };
            alertThread.start();

    }


    private void startInfoFilterUaClient() {
        //Get from preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        int infoNode = Integer.parseInt(sharedPreferences.getString("info_node", getString(R.string.default_info_node)));
        String infoNodePath= sharedPreferences.getString("info_path", getString(R.string.default_info_path));
        String infoMessage = sharedPreferences.getString("info_message", getString(R.string.default_info_message));

        if(infoThread != null)
        {
            infoThread.interrupt();
        }

        //Init alert variable.

            infoThread = new Thread() {
                @Override
                public void run() {

                    //Prepare vibrations.
                    boolean lastEventWasInfo = false;
                    if(opcUaClient.initInfoVariable(infoNode, infoNodePath))
                    {
                        while (!isInterrupted()) {
                            int actualBrightness = getBrightness();

                            //Because alert is more important than info, this part work only if alert is not active.
                                if(opcUaClient.readInfo() && opcUaClient.checkInfoValue())
                                {
                                    if(lastEventWasInfo == false)
                                    {
                                        lastEventWasInfo = true;
                                        makeNotification(getString(R.string.info),infoMessage, INFO_NOTIFICATION_NUMBER);
                                    }
                                    if(!isAlert) {
                                        MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                                            public void run() {
                                                DisplayColorChanger.setGreen(actualView);
                                                changeBrightness(ACTIVE_BRIGHTNESS);
                                            }
                                        });
                                        if (vibration != null) {
                                            vibration.vibrate(VIBRATE_TIME);
                                        }
                                        if (ring != null) {
                                            ring.play();
                                        }
                                        try {
                                            Thread.sleep(PULSE_TIME);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                                            public void run() {
                                                DisplayColorChanger.setNoColor(actualView);
                                                changeBrightness(INACTIVE_BRIGHTNESS);
                                            }
                                        });
                                        try {
                                            Thread.sleep(PULSE_TIME);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                else
                                {
                                    dismissNotification(INFO_NOTIFICATION_NUMBER);
                                    MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            changeBrightness(actualBrightness);
                                        }
                                    });
                                }
                                try {
                                    Thread.sleep(CHECKING_INTERVAL);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                        }
                    }
                    else
                    {
                        makeNotification(getString(R.string.info_error),getString(R.string.cannot_load_variable),INFO_NOTIFICATION_NUMBER);
                    }
                }
            };
            infoThread.start();
    }


    private void makeNotification(String title, String content, int position)
    {
        NotificationManager mNotificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.sciteex_logo_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.sciteex_logo_icon))
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(true)
                .setPriority(PRIORITY_MAX);
        mNotificationManager.notify(position, mBuilder.build());
    }

    private void dismissNotification(int position)
    {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(position);
    }

    private void changeBrightness(int bright)
    {
        android.provider.Settings.System.putInt(this.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS, bright);
    }

    private int getBrightness()
    {
        int brightness = 0;
        try {
            brightness =
                    Settings.System.getInt(
                            getApplicationContext().getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return brightness;
    }

    private void testAlert() {

        makeNotification(getString(R.string.info), "Test message", INFO_NOTIFICATION_NUMBER);
        final int actualBrightness = getBrightness();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            DisplayColorChanger.setGreen(actualView);
                            changeBrightness(ACTIVE_BRIGHTNESS);
                    }
                    });
                    vibration.vibrate(VIBRATE_TIME);
                    if(ring != null)
                    {
                        ring.play();
                    }
                    try {
                        Thread.sleep(PULSE_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            DisplayColorChanger.setNoColor(actualView);
                            changeBrightness(INACTIVE_BRIGHTNESS);
                        }
                    });
                    try {
                        Thread.sleep(PULSE_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            DisplayColorChanger.setRed(actualView);
                            changeBrightness(ACTIVE_BRIGHTNESS);
                        }
                    });
                    vibration.vibrate(VIBRATE_TIME);
                    if(ring != null)
                    {
                        ring.play();
                    }
                    try {
                        Thread.sleep(PULSE_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            DisplayColorChanger.setNoColor(actualView);
                            changeBrightness(INACTIVE_BRIGHTNESS);
                        }
                    });
                    try {
                        Thread.sleep(PULSE_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
    }
}