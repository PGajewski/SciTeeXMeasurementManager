package com.sciteex.ssip.sciteexmeasurementmanager;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.google.common.io.Files;
import com.sciteex.ssip.sciteexmeasurementmanager.services.ButtonActionService;
import com.sciteex.ssip.sciteexmeasurementmanager.services.DatabaseService;
import com.sciteex.ssip.sciteexmeasurementmanager.services.EventFilterService;
import com.sciteex.ssip.sciteexmeasurementmanager.services.OpcUaService;
import com.sciteex.ssip.sciteexmeasurementmanager.services.UserInactiveService;
import com.sciteex.ssip.stllibrary.StlViewer;

import org.artoolkit.ar.base.assets.AssetHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class MeasurementManager extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //Services
    public static MeasurementManager thisActivity = null;


    /**
     *
     */
    static Context applicationContext = null;

    /**
     * Number of static options in menu.
     */
    private static final int NUMBER_OF_OPTIONS_IN_MENU = 7;

    private static final int CHANGING_VARIABLES_TIME = 1000;

    private static final int MAX_CONNECTION_ERRORS_TOLERANCE = 1;
    /**
     * OPC UA client
     */
    private JNIOpcUaClient opcUaClient = JNIOpcUaClient.getSingletonInstance();

    /**
     * Database client
     */
    private SSIPDatabaseConnection databaseClient = SSIPDatabaseConnection.getSingletonInstance();

    /**
     * List of lists with comments to pictures.
     */
    private List<List<String>> commentsList = new ArrayList<>();

    public static File commentsFile;

    private ListView queryNamesList;
    private ArrayAdapter<String> adapter ;

    private static boolean downloadAdditionalInformation = true;

    private View mainPage;
    private boolean fal;

    public static void setDownloadAdditionalInformation(boolean value) {
        downloadAdditionalInformation = value;
    }

    public static boolean getDownloadAdditionalInformation() {
        return downloadAdditionalInformation;
    }

    private final static int MAXIMUM_COMMENT_LENGTH = 250;

    private final static String COMMENT_FILE_PATH = "comments.com";

    //Path to inner file with paths to added program.
    public final static String ADDED_PROGRAM_PATHS_FILE = "added_programs";

    public static String addedProgramPathsFile;

    public static File pathsFile;

    //List of objects to get pictures.
    private List<MeasurementExecuter> programList = new ArrayList<>();

    private View appView;

    /**
     * Client of Bluetooth connection.
     */
    private BluetoothThread bluetoothThread;

    //Number of actual chosen program.
    private int programNumber = -1;

    //Additional info panels.
    private WebView leftPanel;
    private WebView rightPanel;
    private WebView bottomPanel;
    private WebView topPanel;

    /**
     * Main layout on screen.
     */
    private View mainLayout;
    private Menu programMenu;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private static final int REQUEST_PATH = 1;

    //Elcometer measures.

    private LinearLayout measureHistory;
    private TextView actualMeasure;
    private ScrollView measureScrollBox;


    /**
     * Elcometer menu item.
     */
    private MenuItem elcometerItem;
    /**
     * Last measure from Elcometer gauge.
     */
    private List<String[]> elcometerMeasures = new ArrayList<>();

    /**
     * Variable says that is gauge connected to Android device.
     */
    private boolean connectedWithElcometer = false;
    private boolean isVariableReader = false;
    /**
     * Last Elcometer measure.
     */
    private String[] lastElcometerMeasure;


    private List<BluetoothDevice> pairedGauges = new ArrayList<>();
    private List<BluetoothDevice> availableDevices = new ArrayList<>();

    private String appUser = "";

    /**
     * Left button to change picture.
     */
    private FloatingActionButton prevButton;
    private FloatingActionButton nextButton;
    /**
     * Right button to change picture.
     */
    private Button rightButton;

    private LinearLayout measureBox;

    //Main object of picture inside.
    private Bitmap picture;

    /**
     * Linear layout of hint to open drawer menu.
     */
    private LinearLayout drawerHint;

    private ImageView mainImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().getBooleanExtra("EXIT", false))
        {
            finish();
        }
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_manager);

        //Keep screen on.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Save actual reference to activity;
        thisActivity = this;
        applicationContext = this.getApplicationContext();
        this.initializeInstance();
        //Save instance
        //Get file list from file.
        pathsFile = new File(getFilesDir().getAbsolutePath() + "/" + ADDED_PROGRAM_PATHS_FILE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                MeasurementManager.this.hideHint();
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //Set invisible of additional info panels.
        topPanel = (WebView) findViewById(R.id.top_panel);
        bottomPanel = (WebView) findViewById(R.id.bottom_panel);
        leftPanel = (WebView) findViewById(R.id.left_panel);
        rightPanel = (WebView) findViewById(R.id.right_panel);

        //Set change image buttons of front.
        prevButton = (FloatingActionButton) findViewById(R.id.PreviousPictureButton);
        nextButton = (FloatingActionButton) findViewById(R.id.NextPictureButton);

        //Set blur of hint image.
        FadingImageView mFadingImageView = (FadingImageView) findViewById(R.id.slide_menu_image);

        mFadingImageView.setEdgeLength(28);

        mFadingImageView.setFadeDirection(FadingImageView.FadeSide.RIGHT_SIDE);

        //Get main layout.
        mainPage = findViewById(R.id.mainPage);

        //Get main picture reference.
        mainImage = (ImageView) findViewById(R.id.mainImage);

        ImageView elcometerLogo = (ImageView) findViewById(R.id.elcometerImage);
        elcometerLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openElcometerWebPage();
            }
        });

        setPanelsVisibility(false);
        //Set visibility of buttons.
        setButtonVisibility(false);

        //Deserialize comments.
        if ((commentsFile = new File(getFilesDir().getAbsolutePath() + "/" + COMMENT_FILE_PATH)).exists()) {
            deserializeComments();
        }

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        navigationView.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
               System.out.print("Click!");
            }
        });
        //Get hint field.
        drawerHint = (LinearLayout) findViewById(R.id.menu_hint);

        //Rejestr context menu on picture.
        registerForContextMenu(mainImage);

        //startButtonActionListening();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Change, if program is chosen.
        if (programNumber != -1) {
            //Set visibility of buttons.
            setButtonVisibility(true);
            setPanelsVisibility(programList.get(programNumber).isAdditionalInfo);
        }
    }

    @Override
    public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        //Check connecting to Elcometer isn't active UA isn't active.

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (this.programNumber != -1) {
            closeProgram();
        } else {
            showExitQuery();
        }
    }

    //Buttons to manage pistures.
    public void onPreviousPictureButtonPressed(View view) {
        Context context = getApplicationContext();
        if (programNumber != -1) {
            try {
                updatePicture(programList.get(programNumber).getPreviousPicture());
                setPanelsVisibility(programList.get(programNumber).isAdditionalInfo);
                Stream.of(programList.get(programNumber).getOpcUaVariables())
                        .forEach(v -> showVariableOnScreen(v));
                updateAdditionalInformation();
            } catch (Exception e) {
                Toast toast = Toast.makeText(context, getString(R.string.first_measure_point), Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            Toast toast = Toast.makeText(context, R.string.please_run_program, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void onNextPictureButtonPressed(View view) {
        Context context = getApplicationContext();
        if (programNumber != -1) {
            try {
                updatePicture(programList.get(programNumber).getNextPicture());
                setPanelsVisibility(programList.get(programNumber).isAdditionalInfo);
                Stream.of(programList.get(programNumber).getOpcUaVariables())
                        .forEach(v -> showVariableOnScreen(v));
                updateAdditionalInformation();
            } catch (Exception e) {
                Toast toast = Toast.makeText(context, getString(R.string.last_measure_point), Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            Toast toast = Toast.makeText(context, R.string.please_run_program, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //navigationView.getMenu().clear();
        getMenuInflater().inflate(R.menu.measurement_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_add_program) {
            addProgram();
        } else if (id == R.id.nav_manage) {
            showSettings();
        } else if (id == R.id.nav_ar) {
            openARModule();
        } else if (id == R.id.nav_opc) {
            createOpcVariableDialog();
        } else if (id == R.id.nav_barcode) {
            openBarcodeReader();
        } else if(id == R.id.nav_variable_reader) {
            isVariableReader = !isVariableReader;
            item.setCheckable(isVariableReader);
            item.setChecked(isVariableReader);
        } else if (id == R.id.nav_elcometer) {
            if (!connectedWithElcometer) {
                connectWithElcometer();
                connectedWithElcometer = true;

                //Change color of menu option.
                elcometerItem = item;
                elcometerItem.setCheckable(true);
                elcometerItem.setChecked(true);
            } else {
                disconnectElcometer();
                connectedWithElcometer = false;

                //Change color of menu option. Simply setChecked(false) doesn't work.
                elcometerItem.setCheckable(false);
            }
        } else {

            //Get programs by button id.
            programNumber = id - Menu.FIRST - NUMBER_OF_OPTIONS_IN_MENU;
            showProgram(programList.get(programNumber));
        }

        MovingPopupWindow.restorePositions();
        repaintMenu();

        //DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        //drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showProgram(MeasurementExecuter program) {
        Context context = getApplicationContext();
        try {
            program.reset();
            mainImage.setImageBitmap(BitmapFactory.decodeStream(program.getNextPicture()));
            setPanelsVisibility(program.isAdditionalInfo);
            //Set visibility of buttons.
            setButtonVisibility(true);

            //Set panel content
            updateAdditionalInformation();
        } catch (Exception e) {
            Toast toast = Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void setUser(String userName)
    {
        appUser = userName;
    }

    public String getUser()
    {
        return appUser;
    }

    private void connectWithElcometer() {

        BroadcastReceiver mReceiver = null;
        availableDevices.clear();
        //Found paired devices.
        try {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, getString(R.string.not_bluetooth), Toast.LENGTH_LONG).show();
                return;
            }
            ;

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                pairedGauges = Stream.of(pairedDevices)
                        .filter(p -> "E-".equals(p.getName().substring(0, 2)) || "Elcometer-".equals(p.getName().substring(0, 10)))
                        .toList();
            }

            //Filtered paired gauges to gauge in range.
            mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // Add the name and address to an array adapter to show in a ListView
                        availableDevices.add(device);
                    }
                }
            };
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
            //mBluetoothAdapter.startDiscovery();
/*
        //Compaire available device with paired.
            pairedGauges = Stream.of(pairedGauges)
                    .filter(d ->
                        Stream.of(availableDevices)
                                .anyMatch(a -> d.getAddress().equals(a.getAddress())))
                    .toList();
                    */
            //If paired elcometer devices is empty, call intent to pair with another device.
            if (pairedGauges.size() == 0) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, bluetoothThread.REQUEST_ENABLE_BT);
                }
            }
            //If there is more than one, choose device list.
            else if (pairedGauges.size() >= 2) {
                chooseElcometerDevice(pairedGauges);

            } else {
                initializeConnectionToGauge(pairedGauges.get(0), getString(R.string.connect_with_last_elcometer));
            }
        } finally {
            unregisterReceiver(mReceiver);

        }
    }

    public void clickTest(View view) {
        System.out.print("Click!");
    }

    private class BluetoothProgressAsyncTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        private BluetoothDevice gauge;

        private String message;

        public void setMessage(String message) {
            this.message = message;
        }

        public BluetoothProgressAsyncTask(BluetoothDevice gauge, String message) {
            this.gauge = gauge;
            this.message = message;
        }

        @Override
        protected void onPreExecute() {

            bluetoothThread = new BluetoothThread(gauge);

            //Check Bluetooth interface.
            if(!bluetoothThread.isSupported())
            {
                Toast.makeText(MeasurementManager.this, getString(R.string.bluetooth_not_support), Toast.LENGTH_LONG).show();
                bluetoothThread.cancel();
                this.cancel(true);
                return;
            }

            if(!bluetoothThread.isEnabled())
            {
                Toast.makeText(MeasurementManager.this, getString(R.string.bluetooth_not_active), Toast.LENGTH_LONG).show();
                bluetoothThread.cancel();
                this.cancel(true);
                return;
            }

            dialog = new ProgressDialog(MeasurementManager.thisActivity) {
                @Override
                public void onBackPressed() {
                    if (BluetoothProgressAsyncTask.this != null) {
                        BluetoothProgressAsyncTask.this.cancel(true);
                        connectedWithElcometer = false;
                        elcometerItem.setCheckable(false);
                        this.dismiss();
                    }
                }
            };
            dialog.setMessage(getString(R.string.connecting_with_gauge));
            dialog.show();
        }

        protected Void doInBackground(Void... args) {
            ExecutorService service = Executors.newSingleThreadExecutor();

            try {
                bluetoothThread = new BluetoothThread(gauge);

                Future<?> f = service.submit(bluetoothThread);
                f.get(5, TimeUnit.SECONDS);
            } catch(InterruptedException e)
            {   e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }finally {service.shutdown();}
            return null;
        }

        protected void onPostExecute(Void result) {
            // do UI work here
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (bluetoothThread.isConnected()) {
                //Make main box visible.
                measureBox.setVisibility(LinearLayout.VISIBLE);
                Toast.makeText(MeasurementManager.this, message, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MeasurementManager.this, getText(R.string.cannot_connect_with_gauge), Toast.LENGTH_SHORT).show();
                connectedWithElcometer = false;
                elcometerItem.setCheckable(false);

            }

        }
    }

    private void initializeConnectionToGauge(BluetoothDevice gauge, String message) {
        //Catch fields for Elcometer measures.
        measureHistory = (LinearLayout) findViewById(R.id.measureHistory);
        measureBox = (LinearLayout) findViewById(R.id.measureBox);
        actualMeasure = (TextView) findViewById(R.id.actualMeasure);
        measureScrollBox = (ScrollView) findViewById(R.id.measureScrollBox);

        //Set measure box on front.
        //measureBox.bringToFront();

        //ViewCompat.setTranslationZ(measureBox, -1.0f);

        BluetoothProgressAsyncTask bluetoothTask = new BluetoothProgressAsyncTask(gauge, message);
        bluetoothTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void disconnectElcometer() {
        this.bluetoothThread.cancel();
        this.measureBox.setVisibility(LinearLayout.INVISIBLE);
        MeasurementManager.this.actualMeasure.setText(getString(R.string.wait_for_measure));

        //Clear history on gauge.
        while (MeasurementManager.this.measureHistory.getChildCount() != 0) {
            MeasurementManager.this.measureHistory.removeViewAt(MeasurementManager.this.measureHistory.getChildCount() - 1);
        }
        TextView historyText = new TextView(this);
        historyText.setText(getString(R.string.no_history));
        MeasurementManager.this.measureHistory.addView(historyText);


        Toast.makeText(this, getString(R.string.disconnect_gauge), Toast.LENGTH_SHORT).show();
    }

    private void addProgram() {
        Intent intent1 = new Intent(this, FileBrowser.class);
        startActivityForResult(intent1, REQUEST_PATH);

    }

    private void showSettings() {
        Intent intent1 = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent1, REQUEST_PATH);
    }

    public List<List<String>> getCommentsList() {
        return this.commentsList;
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        //Reset timer.
        resetTimer();

        //startTimer();
        MovingPopupWindow.restorePositions();

        //Run OPC UA clients.
        if (OpcUaService.opcUaConfigIsChanged) {
            OpcUaService.opcUaConfigIsChanged = false;
            startOpcUaClient();
        }

        //Run database clients.
        if (DatabaseService.databaseConfigIsChanged) {
            DatabaseService.databaseConfigIsChanged = false;
            startDatabaseClient();
        }

        //Run event threads
        if (EventFilterService.alertConfigIsChanged) {
            EventFilterService.alertConfigIsChanged = false;
            startAlertThread();
        }

        if (EventFilterService.infoConfigIsChanged) {
            EventFilterService.infoConfigIsChanged = false;
            startInfoThread();
        }

        //Save view for event service.
        EventFilterService.actualView = drawer;

            //Download additional data, if needed.
        if (downloadAdditionalInformation) {
            for (MeasurementExecuter program : programList) {
                try {
                    program.openProgram(true);
                } catch (Exception e) {
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, getString(R.string.additional_data_error), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }

        //Try add new program.
        if (getIntent().hasExtra("foundedFile")) {
            try {
                //Get program's name.
                MeasurementExecuter program = new MeasurementExecuter();
                program.setProgramPath(getIntent().getExtras().getString("foundedFile"));
                program.openProgram(downloadAdditionalInformation);

                //Remove extra.
                getIntent().removeExtra("foundedFile");

                //Check for repeating programs on list.
                for (MeasurementExecuter programFromList : programList) {
                    if (programFromList.getProgramName().equals(program.getProgramName())) {
                        Context context = getApplicationContext();
                        Toast toast = Toast.makeText(context, getString(R.string.program_already_added), Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                }


                //Add program and button program to lists.
                programList.add(program);

                //Set message.
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, getString(R.string.added_program), Toast.LENGTH_SHORT);
                toast.show();

                repaintMenu();
            } catch (Exception e) {
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    private void startInfoThread() {
        Intent serviceIntent = new Intent(this, EventFilterService.class);
        serviceIntent.putExtra("ACTION", EventFilterService.RUN_INFO_SERVICE);
        startService(serviceIntent);
    }

    private void startAlertThread() {
        Intent serviceIntent = new Intent(this, EventFilterService.class);
        serviceIntent.putExtra("ACTION", EventFilterService.RUN_ALERT_SERVICE);
        startService(serviceIntent);
    }

    private void startInfoAndAlertThread() {
        Intent serviceIntent = new Intent(this, EventFilterService.class);
        serviceIntent.putExtra("ACTION", EventFilterService.RUN_ALERT_AND_INFO_SERVICE);
        startService(serviceIntent);
    }

    private void updatePicture(InputStream in) {
        ImageView image = (ImageView) findViewById(R.id.mainImage);
        image.setImageBitmap(BitmapFactory.decodeStream(in));
    }

    private void repaintMenu() {
        Menu programMenu = navigationView.getMenu().getItem(NUMBER_OF_OPTIONS_IN_MENU).getSubMenu();
        programMenu.clear();

        //Draw program buttons.
        try {
            int index = Menu.FIRST + NUMBER_OF_OPTIONS_IN_MENU;
            for (MeasurementExecuter program : programList) {
                programMenu.add(0, index, 0, program.getProgramName()).setIcon(R.drawable.ic_menu_send).setVisible(true);
                index++;
            }

            //If programlist is empty, add "No program" to list.
            if (programList.isEmpty()) {
                programMenu.add(0, index, 0, getString(R.string.no_program));
            }

            navigationView.inflateMenu(R.menu.empty_resource);

        } catch (Exception e) {
            Context context = getApplicationContext();
            Toast toast = Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void onPause() {
        //Write paths to file.
        Writer output = null;
        try {
            pathsFile.delete();
            output = new BufferedWriter(new FileWriter(pathsFile.getAbsolutePath(), true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (MeasurementExecuter program : programList) {
            try {
                output.append(program.getProgramPath().concat("\n"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            {
                output.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    @Override
    public void onStart() {

        if (pathsFile.exists() && pathsFile.isFile()) {
            try {
                String fileContent = new String(Files.toByteArray(pathsFile));

                //Adding programs to list.
                for (String path : fileContent.split("\n")) {
                    MeasurementExecuter program = new MeasurementExecuter();
                    program.setProgramPath(path);
                    program.openProgram(downloadAdditionalInformation);
                    programList.add(program);
                }
                repaintMenu();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            repaintMenu();
        }
        goToLogin();
        super.onStart();
    }

    public void onRestart() {
        super.onRestart();
        programList.clear();
    }

    private void setPanelsVisibility(boolean visible) {
        //Reload content.
        leftPanel.reload();
        rightPanel.reload();
        topPanel.reload();
        bottomPanel.reload();

        //Set all panels in background

        if (visible) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                leftPanel.setVisibility(View.VISIBLE);
                rightPanel.setVisibility(View.VISIBLE);
                topPanel.setVisibility(View.GONE);
                bottomPanel.setVisibility(View.GONE);
            } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                leftPanel.setVisibility(View.GONE);
                rightPanel.setVisibility(View.GONE);
                topPanel.setVisibility(View.VISIBLE);
                bottomPanel.setVisibility(View.VISIBLE);
            }
        } else {
            leftPanel.setVisibility(View.GONE);
            rightPanel.setVisibility(View.GONE);
            topPanel.setVisibility(View.GONE);
            bottomPanel.setVisibility(View.GONE);
        }
        mainPage.postInvalidate();
    }

    private void setPanelsContent(String topPanelContent, String bottomPanelContent, String leftPanelContent, String rightPanelContent) {

        //Set content.
        leftPanel.loadData(leftPanelContent, "text/html", "UTF-8");
        rightPanel.loadData(rightPanelContent, "text/html", "UTF-8");
        topPanel.loadData(topPanelContent, "text/html", "UTF-8");
        bottomPanel.loadData(bottomPanelContent, "text/html", "UTF-8");

        //Set transparent background.
        leftPanel.setBackgroundColor(Color.TRANSPARENT);
        rightPanel.setBackgroundColor(Color.TRANSPARENT);
        topPanel.setBackgroundColor(Color.TRANSPARENT);
        bottomPanel.setBackgroundColor(Color.TRANSPARENT);
    }

    private void updateAdditionalInformation() {
        Context context = getApplicationContext();
        if (programList.get(programNumber).isAdditionalInfo) {
            try {
                setPanelsContent(
                        programList.get(programNumber).getAdditionalData("topPanel"),
                        programList.get(programNumber).getAdditionalData("bottomPanel"),
                        programList.get(programNumber).getAdditionalData("leftPanel"),
                        programList.get(programNumber).getAdditionalData("rightPanel"));

            } catch (Exception e) {
                leftPanel.loadUrl("about:blank");
                rightPanel.loadUrl("about:blank");
                topPanel.loadUrl("about:blank");
                bottomPanel.loadUrl("about:blank");
                setPanelsVisibility(false);


                Toast toast = Toast.makeText(context, R.string.additional_data_error, Toast.LENGTH_SHORT);
                toast.show();
            }

        }
    }

    public void createCommentDialog() {
        if (programNumber != -1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.add_comment));

            // Set up the input
            EditText input = new EditText(this);

            //Set max comment size.

            InputFilter[] fa = new InputFilter[1];
            fa[0] = new InputFilter.LengthFilter(MAXIMUM_COMMENT_LENGTH);

            //Set size string limitter.
            input.setFilters(fa);
            input.setMaxLines(5);
            input.setLines(5);

            //Set border
            //input.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.text_edit_border));

            //Set padding
            input.setPadding(30, 30, 30, 30);

            //Set background
            input.setBackgroundColor(Color.LTGRAY);

            //Get picture number
            final int slideNumber = programList.get(programNumber).getSlideNumber();

            //Set text from last comment and create additional containers.
            try {
                commentsList.get(programNumber);
            } catch (IndexOutOfBoundsException e) {
                for (int i = commentsList.size(); commentsList.size() <= programNumber; ++i)
                    commentsList.add(i, new ArrayList<String>());
            }

            try {
                input.setText(commentsList.get(programNumber).get(slideNumber));
            } catch (IndexOutOfBoundsException e) {
                for (int i = commentsList.get(programNumber).size(); commentsList.get(programNumber).size() <= slideNumber; ++i)
                    commentsList.get(programNumber).add(i, new String());
            }
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton(getString(R.string.accept_comment), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    commentsList.get(programNumber).add(slideNumber, input.getText().toString());
                }
            });
            builder.setNegativeButton(getString(R.string.cancel_comment), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }


    public void onMainImageClick(View view) {
        if (programNumber != -1)
            createCommentDialog();
        else
            openSciTeeXWebPage();

    }

    /**
     * Method to serialize comments to file.
     */
    private void serializeComments() {
        try {
            FileOutputStream out = new FileOutputStream(commentsFile.getAbsolutePath());
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(commentsList);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            System.out.println("Problem serializing: " + e);
        }

    }

    /**
     * Method to deserialize comments from file.
     */
    private void deserializeComments() {
        try {
            FileInputStream in = new FileInputStream(commentsFile.getAbsolutePath());
            ObjectInputStream ois = new ObjectInputStream(in);
            commentsList = (List<List<String>>) (ois.readObject());
            ois.close();
        } catch (Exception e) {
            System.out.println("Problem serializing: " + e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Serialize comments.
        serializeComments();
    }

    //////////////////////////////////////////AR////////////////////////////////////
    protected void initializeInstance() {

        // Unpack assets to cache directory so native library can read them.
        // N.B.: If contents of assets folder changes, be sure to increment the
        // versionCode integer in the AndroidManifest.xml file.
        AssetHelper assetHelper = new AssetHelper(getAssets());
        assetHelper.cacheAssetFolder(this, "Data");
    }

    private void openARModule() {
        Intent intent1 = new Intent(this, SciTeeXAR.class);
        startActivityForResult(intent1, REQUEST_PATH);

    }

    private void openBarcodeReader() {
        //Intent intent1 = new Intent(this, BarcodeScanner.class);
        //startActivityForResult(intent1, REQUEST_PATH);
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
        scanIntegrator.initiateScan();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (intent != null) {
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            if(isVariableReader)
                showFromVariableReader(scanContent);
            else
                openProgramByName(scanContent);

            //Sending scan result to OPC UA Server.
            sendingScanVariable(scanContent);
        }



        if (OpcUaService.opcUaConfigIsChanged) {
            OpcUaService.opcUaConfigIsChanged = false;
            startOpcUaClient();
        }
        if (DatabaseService.databaseConfigIsChanged) {
            DatabaseService.databaseConfigIsChanged = false;
            startDatabaseClient();
        }

        //Run event threads
        if (EventFilterService.alertConfigIsChanged) {
            EventFilterService.alertConfigIsChanged = false;
            startAlertThread();
        }

        if (EventFilterService.infoConfigIsChanged) {
            EventFilterService.infoConfigIsChanged = false;
            startInfoThread();
        }

        MovingPopupWindow.restorePositions();
    }

    private void closeProgram() {
        programList.get(programNumber).reset();
        programNumber = -1;
        setPanelsVisibility(false);
        mainImage.setImageResource(R.mipmap.logo_blue);
        mainImage.invalidate();
        Toast.makeText(this, R.string.program_closed, Toast.LENGTH_SHORT).show();
    }

    private void openProgramByName(String name) {
        //Unicate program name is quarranteed by adding programs system, so foreach is
        for (MeasurementExecuter program : programList) {
            try {
                String programName;
                programName = program.getProgramName();
                if (name.equals(programName)) {
                    //Open find program.
                    programNumber = programList.indexOf(program);
                    showProgram(program);

                    //Display toast about opened program.
                    Toast.makeText(this, getString(R.string.open_progam), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startOpcUaClient() {
        Intent serviceIntent = new Intent(this, OpcUaService.class);
        serviceIntent.putExtra("ACTION", OpcUaService.RUN_OPC_UA_CLIENT);
        startService(serviceIntent);
    }

    private void startDatabaseClient() {
        Intent serviceIntent = new Intent(this, DatabaseService.class);
        serviceIntent.putExtra("ACTION", DatabaseService.RUN_DATABASE_CLIENT);
        startService(serviceIntent);
    }

    public void createOpcVariableDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.opc_ua_variable_modifier, null);
        builder.setView(dialogView);

        builder.setTitle(getString(R.string.modify_opc_ua_variable));

        LinearLayout nodesLayout = (LinearLayout) dialogView.findViewById(R.id.opc_nodes);
        LinearLayout variableLayout = (LinearLayout) dialogView.findViewById(R.id.opc_values);

        //List for variables values.
        List<Object> lastReadValues = opcUaClient.getActualVariables();

        //List of views in menu to change OPC variables.y
        List<View> opcUaVariablesViews = new ArrayList<>();

        //Set text field, if is zero.
        if (lastReadValues.size() == 0) {
            TextView text = new TextView(this);
            text.setTextColor(Color.BLACK);
            text.setGravity(Gravity.CENTER_VERTICAL);
            text.setText(getString(R.string.no_opc_ua_data));
            text.setPadding(10, 10, 10, 10);
            nodesLayout.addView(text);
            text.getLayoutParams().height = 50;
        }

        //Create line for all declared variable.
        for (int i = 0; i < opcUaClient.nativeSize; ++i) {

            TextView text = new TextView(this);
            text.setTextColor(Color.BLACK);
            text.setGravity(Gravity.CENTER_VERTICAL);
            text.setText("(" + opcUaClient.getNodes().get(i) + ", " + opcUaClient.getVariablePaths().get(i) + ")");
            text.setPadding(10, 10, 10, 10);
            nodesLayout.addView(text);
            text.getLayoutParams().height = 50;

            //Adding variables field.
            if (lastReadValues.get(i) instanceof java.lang.Boolean) {
                CheckBox variableField = new CheckBox(this);
                variableField.setChecked(((Boolean) lastReadValues.get(i)).booleanValue());
                variableField.setPadding(10, 10, 10, 10);
                variableLayout.addView(variableField);
                variableField.getLayoutParams().height = 50;
                opcUaVariablesViews.add(variableField);
            } else if (lastReadValues.get(i) instanceof java.lang.Integer) {
                EditText variableField = new EditText(this);
                variableField.setText(lastReadValues.get(i).toString());
                variableField.setPadding(10, 10, 10, 10);
                variableLayout.addView(variableField);
                variableField.setInputType(InputType.TYPE_CLASS_NUMBER);
                variableField.getLayoutParams().height = 50;
                opcUaVariablesViews.add(variableField);
            } else if (lastReadValues.get(i) instanceof java.lang.Float) {
                EditText variableField = new EditText(this);
                variableField.setText(lastReadValues.get(i).toString());
                variableField.setPadding(10, 10, 10, 10);
                variableLayout.addView(variableField);
                variableField.setInputType(InputType.TYPE_CLASS_NUMBER);
                variableField.getLayoutParams().height = 50;
                opcUaVariablesViews.add(variableField);

            } else if (lastReadValues.get(i) instanceof java.lang.Long) {
                EditText variableField = new EditText(this);
                variableField.setText(lastReadValues.get(i).toString());
                variableField.setPadding(10, 10, 10, 10);
                variableLayout.addView(variableField);
                variableField.setInputType(InputType.TYPE_CLASS_NUMBER);
                variableField.getLayoutParams().height = 50;
                opcUaVariablesViews.add(variableField);

            } else if (lastReadValues.get(i) instanceof java.lang.Double) {
                EditText variableField = new EditText(this);
                variableField.setText(lastReadValues.get(i).toString());
                variableField.setPadding(10, 10, 10, 10);
                variableLayout.addView(variableField);
                variableField.setInputType(InputType.TYPE_CLASS_NUMBER);
                variableField.getLayoutParams().height = 50;
                opcUaVariablesViews.add(variableField);

            } else if (lastReadValues.get(i) instanceof java.lang.String) {
                EditText variableField = new EditText(this);
                variableField.setText(lastReadValues.get(i).toString());
                variableField.setPadding(10, 10, 10, 10);
                variableLayout.addView(variableField);
                opcUaVariablesViews.add(variableField);

                variableField.getLayoutParams().height = 50;
            }

        }

        //Create dialog interface to manage content
        OpcUaDialogController dialog = new OpcUaDialogController(lastReadValues, opcUaVariablesViews);
        dialog.setOpcUaClient(opcUaClient);
        builder.setPositiveButton(getString(R.string.accept_opc_ua_change), dialog);
        builder.setNegativeButton(getString(R.string.cancel_opc_ua_change), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * Method to connect with Elcometer webpage
     */
    public void openElcometerWebPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.elcometer_webpage)));
        startActivity(browserIntent);
    }

    public void openSciTeeXWebPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.sciteex_webpage)));
        startActivity(browserIntent);
    }

    class OpcUaDialogController implements DialogInterface.OnClickListener {
        //Create dialog interface to manage content

        private List<Object> lastReadValues;
        private List<View> opcUaVariablesViews;
        private JNIOpcUaClient client;

        public OpcUaDialogController(List<Object> lastReadValues, List<View> opcUaVariablesViews) {
            this.lastReadValues = lastReadValues;
            this.opcUaVariablesViews = opcUaVariablesViews;
        }

        public void setOpcUaClient(JNIOpcUaClient client) {
            this.client = client;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            //List for actual variables values in fields.
            List<Object> variablesFromFields = new ArrayList<>();

            //Get values from all variable fields.
            for (int i = 0; i < opcUaVariablesViews.size(); ++i) {
                if (lastReadValues.get(i) instanceof java.lang.Boolean) {
                    variablesFromFields.add(new Boolean(((CheckBox) opcUaVariablesViews.get(i)).isChecked()));
                } else if (lastReadValues.get(i) instanceof java.lang.Integer) {
                    variablesFromFields.add(Integer.parseInt(((TextView) opcUaVariablesViews.get(i)).getText().toString()));
                } else if (lastReadValues.get(i) instanceof java.lang.Float) {
                    variablesFromFields.add(Float.parseFloat(((TextView) opcUaVariablesViews.get(i)).getText().toString()));
                } else if (lastReadValues.get(i) instanceof java.lang.Long) {
                    variablesFromFields.add(Long.parseLong(((TextView) opcUaVariablesViews.get(i)).getText().toString()));
                } else if (lastReadValues.get(i) instanceof java.lang.Double) {
                    variablesFromFields.add(Double.parseDouble(((TextView) opcUaVariablesViews.get(i)).getText().toString()));
                } else if (lastReadValues.get(i) instanceof java.lang.String) {
                    variablesFromFields.add(((TextView) opcUaVariablesViews.get(i)).getText().toString());
                }
            }

            //Compare new values with old.
            for (int i = 0; i < variablesFromFields.size(); ++i) {
                if (!lastReadValues.get(i).equals(variablesFromFields.get(i))) {
                    OpcUaDialogWritter writter = new OpcUaDialogWritter();
                    writter.setPosition(i);
                    writter.setVariable(variablesFromFields.get(i));
                    new Thread(writter).start();
                }
            }
        }
    }

    private void chooseElcometerDevice(List<BluetoothDevice> elcometerDevices) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MeasurementManager.this);
        builderSingle.setIcon(R.mipmap.bluetooth_icon);
        builderSingle.setTitle("Choose Elcometer device:");
        builderSingle.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK &&
                        event.getAction() == KeyEvent.ACTION_UP &&
                        !event.isCanceled()) {
                    elcometerItem.setCheckable(false);
                    connectedWithElcometer = false;
                    dialog.cancel();
                    return true;
                }
                return false;
            }
        });

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MeasurementManager.this, android.R.layout.select_dialog_singlechoice);
        for (BluetoothDevice gauge : elcometerDevices) {
            arrayAdapter.add(gauge.getName());
        }

        builderSingle.setNegativeButton(getString(R.string.cancel_choose_gauge), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                elcometerItem.setCheckable(false);
                connectedWithElcometer = false;
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                initializeConnectionToGauge(elcometerDevices.get(which), getString(R.string.connected_with_gauge) + elcometerDevices.get(which).getName());
            }
        });
        builderSingle.show();
    }


    class OpcUaDialogWritter<T> implements Runnable {

        private int position;

        private T variable;

        public OpcUaDialogWritter setVariable(T variable) {
            this.variable = variable;
            return this;
        }

        public OpcUaDialogWritter setPosition(int i) {
            this.position = i;
            return this;
        }

        public void run() {
            opcUaClient.write(position, variable);
        }
    }

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    private interface ElcometerConnection {
        public final static String DATE_BEGINNING = "01/01/2010";

        //Constants in transmission frame.
        /**
         * Start of packet.
         */
        public final static byte SOH = 0x01;

        /**
         * Acknowledge
         */
        public final static byte ACK = 0x06;

        /**
         * Negative Acknowledge
         */
        public final static byte NAK = 0x15;

        /**
         * End of package.
         */
        public final static byte EOP = 0x17;

        /**
         * End of command.
         */
        public final static byte EOT = 0x14;

        /**
         * Data item delimiter
         */
        public final static byte MDML = 0x1C;

        /**
         * Reset Communications and re-negotiate communications.
         */
        public final static byte EOS = 0x10;

        /**
         * Close communications
         */
        public final static byte EOC = 0x1B;

        /**
         * Not Applicable.
         */
        public final static byte NA = 0x07;

        public final static byte CONFIRM_LAST_CONNECTION = 0x4F;

        /**
         * Escape Command – Gauge returns to a state where it is waiting for a command.
         * Gauge responds to this control code with an ACK
         */
        public final static byte ESC = 0x16;
    }

    private void rememberMeasure() {
        TextView oldMeasure = new TextView(thisActivity);

        //Get first message from history.
        TextView oldHistory = (TextView) MeasurementManager.this.measureHistory.getChildAt(0);

        //If is "no_history, clear container.
        if (getString(R.string.no_history).equals(oldHistory.getText())) {
            MeasurementManager.this.measureHistory.removeAllViews();
        }
        int index = MeasurementManager.this.measureHistory.getChildCount();
        oldMeasure.setText((index + 1) + ". " + actualMeasure.getText());
        oldHistory.setOnClickListener(new MeasureHistoryOnClickListener(index));
        MeasurementManager.this.measureHistory.addView(oldMeasure, index++);

        //Scroll view to last added element.
        measureScrollBox.fullScroll(View.FOCUS_DOWN);


        elcometerMeasures.add(lastElcometerMeasure);
    }

    /**
     * Method to addlast measure from gauge to measures list.
     *
     * @param v
     */
    public void rememberMeasure(View v) {
        //Check measured value isn't empty.
        if (getString(R.string.wait_for_measure).equals(MeasurementManager.this.actualMeasure.getText())) {
            Toast.makeText(this, getString(R.string.first_make_measure), Toast.LENGTH_SHORT).show();
            return;
        }
        int textColor = actualMeasure.getTextColors().getDefaultColor();
        if (textColor == Color.RED) {
            confirmAddBadMeasure();
        } else {
            rememberMeasure();
        }
    }

    private class MeasureHistoryOnClickListener implements View.OnClickListener {
        private int position;

        public MeasureHistoryOnClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            deleteMeasure(position);
        }

        private void deleteMeasureFromList(int position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MeasurementManager.this);
            builder.setTitle(getString(R.string.delete_measure));

            // Set up the input
            TextView alertMessage = new TextView(MeasurementManager.this);
            alertMessage.setText(getString(R.string.confirm_deleting_of_point_text) + position + "?");

            builder.setView(alertMessage);

            // Set up the buttons
            builder.setPositiveButton(getString(R.string.accept_deleting_measure), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(getString(R.string.cancel_deleting_measure), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        }

        private void deleteMeasure(int position) {
            measureHistory.removeViewAt(position);
            elcometerMeasures.set(position, new String[]{(position + 1) + ". " + getString(R.string.empty_measure), "", ""});
            TextView emptyRecord = new TextView(MeasurementManager.this);
            emptyRecord.setOnClickListener(new MeasureHistoryOnClickListener(position));
            emptyRecord.setText(elcometerMeasures.get(position)[0]);
            measureHistory.addView(emptyRecord, position);
        }
    }

    /**
     * Inner class for Bluetooth connection.
     */
    private class BluetoothThread extends Thread {

        /**
         * Found Elcometer gauges.
         */
        private static final String TAG = "MY_APP_DEBUG_TAG";
        public static final int REQUEST_ENABLE_BT = 1;
        public final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        private Handler mHandler; // handler that gets info from Bluetooth service
        private BluetoothDevice device;

        private BluetoothSocket mmSocket;
        private InputStream mmInStream = null;
        private OutputStream mmOutStream = null;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public BluetoothThread(BluetoothDevice device) {
            this.device = device;
        }

        public boolean isSupported()
        {
            if(mBluetoothAdapter == null)
            {
                return false;
            }
            else return true;
        }

        public boolean isEnabled()
        {
            return mBluetoothAdapter.isEnabled();
        }


        public void run() {

            try
            {
                mmSocket = device.createRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
                mmSocket.connect();
            }catch(IOException e)
            {
                Toast.makeText(MeasurementManager.this, getText(R.string.cannot_connect), Toast.LENGTH_SHORT).show();
                return;
            }

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    if (mmInStream.available() > 0) {
                        numBytes = mmInStream.read(mmBuffer);

                        //Confirm recived measure to gauge.
                        write(new byte[]{ElcometerConnection.CONFIRM_LAST_CONNECTION});

                        //Add actual value for history.
                        MeasurementManager.this.runOnUiThread(new Runnable() {
                            public void run() {

                                //Add new value.
                                String[] temp = readValue(mmBuffer);
                                actualMeasure.setText(temp[0] + " " + temp[1]);

                                float tempFloat = Float.parseFloat(temp[0]);

                                //Check bounds and change color if needed.
                                if (programNumber != -1) {
                                    MeasureBounds bounds = programList.get(programNumber).getBoundsForPoint(elcometerMeasures.size());
                                    if (bounds != null) {
                                        //Right bounds - color green.
                                        if (tempFloat <= bounds.getMax() && tempFloat >= bounds.getMin()) {
                                            actualMeasure.setTextColor(Color.GREEN);
                                        } else {
                                            actualMeasure.setTextColor(Color.RED);
                                        }
                                    }
                                } else {
                                    actualMeasure.setTextColor(ContextCompat.getColor(thisActivity, R.color.black_overlay));
                                }

                                MeasurementManager.this.lastElcometerMeasure = temp;

                                //Send actual value to OPC UA
                                //Get Elcometer array location on OPC Ua server.
                                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                if (sharedPreferences.getBoolean("parse_value_key", true)) {
                                    int arrayNode = Integer.parseInt(sharedPreferences.getString("elcometer_actual_value_node", getString(R.string.default_elcometer_array_node)));
                                    String arrayNodeName = sharedPreferences.getString("elcometer_actual_value_path", getString(R.string.default_elcometer_array_path));
                                    String endpoint = sharedPreferences.getString("opc_ua_endpoint", getString(R.string.default_opc_ua_endpoint));

                                    //Init write AsyncTask with Progress bar.
                                    ElcometerMeasuresAsyncTask writeToOpcUa = new ElcometerMeasuresAsyncTask(opcUaClient,
                                            getString(R.string.sending_elcometer),
                                            arrayNode, arrayNodeName, endpoint, tempFloat);
                                    thisActivity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            writeToOpcUa.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                        }
                                    });
                                }
                            }
                        });
                    } else SystemClock.sleep(100);
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

        private String[] readValue(byte[] byteArray) {
            //TODO
            String measure = null;
            String[] measureData = null;
            try {
                measure = new String(byteArray, "UTF-8");
                measureData = Stream.of(measure.substring(0, measure.indexOf("\r")).split(" "))
                        .filter(s -> !"".equals(s))
                        .toArray(String[]::new);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return measureData;
        }

        public boolean isConnected() {
            return mmSocket.isConnected();
        }
    }

    public void sendElcometerMeasuresToOpcUa(View view) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isParsingEnable = sharedPreferences.getBoolean("parse_value_key", true);

        //Prepare float array.
        float[] array = new float[elcometerMeasures.size()];
        List<Float> floatList;

        if (isParsingEnable)
            floatList = Stream.of(elcometerMeasures)
                    .map(sa -> parseFloatFromMansisaAndUnit(sa[0], sa[1]))
                    .toList();
        else
            floatList = Stream.of(elcometerMeasures)
                    .map(sa -> Float.parseFloat(sa[0]))
                    .toList();

        for (int i = 0; i < floatList.size(); ++i) {
            array[i] = (float) floatList.get(i);
        }

        //Get Elcometer array location on OPC Ua server.
        int arrayNode = Integer.parseInt(sharedPreferences.getString("elcometer_opc_ua_node", getString(R.string.default_elcometer_array_node)));
        String arrayNodeName = sharedPreferences.getString("elcometer_opc_ua_path", getString(R.string.default_elcometer_array_path));
        String endpoint = sharedPreferences.getString("opc_ua_endpoint", getString(R.string.default_opc_ua_endpoint));

        //Init write AsyncTask with Progress bar.
        ElcometerMeasuresAsyncTask writeToOpcUa = new ElcometerMeasuresAsyncTask(opcUaClient,
                getString(R.string.sending_elcometer),
                arrayNode, arrayNodeName, endpoint, array);
        writeToOpcUa.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Inner class to send array from Elcometer to OPC UA server.
     */
    private class ElcometerMeasuresAsyncTask extends AsyncTask<Void, Void, Void> {
        private boolean isArraySend;

        private ProgressDialog dialog;

        private JNIOpcUaClient client;

        private int node;

        private String nodeName;

        private String endpoint;

        private float[] measures;

        private float actualValue;

        private String message;

        private boolean isCorrectlySend;

        public void setMessage(String message) {
            this.message = message;
        }

        public void setMeasures(float[] measures) {
            this.measures = measures;
        }

        public void setActualValue(float value) {
            this.actualValue = value;
        }

        public ElcometerMeasuresAsyncTask(JNIOpcUaClient client, String message, int node, String nodeName, String endpoint, float[] measures) {
            this.client = client;
            setMessage(message);
            this.node = node;
            this.nodeName = nodeName;
            this.endpoint = endpoint;
            setMeasures(measures);
            isArraySend = true;
        }

        public ElcometerMeasuresAsyncTask(JNIOpcUaClient client, String message, int node, String nodeName, String endpoint, float actualValue) {
            this.client = client;
            setMessage(message);
            this.node = node;
            this.nodeName = nodeName;
            this.endpoint = endpoint;
            setActualValue(actualValue);
            isArraySend = false;
        }

        @Override
        protected void onPreExecute() {
            if (isArraySend) {
                dialog = new ProgressDialog(MeasurementManager.thisActivity);
                dialog.setMessage(message);
                dialog.show();
            }
        }

        protected Void doInBackground(Void... args) {

            if (isArraySend)
                isCorrectlySend = client.writeElcometerArray(endpoint, node, nodeName, measures);
            else
                isCorrectlySend = client.writeElcometerActualValue(endpoint, node, nodeName, actualValue);
            return null;
        }

        protected void onPostExecute(Void result) {
            // do UI work here
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            Context context = getApplicationContext();
            if (isCorrectlySend) {
                if (isArraySend)
                    Toast.makeText(context, R.string.elcometer_array_to_opc_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.cannot_send_elcometer_measures, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static float parseFloatFromMansisaAndUnit(String mantisa, String unit) {

        if(mantisa == null)
        {
            return Float.POSITIVE_INFINITY;
        }
        try {
            float value = Float.parseFloat(mantisa);
            if ("mm".equals(unit))
                value *= 0.001;
            else if ("um".equals(unit))
                value *= 0.000001;
            else if ("nm".equals(unit))
                value *= 0.000000001;
            return value;
        } catch (Exception e) {
            return 0.0f;
        }
    }

    /**
     * Method to hide slide menu hint after first open drawer menu.
     */
    private void hideHint() {
        drawerHint.setVisibility(LinearLayout.INVISIBLE);
    }

    public static void sendViewToBack(final View child) {
        final ViewGroup parent = (ViewGroup) child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    public void confirmAddBadMeasure() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.remember_measure));

        // Set up the input
        TextView alertMessage = new TextView(this);
        alertMessage.setPadding(30, 10, 10, 10);
        alertMessage.setText(getString(R.string.adding_measure_confirmation_text));

        builder.setView(alertMessage);

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.accept_adding_measure), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                rememberMeasure();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_adding_measure), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }


    ////////////////////////////////////////Stl Viewer////////////////////////////////////////////////
    private void createStlFreeView() {
        if (programNumber != -1) {
            StlViewer view = null;
            try {
                view = new StlViewer(this, programList.get(programNumber).getStlObject());
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, getString(R.string.stl_error), Toast.LENGTH_SHORT);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.stl_viewer));

            //Start view.
            view.read();

            builder.setView(view);

            builder.setNegativeButton(getString(R.string.cancel_comment), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }

    private void showQueryResult(String title, String resultInHTML) {


        WebView view = new WebView(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        //Start view.
        view.loadData(resultInHTML, "text/html", null);

        builder.setView(view);

        builder.setNegativeButton(getString(R.string.cancel_comment), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }



    private void createDatabaseQueryList() {


        final Dialog dialog = new Dialog(this);
        dialog.setTitle(getString(R.string.choose_query));
        dialog.setContentView(R.layout.query_list_layout);

        queryNamesList = (ListView) dialog.findViewById(R.id.queryListView);
        List<String> queryNames = DatabaseService.getQueryNames();

        adapter = new ArrayAdapter<String>(this, R.layout.query_row, queryNames);
        queryNamesList.setAdapter(adapter);


        Button addQueryButton = (Button) dialog.findViewById(R.id.addQueryButton);
        addQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                MeasurementManager.this.addNewQueries();
            }
        });

        Button cancelQueryButton = (Button) dialog.findViewById(R.id.cancelQuery);
        cancelQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        if (programNumber != -1) {
            super.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(R.menu.slide_context_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.run_stl_viewer: createStlFreeView(); break;
            case R.id.run_sql_viewer: createDatabaseQueryList(); break;
            default: return false;
        }
        return super.onContextItemSelected(item);
    }

    private void setButtonVisibility(boolean value)
    {
        if(value) {
            prevButton.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
        }
        else
        {
            prevButton.setVisibility(View.INVISIBLE);
            nextButton.setVisibility(View.INVISIBLE);
        }
    }

    void showFromVariableReader(String contentFromCamera)
    {
        //Part content from camera.
        char type = contentFromCamera.charAt(0);
        String[] afterConcat = contentFromCamera.split(":");

        //Cast node to int
        try {
            int node = Integer.parseInt(afterConcat[0].substring(1, afterConcat[0].length()));
            createVariableWindow(afterConcat[0].charAt(0),node,afterConcat[1]);
            MovingPopupWindow.restorePositions();
        }
        catch(Exception e)
        {
            Toast.makeText(this, R.string.invalid_node, Toast.LENGTH_SHORT);
        }
    }

    void showVariableOnScreen(OpcUaVariable opcUaVariable)
    {
        //Cast node to int
        try {
            MovingPopupWindow window = createVariableWindow(opcUaVariable.getType(),opcUaVariable.getNode(),opcUaVariable.getPath());
            MovingPopupWindow.restorePositions();
            window.setWindowPosition(mainImage, opcUaVariable.getXPosition(), opcUaVariable.getYPosition());
        }
        catch(Exception e)
        {
            Toast.makeText(this, R.string.invalid_node, Toast.LENGTH_SHORT);
        }
    }

    private MovingPopupWindow createVariableWindow(char type, int node, String path) throws Exception
    {
        final TextView variableText = new android.support.v7.widget.AppCompatTextView(this){
            private int position = MovingPopupWindow.total_counter + 1;
            public void finalize()
            {
                opcUaClient.deleteVariable(position);
            }
        };
        variableText.setGravity(Gravity.CENTER);
        variableText.setBackground(ContextCompat.getDrawable(this,R.drawable.text_edit_border));
        variableText.setBackgroundColor(Color.LTGRAY);
        variableText.setHeight(50);
        variableText.setWidth(200);
        variableText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        variableText.setText("Error!");

        final MovingPopupWindow window = new MovingPopupWindow(this, path, variableText);
        window.show(findViewById(R.id.partners));
        Handler handler = new Handler();
        final Runnable r = new Runnable()
        {
            private Object variable;
            private int errorsCounter;

            public Runnable setVariable(char typeChar)
            {
                switch(typeChar)
                {
                    case 'b': variable = new Boolean(false); break;
                    case 'f': variable = new Float(0.0); break;
                    case 'i': variable = new Integer(0); break;
                    case 'd': variable = new Double(0); break;
                    case 's': variable = new String("empty"); break;
                }
                return this;
            }

            public void run()
            {

                if(!opcUaClient.initVariableOnDemand(opcUaClient.getEndpoint(),node, path, variable, window.getPositionCounter()))
                    return;

                while (!window.wasDissmissButtonPressed()) {
                    variable = opcUaClient.readOnDemand(variable, window.getPositionCounter());
                    if(variable == null)
                    {
                        ++errorsCounter;
                        if(errorsCounter == MAX_CONNECTION_ERRORS_TOLERANCE) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    window.dismiss();
                                }
                            });
                            return;
                        }
                    }
                    else
                    {
                        errorsCounter = 0;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            variableText.setText(variable.toString());
                            variableText.setWidth(WRAP_CONTENT);
                            if(variableText.getWidth() < 200 )
                                variableText.setWidth(200);
                            variableText.invalidate();
                        }});
                    try {
                        Thread.currentThread().sleep(CHANGING_VARIABLES_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.setVariable(type);
        new Thread(r).start();
        return window;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    private void showExitQuery() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.exit));

        //Start view.
        final TextView text = new TextView(this);
        text.setText(R.string.exit_question);
        text.setTextAlignment(LinearLayout.TEXT_ALIGNMENT_CENTER);
        text.setTextSize(15);

        builder.setView(text);

        builder.setNegativeButton(getString(R.string.hide_app), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                Intent intent = new Intent(MeasurementManager.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("EXIT", true);
                startActivity(intent);
            }
        });
        builder.setPositiveButton(getString(R.string.close_app), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finishAffinity();
            }
        });
        builder.setNeutralButton(getString(R.string.stay_in_app), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void goToLogin()
    {
        Intent serviceIntent = new Intent(this, UserInactiveService.class);
        serviceIntent.putExtra("ACTION", UserInactiveService.GO_WITHOUT_TIMER);
        stopService(serviceIntent);
        startService(serviceIntent);
    }

    @Override
    public void onUserInteraction(){
        resetTimer();
    }

    private void resetTimer()
    {
        if(UserInactiveService.isTimerActive)
        {
            Intent serviceIntent = new Intent(this, UserInactiveService.class);
            serviceIntent.putExtra("ACTION", UserInactiveService.RESET_TIMER);
            stopService(serviceIntent);
            startService(serviceIntent);
        }
    }

    private void startButtonActionListening()
    {
        Intent serviceIntent = new Intent(this, ButtonActionService.class);
        startService(serviceIntent);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_POWER:
                Intent serviceIntent = new Intent(this, UserInactiveService.class);
                serviceIntent.putExtra("ACTION", UserInactiveService.GO_WITHOUT_TIMER);
                serviceIntent.putExtra("SCREEN_SHUTDOWN", true);
                stopService(serviceIntent);
                startService(serviceIntent);
                break;
            default:
                return super.dispatchKeyEvent(event);
        }
        return true;
    }

    private void sendingScanVariable(String scanningResult)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if(sharedPreferences.getBoolean("scan_result_switch", true))
        {
            int scanNode = Integer.parseInt(sharedPreferences.getString("opc_ua_scan_node", getString(R.string.default_scanning_result_node)));
            String scanPath = sharedPreferences.getString("opc_ua_scan_path", getString(R.string.default_scanning_result_path));
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if(opcUaClient.initScanVariable(scanNode, scanPath))
                    {
                        opcUaClient.setScanningVariable(scanningResult);
                        if(opcUaClient.writeScanningVariable())
                        {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MeasurementManager.thisActivity,MeasurementManager.thisActivity.getString(R.string.scan_sent), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else
                        {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MeasurementManager.thisActivity,MeasurementManager.thisActivity.getString(R.string.cannot_send_scan) + ": " + getString(R.string.no_connection_or_variable), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    else
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MeasurementManager.thisActivity,MeasurementManager.thisActivity.getString(R.string.cannot_send_scan) + ": " + getString(R.string.write_error), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            };
            new Thread(r).start();
        }
    }

    private void addNewQueries()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_new_query));

        AlertDialog alertDialog = builder.create();
        LayoutInflater inflater = this.getLayoutInflater();
        alertDialog.setContentView(inflater.inflate(R.layout.add_query_layout, null));
        final EditText titleView = (EditText) alertDialog.findViewById(R.id.queryTitleView);
        LinearLayout addQueryLayout = (LinearLayout) alertDialog.findViewById(R.id.addQueryLayout);
        final EditText queryBodyView = (EditText) alertDialog.findViewById(R.id.queryTitleView);

        //Set size string limitter
        queryBodyView.setMaxLines(5);
        queryBodyView.setLines(5);

        //Set border
        //input.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.text_edit_border));

        //Set padding
        addQueryLayout.setPadding(30, 30, 30, 30);

        //Set background
        addQueryLayout.setBackgroundColor(Color.LTGRAY);

        builder.setView(addQueryLayout);

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.add_new_query), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (TextUtils.isEmpty(titleView.getText())) {
                    titleView.setError(MeasurementManager.this.getString(R.string.query_title_required));
                    return;
                }
                if (TextUtils.isEmpty(titleView.getText())) {
                    titleView.setError(MeasurementManager.this.getString(R.string.query_body_required));
                    return;
                }
                List<String> queryNames = DatabaseService.getQueryNames();
                DatabaseService.addQuery(titleView.getText().toString(), queryBodyView.getText().toString());
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_adding_query_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}