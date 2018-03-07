package com.sciteex.ssip.sciteexmeasurementmanager.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.sciteex.ssip.sciteexmeasurementmanager.MeasurementManager;
import com.sciteex.ssip.sciteexmeasurementmanager.R;
import com.sciteex.ssip.sciteexmeasurementmanager.SQLQueryObject;
import com.sciteex.ssip.sciteexmeasurementmanager.SSIPDatabaseConnection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;

/**
 * Created by Gajos on 10/26/2017.
 */

public class DatabaseService extends IntentService
{
    private final static Handler handler = new Handler();

    private final IBinder mBinder = new DatabaseBinder();

    public class DatabaseBinder extends Binder {
        DatabaseService getService() {
            return DatabaseService.this;
        }
    }

    /**
     * Last result from database.
     */
    private String lastResult;

    public static boolean databaseConfigIsChanged = true;

    /**
     * Query list.
     */
    private static List<SQLQueryObject> queryList = new ArrayList<>();

    public static List<String> getQueryNames()
    {
        return Stream.of(queryList)
                .map(q -> q.getName())
                .toList();
    }
    /**
     * Add query to list
     */
    public static void addQuery(String title, String body)
    {
        SQLQueryObject object = databaseClient.createNewQuery(title, body);
        if(object != null)
        {
            queryList.add(object);
        }
        else
        {
            generateToast(MeasurementManager.thisActivity.getString(R.string.cannot_add_query));
        }
    }



    public final static String RUN_DATABASE_CLIENT = "run_database";

    private static SSIPDatabaseConnection databaseClient = SSIPDatabaseConnection.getSingletonInstance();

    @Override
    public void onStart(Intent intent, int startId) {
    }

    public DatabaseService()
    {
        super("DatabaseService");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String actionString = intent.getStringExtra("ACTION");
        switch(actionString)
        {
            case RUN_DATABASE_CLIENT: startDatabaseConnection();
        }
        return START_STICKY;
    }

    private static void generateToast(String toast)
    {
        handler.post(new Runnable()
        {
            public void run()
            {
                Toast.makeText(MeasurementManager.thisActivity.getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String showTable(int number)
    {
        Thread queryThread = new Thread()
        {
            public void run()
            {
                lastResult = databaseClient.showTable(queryList.get(number));
            }
        };
        queryThread.run();
        try {
            queryThread.join();
            return databaseClient.showTable(queryList.get(number));
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

    @Override
    public void onLowMemory ()
    {
    }

    @Override
    public void onDestroy()
    {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

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
                .setPriority(PRIORITY_MAX);
        mNotificationManager.notify(position, mBuilder.build());
    }

    private void startDatabaseConnection() {
        //Get from preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String dbServer = sharedPreferences.getString("database_server", getString(R.string.default_database_server));
        String dbName = sharedPreferences.getString("database_name", getString(R.string.default_database));
        String dbUser = sharedPreferences.getString("database_user", getString(R.string.default_database_user));
        String dbPassword = sharedPreferences.getString("database_password", getString(R.string.default_database_password));
        String dbType = sharedPreferences.getString("database_type",getString(R.string.default_database_type));
        //Try connect to database.
        databaseClient.setDatabaseServer(dbServer);
        databaseClient.setDatabaseName(dbName);
        databaseClient.setDatabaseUser(dbUser);
        databaseClient.setDatabasePassword(dbPassword);
        databaseClient.setDatabaseType(dbType);
        try {
            databaseClient.startSingleton();
            databaseConfigIsChanged = false;
        } catch (SQLException e) {
            makeNotification(getString(R.string.sql_error), e.getLocalizedMessage(),999);
            e.printStackTrace();
        }
    }

}
