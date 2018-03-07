package com.sciteex.ssip.sciteexmeasurementmanager.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.sciteex.ssip.sciteexmeasurementmanager.LoginActivity;

public class UserInactiveService extends IntentService {

    @Override
    public void onStart(Intent intent, int startId) {
    }

    public static long delayTime;

    public static final String RUN_TIMER = "run_timer";

    public static final String RESET_TIMER = "reset_timer";

    public static final String STOP_TIMER = "stop_timer";

    public static final String GO_WITHOUT_TIMER = "go_without_timer";

    public static final String SET_INACTIVE_TIME = "set_inactive_time";

    public UserInactiveService()
    {
        super("UserInactiveService");
    }

    public static boolean isTimerActive = true;

    private static boolean shutdownScreen = false;

    public static void setInactiveTime(long seconds)
    {
        delayTime = seconds * 1000;
    }

    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            UserInactiveService.this.goToLoginPage();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String actionString = intent.getStringExtra("ACTION");
        shutdownScreen = intent.getBooleanExtra("SCREEN_SHUTDOWN", false);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int time = Integer.parseInt(sharedPreferences.getString("inactive_time_list", "-1"));
        if(time < 0)
        {
            isTimerActive = false;
        }
        else
        {
            isTimerActive = true;
            setInactiveTime(time);
        }
        switch(actionString)
        {
            case RUN_TIMER: startTimer(); break;
            case RESET_TIMER: resetTimer(); break;
            case STOP_TIMER: stopTimer(); break;
            case GO_WITHOUT_TIMER: goToLoginPage(); break;
        }
        return START_STICKY;
    }

    private void goToLoginPage() {
        stopTimer();
        if(!LoginActivity.isLoginActive) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            if(shutdownScreen)
            {
                loginIntent.putExtra("SCREEN_SHUTDOWN",true);
            }
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
        }
    }

    @Override
    public void onLowMemory ()
    {
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onDestroy()
    {
        timerHandler.removeCallbacks(timerRunnable);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    private void startTimer()
    {
        if(isTimerActive)
        {
            timerHandler.postDelayed(timerRunnable, delayTime);
        }
    }

    private void stopTimer()
    {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void resetTimer()
    {
        stopTimer();
        startTimer();
    }
}
