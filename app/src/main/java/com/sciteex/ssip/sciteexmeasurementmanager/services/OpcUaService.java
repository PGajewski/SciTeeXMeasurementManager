package com.sciteex.ssip.sciteexmeasurementmanager.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.sciteex.ssip.sciteexmeasurementmanager.JNIOpcUaClient;
import com.sciteex.ssip.sciteexmeasurementmanager.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Gajos on 10/26/2017.
 */

public class OpcUaService extends IntentService
{

    /////////////////Patterns/////////////////
    private Pattern messagePattern = Pattern.compile("\\(.*\\)");
    private Pattern opcAddressPattern = Pattern.compile("\\[[0-9]*\\,.*\\]");


    public final static String RUN_OPC_UA_CLIENT = "run_opc";

    private JNIOpcUaClient opcUaClient = JNIOpcUaClient.getSingletonInstance();

    @Override
    public void onStart(Intent intent, int startId) {
    }

    public OpcUaService()
    {
        super("OPCUAService");
    }

    public static boolean opcUaConfigIsChanged = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String actionString = intent.getStringExtra("ACTION");
        switch(actionString)
        {
            case RUN_OPC_UA_CLIENT: startOpcUaClient();
        }
        return START_STICKY;
    }

    @Override
    public void onLowMemory ()
    {
        opcUaClient.interrupt();
    }

    @Override
    public void onDestroy()
    {
        opcUaClient.interrupt();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }



    private void startOpcUaClient() {
        //Get from preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String variablesField = sharedPreferences.getString("opc_ua_variables_field", getString(R.string.variable_def_template));

        opcUaClient.setEndpoint(sharedPreferences.getString("opc_ua_endpoint", getString(R.string.default_opc_ua_endpoint)))
                .setUser(sharedPreferences.getString("opc_ua_user", getString(R.string.default_opc_ua_user)))
                .setPassword(sharedPreferences.getString("opc_ua_password", getString(R.string.default_database_password)));

        //if message in field is default message, don't do nothing.
        if (getString(R.string.variable_def_template).equals(variablesField))
            return;

        //Temporary lists.
        List<Integer> tempNodes = new ArrayList<>();
        List<String> tempPaths = new ArrayList<>();
        List<String> tempTypes = new ArrayList<>();
        List<String> tempMessages = new ArrayList<>();


        //Create list with data for variables.
        //Split by enters.
        String[] variablesInString = variablesField
                .split("\n");

        for (String variableLine : variablesInString) {

            //Get pattern from node and path.
            Matcher opcUaAdressMatcher = opcAddressPattern.matcher(variableLine);
            if (!opcUaAdressMatcher.find()) {
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_template_icon_bg)
                        .setContentTitle(getString(R.string.invalid_opc_ua_path))
                        .setContentText(getString(R.string.in_line) + tempTypes.size())
                        .notify();
            } else {
                //Get variable type.
                try {
                    tempTypes.add(variableLine.substring(0, variableLine.indexOf('[')));
                } catch (Exception e) {
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.notification_template_icon_bg)
                            .setContentTitle(getString(R.string.invalid_opc_ua_path))
                            .setContentText(getString(R.string.in_line) + tempTypes.size())
                            .notify();
                }
                String[] opcUaAdress = opcUaAdressMatcher.group().split(",");
                tempNodes.add(Integer.parseInt(opcUaAdress[0].substring(1, opcUaAdress[0].length())));
                tempPaths.add(opcUaAdress[1].substring(0, opcUaAdress[1].length() - 1));

                //Get message from line.
                Matcher messageMatcher = messagePattern.matcher(variableLine);
                if (messageMatcher.find()) {
                    String message = messageMatcher.group();
                    tempMessages.add(message.substring(1, message.length() - 1));
                }
            }
        }

        //Restart opcUaClient.
        opcUaClient.interrupt();


        //Add lists to JNI OPC UA client.
        opcUaClient.setNodes(tempNodes)
                .setVariablePaths(tempPaths)
                .setVariableTypes(tempTypes)
                .setMessages(tempMessages);

        //Start connections in background.
        JNIOpcUaClient.startSingleton();

        //Set change variable.
        opcUaConfigIsChanged = false;
    }
}
