package com.sciteex.ssip.sciteexmeasurementmanager;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;

/**
 * JNI class of OPC UA client.
 */

public class JNIOpcUaClient extends Thread{


    private JNIOpcUaClient()
    {
        if(cInitClientList())
        {
            System.out.print("Good init");
        }
        System.out.print("Bad init");

    }

    public void finalize()
    {
        cDeleteClientList();
        //this.cCleanAlertVariable();
        //this.cCleanInfoVariable();
        this.cCleanLoginVariable();
    }

    public static JNIOpcUaClient getSingletonInstance()
    {
        return singletonInstance;
    }

    public static void startSingleton()
    {
        singletonInstance.start();
    }

    /**
     * Handler to main foop
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            // This is where you do your work in the UI thread.
            // Your worker tells you in the message what to do.
        }
    };

    /**
     * Size of native array with clients definitions.
     */
    public int nativeSize;

    /**
     * List of threads to watch variable in OPC UA server.
     */
    private List<OpcUaVariableGetter> watchThreadsList = new ArrayList<>();


    /**
     * Endpoint of OPC UA server.
     */
    private String endpoint;

    /**
     * User of OPC UA server.
     */
    private String user;

    /**
     * Password to OPC UA server.
     */
    private String password;

    /**
     * List with nodes of variables.
     */
    private List<Integer> nodes;

    /**
     * List of paths to variable.
     */
    private List<String> variablePaths;

    /**
     * List of variables types.
     */
    private List<String> variableTypes;

    /**
     * List with messages to display.
     */
    private List<String> messagesList;

    ////////////Setters.
    public JNIOpcUaClient setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
        return this;
    }

    public JNIOpcUaClient setUser(String user)
    {
        this.user = user;
        return this;
    }

    public JNIOpcUaClient setPassword(String password)
    {
        this.password = password;
        return this;
    }

    public JNIOpcUaClient setNodes(List<Integer> nodes)
    {
        this.nodes = nodes;
        return this;
    }

    public JNIOpcUaClient setVariablePaths(List<String> variablePaths)
    {
        this.variablePaths = variablePaths;
        return this;
    }

    public JNIOpcUaClient setVariableTypes(List<String> variableTypes)
    {
        this.variableTypes = variableTypes;
        return this;
    }

    public JNIOpcUaClient setMessages(List<String> messagesList)
    {
        this.messagesList = messagesList;
        return this;
    }

    ////////////Getters.
    public String getEndpoint()
    {
        return this.endpoint;
    }

    public String getUser()
    {
        return this.user;
    }

    public String getPassword()
    {
        return this.password;
    }

    public List<Integer> getNodes()
    {
        return this.nodes;
    }

    public List<String> getVariablePaths()
    {
        return this.variablePaths;
    }

    public List<String> getVariableTypes()
    {
        return this.variableTypes;
    }

    ///////////////Native methods.
    static {
        System.loadLibrary("opcuaclient");
    }

    private static JNIOpcUaClient singletonInstance = new JNIOpcUaClient();

    private native void cCreateClients(int size);

    //Getters
    private native void cSetEndpoint(int position, String endpoints);

    private native void cSetNode(int position, int nodeID, String nodeNames);

    private native void cSetVariablesType(int position, String variableNames);

    private native boolean cConnect(int position);

    private native boolean cGetVariableBoolean(int position);

    private native float cGetVariableFloat(int position);

    private native String cGetVariableString(int position);

    private native int cGetVariableInteger(int position);

    private native long cGetVariableLong(int position);

    //Setters
    private native void cSetVariableBoolean(int position, boolean value);

    private native void cSetVariableFloat(int position, float value);

    private native void cSetVariableString(int position, String value);

    private native void cSetVariableInteger(int position, int value);

    private native long cSetVariableLong(int position, long value);

    private native boolean cRead(int position);

    private native boolean cWrite(int position);

    private native void cClearMemory(int size);

    private native int cCheckReadStatus(int position);

    private native boolean cInitElcometerArrayWriting(String endpoint, int node, String nodeNames);

    private native boolean cInitElcometerWriting(String endpoint, int node, String nodeNames);

    private native boolean cWriteToElcometerArray(float[] measures, int size);

    private native boolean cWriteToActualElcometer(float value);

    private native void cClearElcometerArrayClient();

    private native void cClearActualElcometerClient();

    private native int cGetVariableStatus(int position);

    private native void cAddUndefiniedIntVariable(int position);

    private native void cAddUndefiniedBoolVariable(int position);

    private native void cAddUndefiniedFloatVariable(int position);

    private native int cGetUndefiniedIntVariable(int position);

    private native boolean cGetUndefiniedBoolVariable(int position);

    private native float cGetUndefiniedFloatVariable(int position);

    private native boolean cReadUndefiniedVariable(int position);

    private native boolean cConnectWithUndefiniedVariable(String endpoint, int node, String nodeName, int position);

    private native void cCleanUndefiniedVariable(int position);

    private native boolean  cInitClientList();

    private native void cDeleteClientList();

    private native boolean cInitAlertVariableReading(String endpoint, int node, String nodeName);

    private native boolean cReadAlertVariable();

    private native boolean cGetAlertValue();

    private native void cCleanAlertVariable();

    private native boolean cInitInfoVariableReading(String endpoint, int node, String nodeName);

    private native boolean cReadInfoVariable();

    private native boolean cGetInfoValue();

    private native void cCleanInfoVariable();

    private native boolean cInitScanVariableWriting(String endpoint, int node, String nodeName);

    private native boolean cWriteScanVariable();

    private native void cSetScanVariable(String scanString);

    private native void cCleanScanVariable();

    private native boolean cInitLoginVariableWriting(String endpoint, int node, String nodeName);

    private native boolean cWriteLoginVariable();

    private native void cSetLoginVariable(String login);

    private native void cCleanLoginVariable();
    /**
     * Method to init connection to OPC UA Server
     */
    public JNIOpcUaClient connect() throws NullPointerException
    {

        if(endpoint.isEmpty() || "".equals(endpoint)) {
            return null;
        }

        //Set nativeSize.
        this.nativeSize = variablePaths.size();

        //Create clients.
        cCreateClients(nativeSize);

        for(int i = 0; i < nativeSize; ++i)
        {
            if(isInterrupted())
                return null;

            cSetEndpoint(i, endpoint);
            cSetVariablesType(i, variableTypes.get(i));
            cSetNode(i, nodes.get(i), variablePaths.get(i));
        }

        for(int i = 0; i < nativeSize; ++i) {
            if(isInterrupted())
                return null;
            while(!cConnect(i))
            {
                try
                {
                    Thread.sleep(1000);

                }catch(InterruptedException e)
                {
                    e.printStackTrace();
                }
                String message = MeasurementManager.applicationContext.getString(R.string.cannot_connect);
                makeNotification(message,"Server: " + endpoint, i);
                nativeSize = 0;
            }
        }

        return this;
    }

    public JNIOpcUaClient watch() {
        //Create connection asynctasks.
        for (int i = 0; i < nativeSize; ++i) {
            if (isInterrupted())
                return null;
            if (nodes.get(i) != null && variableTypes.get(i) != null && variablePaths.get(i) != null && !"".equals(variablePaths.get(i))) {
                try {
                    switch (variableTypes.get(i)) {
                        case "bool":
                            watchThreadsList.add(new OpcUaVariableGetter<Boolean>(i, messagesList.get(i),new Boolean(false)));
                            break;
                        case "int":
                            watchThreadsList.add(new OpcUaVariableGetter<Integer>(i, messagesList.get(i), new Integer(0)));
                            break;
                        case "long":
                            watchThreadsList.add(new OpcUaVariableGetter<Long>(i, messagesList.get(i), new Long(0)));
                            break;
                        case "string":
                            watchThreadsList.add(new OpcUaVariableGetter<String>(i, messagesList.get(i), new String("")));
                            break;
                        case "float":
                            watchThreadsList.add(new OpcUaVariableGetter<Float>(i, messagesList.get(i), new Float(0.0f)));
                            break;
                        default:
                            makeNotification(MeasurementManager.applicationContext.getString(R.string.error), MeasurementManager.applicationContext.getString(R.string.variable_type_fault), i);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
        }

        Stream.of(watchThreadsList)
                .forEach(t -> t.start());
        return this;
    }


    public void disconnect()
    {
        //Stop all watch threads.
        Stream.of(watchThreadsList)
                .forEach(t ->{
                    if(!t.isInterrupted())
                        t.interrupt();
                });
        //TODO: Init disconnection
        this.cClearMemory(nativeSize);
        this.cClearElcometerArrayClient();
        this.watchThreadsList.clear();
    }

    public <T> void write(int position, T variable)
    {
        if(!this.isInterrupted()) {
            //Interrupt getter of this variable
            OpcUaVariableSetter<T> setter = new OpcUaVariableSetter<T>(position, variable);
            setter.run();
        }
    }

    public boolean writeElcometerArray(String endpoint, int node, String nodeName, float[] array)
    {
        if(!cInitElcometerArrayWriting(endpoint, node, nodeName))
            return false;
        if(!cWriteToElcometerArray(array, array.length))
            return false;
        return true;
    }

    /**
     * Attlication to send notifications.
     * @param title
     * @param content
     */
    private static void makeNotification(String title, String content, int position)
    {
            NotificationManager mNotificationManager =
                    (NotificationManager) MeasurementManager.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MeasurementManager.applicationContext)
                    .setSmallIcon(R.mipmap.sciteex_logo_icon)
                    .setLargeIcon(BitmapFactory.decodeResource(MeasurementManager.applicationContext.getResources(),
                            R.mipmap.sciteex_logo_icon))
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(PRIORITY_MAX);
            mNotificationManager.notify(position, mBuilder.build());
        }

    private static void makeToast(String message)
    {
        if(MeasurementManager.thisActivity != null) {
            MeasurementManager.thisActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MeasurementManager.thisActivity, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public synchronized void run() {
        //Wait for cancelled all watch threads.
        boolean isStillWorking = true;
        do {
            isStillWorking = false;
            for (Thread task : watchThreadsList) {
                if(!task.isInterrupted())
                {
                    isStillWorking = true;
                }
            }
        }
        while(isStillWorking);

        try {
            connect().watch();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void interrupt() {
        try {
            this.disconnect();
        }catch (NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    public List<Object> getActualVariables()
    {
        return Stream.of(watchThreadsList)
                .map(t -> t.getVariable())
                .collect(Collectors.toList());
    }

    public boolean writeElcometerActualValue(String endpoint, int node, String nodeName, float actualValue) {
        if(!cInitElcometerWriting(endpoint, node, nodeName))
            return false;
        if(!cWriteToActualElcometer(actualValue))
            return false;
        return true;
    }

    public class OpcUaVariableGetter<T> extends Thread
    {
        //
        private boolean isConnected;

        //Variable of getter.
        private T variable;

        //Position of client in clients array.
        private int position;

        /**
         * Message sended by notification, when wariable is changed.
         */
        private String message = "Variable was changed!";

        /**
         * Method to set message to display by notification.
         * @param message
         */
        public void setMessage(String message)
        {
            this.message = message;
        }

        public void setConnectionFlag(boolean isConnected)
        {
            this.isConnected = isConnected;
        }

        public boolean getConnectionFlag()
        {
            return this.isConnected;
        }

        public OpcUaVariableGetter(int position, T variable)
        {
            this.variable = variable;
            this.position = position;
        }

        public OpcUaVariableGetter(int position, String message, T variable)
        {

            this(position, variable);
            this.message = message;
        }

        /**
         * Method to get variable from getter.
         * @return variable
         */
        synchronized public T getVariable()
        {
            return variable;
        }

        public void run() {

            //Flag to block to much messages on screen.
            //Begin of program - simulated error.
            boolean lastStateWasError = true;
            if (variable instanceof java.lang.Boolean)
                while (!this.isInterrupted()) {
                    try {
                        this.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (cRead(position)) {
                        Boolean newVariable = cGetVariableBoolean(position);
                        if (newVariable != null && (!variable.equals(newVariable) || lastStateWasError)) {
                            variable = (T) newVariable;
                            lastStateWasError = false;
                            makeNotification(message,
                                    //"(" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                            "Value: " + variable.toString(),
                                            position);
                        }
                    } else {
                        if (!lastStateWasError) {
                            makeNotification(MeasurementManager.applicationContext.getApplicationContext().getString(R.string.error),
                                    MeasurementManager.applicationContext.getApplicationContext().getString(R.string.cannot_load_variable) + " (" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + ")",
                                    position);
                            lastStateWasError = true;
                        }
                    }
                }
            else if (variable instanceof java.lang.Integer)
                while (!this.isInterrupted()) {
                    try {
                        this.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int check = cCheckReadStatus(position);
                    if (cRead(position)) {
                        Integer newVariable = cGetVariableInteger(position);
                        if (newVariable != null && (!variable.equals(newVariable) || lastStateWasError)) {
                            variable = (T) newVariable;
                            lastStateWasError = false;
                            makeNotification(message,
                                    //"(" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                    "Value: " + variable.toString(),
                                    position);
                        }
                    } else {
                        if (!lastStateWasError) {
                            makeNotification(MeasurementManager.applicationContext.getApplicationContext().getString(R.string.error),
                                    MeasurementManager.applicationContext.getApplicationContext().getString(R.string.cannot_load_variable) + " (" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + ")",
                                    position);
                            lastStateWasError = true;
                        }
                    }
                }
            else if (variable instanceof java.lang.Long)
                while (!this.isInterrupted()) {
                    try {
                        this.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int check = cCheckReadStatus(position);
                    if (cRead(position)) {
                        Long newVariable = cGetVariableLong(position);
                        if (newVariable != null && (!variable.equals(newVariable) || lastStateWasError)) {
                            variable = (T) newVariable;
                            lastStateWasError = false;
                            makeNotification(message,
                                    //"(" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                    "Value: " + variable.toString(),
                                    position);
                        }
                    } else {
                        if (!lastStateWasError) {
                            makeNotification(MeasurementManager.applicationContext.getApplicationContext().getString(R.string.error),
                                    MeasurementManager.applicationContext.getApplicationContext().getString(R.string.cannot_load_variable) + " (" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + ")",
                                    position);
                            lastStateWasError = true;
                        }
                    }
                }
            else if (variable instanceof java.lang.String)
                while (!this.isInterrupted()) {
                    try {
                        this.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int check = cCheckReadStatus(position);
                    if (cRead(position)) {
                        String newVariable = cGetVariableString(position);
                        if (newVariable != null && (!variable.equals(newVariable) || lastStateWasError)) {
                            variable = (T) newVariable;
                            lastStateWasError = false;
                            makeNotification(message,
                                    //"(" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                    "Value: " + variable.toString(),
                                    position);
                        }
                    } else {
                        if (!lastStateWasError) {
                            makeNotification(MeasurementManager.applicationContext.getApplicationContext().getString(R.string.error),
                                    MeasurementManager.applicationContext.getApplicationContext().getString(R.string.cannot_load_variable) + " (" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + ")",
                                    position);
                            lastStateWasError = true;
                        }
                    }
                }
            else if (variable instanceof java.lang.Float)
                while (!this.isInterrupted()) {
                    try {
                        this.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int check = cCheckReadStatus(position);
                    if (cRead(position)) {
                        Float newVariable = cGetVariableFloat(position);
                        if (newVariable != null && (!variable.equals(newVariable) || lastStateWasError)) {
                            variable = (T) newVariable;
                            lastStateWasError = false;
                            makeNotification(message,
                                    //"(" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                    "Value: " + variable.toString(),
                                    position);
                        }
                    } else {
                        if (!lastStateWasError) {
                            makeNotification(MeasurementManager.applicationContext.getApplicationContext().getString(R.string.error),
                                    MeasurementManager.applicationContext.getApplicationContext().getString(R.string.cannot_load_variable) + ": (" + JNIOpcUaClient.this.getNodes().get(position).toString() + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + ")",
                                    position);
                            lastStateWasError = true;
                        }
                    }
                }
        }

        @Override
        public Object clone()
        {
            return new OpcUaVariableGetter<T>(position, message, variable);
        }
    }


    public class OpcUaVariableSetter<T> extends Thread {
        //Variable of getter.
        private T variable;

        //Position of client in clients array.
        private int position;

        public OpcUaVariableSetter(int position, T variable) {
            this.variable = variable;
            this.position = position;
        }

        /**
         * Method to set variable to send.
         *
         * @param variable
         * */
        public void setVariable(T variable) {
        this.variable = variable;
        }

        public void run()
         {
            try {
                //Stop getter using native client on these same position.
                JNIOpcUaClient.this.watchThreadsList.get(position).interrupt();
                JNIOpcUaClient.this.watchThreadsList.get(position).join(100);
                //JNIOpcUaClient.this.watchThreadsList.get(position).join();
                if (variable instanceof java.lang.Boolean) {
                    cSetVariableBoolean(position, (Boolean) variable);
                    if (cWrite(position)) {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.variable_save_success));
                    } else {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.cannot_save_variable));
                    }
                } else if (variable instanceof java.lang.Integer) {
                    cSetVariableInteger(position, (Integer) variable);
                    if (cWrite(position)) {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.variable_save_success));
                    } else {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.cannot_save_variable));
                    }
                } else if (variable instanceof java.lang.Long) {
                    cSetVariableLong(position, (Long) variable);
                    if (cWrite(position)) {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.variable_save_success));
                    } else {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.cannot_save_variable));
                    }
                } else if (variable instanceof java.lang.String) {
                    cSetVariableString(position, (String) variable);
                    if (cWrite(position)) {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.variable_save_success));
                    } else {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.cannot_save_variable));
                    }
                } else if (variable instanceof java.lang.Float) {
                    cSetVariableFloat(position, (Float) variable);
                    if (cWrite(position)) {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.variable_save_success));
                    } else {
                        makeToast("(" + JNIOpcUaClient.this.getNodes().get(position).toString()
                                + ", " + JNIOpcUaClient.this.getVariablePaths().get(position).toString() + "): "
                                + MeasurementManager.applicationContext.getString(R.string.cannot_save_variable));
                    }
                }
                //Start new getter thread.
                OpcUaVariableGetter oldThread = (OpcUaVariableGetter) watchThreadsList.get(position);
                //watchThreadsList.remove(position);
                watchThreadsList.set(position,(OpcUaVariableGetter) oldThread.clone());

                //Start clone thread.
                watchThreadsList.get(position).run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean initVariableOnDemand(String endpoint, int node, String nodeName, Object variable, int position)
    {
        if(!cConnectWithUndefiniedVariable(endpoint, node, nodeName, position)) {
            makeToast(MeasurementManager.applicationContext.getString(R.string.cannot_connect));
            return false;
        }
        //Set type
        if(variable instanceof Integer)
        {
            cAddUndefiniedIntVariable(position);
            return true;

        }
        else if(variable instanceof Boolean)
        {
            cAddUndefiniedBoolVariable(position);
            return true;

        }
        else if(variable instanceof Float)
        {
            cAddUndefiniedFloatVariable(position);
            return true;
        }
        return false;
    }

    public Object readOnDemand(Object variable, int position) {
        //Set type
        if(variable instanceof Integer)
        {
            if(!cReadUndefiniedVariable(position))
            {
                makeToast( MeasurementManager.applicationContext.getString(R.string.cannot_load_variable));
                return null;
            }
            return cGetUndefiniedIntVariable(position);
        }
        else if(variable instanceof Boolean)
        {
            if(!cReadUndefiniedVariable(position))
            {
                makeToast( MeasurementManager.applicationContext.getString(R.string.cannot_load_variable));
                return null;
            }
            return cGetUndefiniedBoolVariable(position);
        }
        else if(variable instanceof Float)
        {
            if(!cReadUndefiniedVariable(position))
            {
                makeToast( MeasurementManager.applicationContext.getString(R.string.cannot_load_variable));
                return null;
            }
            return cGetUndefiniedFloatVariable(position);
        }
        else return null;
    }

    public void deleteVariable(int position)
    {
        cCleanUndefiniedVariable(position);
    }


    public boolean initAlertVariable(int node, String nodeName)
    {
        if(!cInitAlertVariableReading(endpoint, node, nodeName)) {
            makeToast(MeasurementManager.applicationContext.getString(R.string.cannot_connect));
            return false;
        }
        return true;
    }

    public boolean readAlert()
    {
        if(!cReadAlertVariable())
        {
            return false;
        }
        return true;
    }

    public boolean checkAlertValue()
    {
        return cGetAlertValue();
    }

    public boolean initInfoVariable(int node, String nodeName)
    {
        if(!cInitInfoVariableReading(endpoint, node, nodeName)) {
            makeToast(MeasurementManager.applicationContext.getString(R.string.cannot_connect));
            return false;
        }
        return true;
    }

    public boolean readInfo()
    {
        if(!cReadInfoVariable())
        {
            return false;
        }
        return true;
    }

    public void setScanningVariable(String scan)
    {
        cSetScanVariable(scan);
    }

    public boolean initScanVariable(int node, String nodeName)
    {
        if(!cInitScanVariableWriting(endpoint, node, nodeName)) {
            makeToast(MeasurementManager.applicationContext.getString(R.string.cannot_connect));
            return false;
        }
        return true;
    }

    public boolean writeScanningVariable()
    {
        if(!cWriteScanVariable())
        {
            return false;
        }
        return true;
    }

    public void setLoginVariable(String scan)
    {
        cSetLoginVariable(scan);
    }

    public boolean initLoginVariable(int node, String nodeName)
    {
        if(!cInitLoginVariableWriting(endpoint, node, nodeName)) {
            return false;
        }
        return true;
    }

    public boolean writeLoginVariable()
    {
        if(!cWriteLoginVariable())
        {
            return false;
        }
        return true;
    }

    public boolean checkInfoValue()
    {
        return cGetInfoValue();
    }
}
