package com.sciteex.ssip.sciteexmeasurementmanager;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.sciteex.ssip.sciteexmeasurementmanager.services.DatabaseService;
import com.sciteex.ssip.sciteexmeasurementmanager.services.EventFilterService;
import com.sciteex.ssip.sciteexmeasurementmanager.services.OpcUaService;
import com.sciteex.ssip.sciteexmeasurementmanager.services.UserInactiveService;

import java.sql.SQLException;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */

    static Activity thisActivity = null;


    private static void clearProgramsList()
    {
        //Delete file with paths.
        MeasurementManager.pathsFile.delete();
        Context context = thisActivity.getApplicationContext();
        Toast toast = Toast.makeText(context, R.string.program_cleared, Toast.LENGTH_SHORT);
        toast.show();

    }

    private static void sendCommentsToDatabase()
    {
        //Create database DAO based on values from preferences.
        SSIPDatabaseConnection dao = SSIPDatabaseConnection.getSingletonInstance();

        //Get data from form.
        dao.setConnectionURL(DataSyncPreferenceFragment.databaseServer.getSummary() + "/" + DataSyncPreferenceFragment.databaseName.getSummary());
        dao.setDatabaseUser(DataSyncPreferenceFragment.databaseUser.getSummary().toString());
        dao.setDatabasePassword(DataSyncPreferenceFragment.databasePassword.getSummary().toString());

        final ProgressDialog dialog = ProgressDialog.show(thisActivity, "",
                thisActivity.getString(R.string.sending_comments_in_progress), true);

        try {
            dao.startSingleton();
            dao.sendComments(MeasurementManager.thisActivity.getCommentsList(),
                    Integer.parseInt(GeneralPreferenceFragment.jobId.getDependency())
                    , Integer.parseInt(GeneralPreferenceFragment.workerId.getDependency()));
        }catch(SQLException e)
        {
            Context context = thisActivity.getApplicationContext();
            Toast toast = Toast.makeText(context, R.string.send_to_database_error + e.getLocalizedMessage(), Toast.LENGTH_SHORT);
            toast.show();

        }
        finally {
            dialog.hide();
        }

    }

    private static boolean onPasswordChange(Preference preference, Object value) {
        String stringValue = value.toString();
        StringBuffer buff = new StringBuffer();
        for(int i = 0; i != stringValue.length(); ++i) {
            buff.append('*');
        }
            // For all other preferences, set the summary to the value's
            // simple string representation.
            preference.setSummary(buff.toString());
        return true;
    }

    private static boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // Set the summary to reflect the new value.
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

        } else if (preference instanceof RingtonePreference) {
            // For ringtone preferences, look up the correct display value
            // using RingtoneManager.
            if (TextUtils.isEmpty(stringValue)) {
                // Empty values correspond to 'silent' (no ringtone).
                preference.setSummary(R.string.pref_ringtone_silent);

            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(
                        preference.getContext(), Uri.parse(stringValue));

                if (ringtone == null) {
                    // Clear the summary if there was a lookup error.
                    preference.setSummary(null);
                } else {
                    // Set the summary to reflect the new ringtone display
                    // name.
                    String name = ringtone.getTitle(preference.getContext());
                    preference.setSummary(name);
                }
            }

        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            preference.setSummary(stringValue);
        }
        return true;
    }

    //Preferrences.
    public SwitchPreference additionalInfoPref;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            return SettingsActivity.onPreferenceChange(preference, value);
        }
    };

    public static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToStarsListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            return SettingsActivity.onPasswordChange(preference, value);
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void bindPreferenceSummaryToPassword(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToStarsListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToStarsListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void bindPreferenceSummaryToValueWithOwnChangeListener(Preference preference, Preference.OnPreferenceChangeListener changeListener) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(changeListener);

        // Trigger the listener immediately with the preference's
        // current value.
        changeListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisActivity = this;

        setupActionBar();
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

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        /**
         * Switch preference for additional information.
         */
        SwitchPreference additionalInfoPref;

        /**
         * List preference for inactive time.
         */
        ListPreference timeList;

        /**
         * Preference fot worker ID.
         */
        static EditTextPreference workerId;

        /**
         * Preference for job ID.
         */
        static EditTextPreference jobId;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            //Set listening of preference.
            Preference myPref = (Preference) findPreference("clean_programs");
            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    SettingsActivity.clearProgramsList();
                    return true;
                }});

            //Preferrences for additional information.
            additionalInfoPref = (SwitchPreference) findPreference("additional_info_switch");
            additionalInfoPref.setChecked(MeasurementManager.getDownloadAdditionalInformation());
            additionalInfoPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    MeasurementManager.setDownloadAdditionalInformation(GeneralPreferenceFragment.this.additionalInfoPref.isChecked());
                    return true;
                }});

            timeList = (ListPreference) findPreference("inactive_time_list");
            bindPreferenceSummaryToValueWithOwnChangeListener(timeList, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int time = Integer.parseInt((String) newValue);
                    if(time < 0)
                    {
                        UserInactiveService.isTimerActive = false;
                        return SettingsActivity.onPreferenceChange(preference, newValue);
                    }
                    UserInactiveService.isTimerActive = true;
                    UserInactiveService.setInactiveTime(time);
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            workerId = (EditTextPreference) findPreference("worker_id");
            jobId = (EditTextPreference) findPreference("worker_id");

            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("worker_id"));
            bindPreferenceSummaryToValue(findPreference("actual_job_id"));

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {

        /**
         * Preference to URL to connect with database
         */
        public static Preference databaseServer;

        /**
         * Preference to URL to connect with database
         */
        public static Preference databaseName;

        /**
         * Preference to database user.
         */
        public static Preference databaseUser;

        /**
         * Preference to database password
         */
        public static Preference databasePassword;

        /**
         * Preference to database type
         */
        public static Preference databaseType;

        /**
         * Preference to OPC UA endpoint.
         */
        public static Preference opcUaEndpoint;

        /**
         * Preference to OPC UA user.
         */
        public static Preference opcUaUser;

        /**
         * Preference to OPC UA password.
         */
        public static Preference opcUaPassword;

        /**
         * Preference to OPC UA variables.
         */
        public static Preference opcUaVariables;

        /**
         * Preference to alert OPC UA variable node.
         */
        public static Preference alertNode;

        /**
         * Preference to alert OPC UA variable path.
         */
        public static Preference alertPath;

        /**
         * Preference to alert message.
         */
        public static Preference alertMessage;

        /**
         * Preference to info OPC UA variable node.
         */
        public static Preference infoNode;

        /**
         * Preference to info OPC UA variable path.
         */
        public static Preference infoPath;

        /**
         * Preference to info message.
         */
        public static Preference infoMessage;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);

            //Add listener to send comments to database.
            Preference myDatabasePref = (Preference) findPreference("send_comments_to_database");
            myDatabasePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    SettingsActivity.sendCommentsToDatabase();
                    return true;
                }});

            //Add objects to database.
            databaseServer = (EditTextPreference) findPreference("database_server");
            bindPreferenceSummaryToValueWithOwnChangeListener(databaseServer, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    DatabaseService.databaseConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });
            databaseName = (EditTextPreference) findPreference("database_name");
            bindPreferenceSummaryToValueWithOwnChangeListener(databaseName, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    DatabaseService.databaseConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });
            databaseUser = (EditTextPreference) findPreference("database_user");
            bindPreferenceSummaryToValueWithOwnChangeListener(databaseUser, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    DatabaseService.databaseConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });
            databasePassword = (EditTextPreference) findPreference("database_password");
            bindPreferenceSummaryToValueWithOwnChangeListener(databasePassword, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    DatabaseService.databaseConfigIsChanged = true;
                    return SettingsActivity.onPasswordChange(preference, newValue);
                }
            });
            databaseType = (ListPreference) findPreference("database_type");
            bindPreferenceSummaryToValueWithOwnChangeListener(databaseType, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    DatabaseService.databaseConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            opcUaEndpoint = (EditTextPreference) findPreference("opc_ua_endpoint");
            bindPreferenceSummaryToValueWithOwnChangeListener(opcUaEndpoint, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    OpcUaService.opcUaConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });
            opcUaUser = (EditTextPreference) findPreference("opc_ua_user");
            bindPreferenceSummaryToValueWithOwnChangeListener(opcUaUser, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    OpcUaService.opcUaConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });
            opcUaPassword = (EditTextPreference) findPreference("opc_ua_password");
            bindPreferenceSummaryToValueWithOwnChangeListener(opcUaPassword, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    OpcUaService.opcUaConfigIsChanged = true;
                    return SettingsActivity.onPasswordChange(preference, newValue);                }
            });
            opcUaVariables = (EditTextPreference) findPreference("opc_ua_variables_field");
            bindPreferenceSummaryToValueWithOwnChangeListener(opcUaVariables, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    OpcUaService.opcUaConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            alertNode = (EditTextPreference) findPreference("alert_node");
            bindPreferenceSummaryToValueWithOwnChangeListener(alertNode, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    EventFilterService.alertConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            alertPath = (EditTextPreference) findPreference("alert_path");
            bindPreferenceSummaryToValueWithOwnChangeListener(alertPath, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    EventFilterService.alertConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            alertMessage = (EditTextPreference) findPreference("alert_message");
            bindPreferenceSummaryToValueWithOwnChangeListener(alertMessage, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    EventFilterService.alertConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            infoNode = (EditTextPreference) findPreference("info_node");
            bindPreferenceSummaryToValueWithOwnChangeListener(infoNode, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    EventFilterService.alertConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            infoPath = (EditTextPreference) findPreference("info_path");
            bindPreferenceSummaryToValueWithOwnChangeListener(infoPath, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    EventFilterService.alertConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            infoMessage = (EditTextPreference) findPreference("info_message");
            bindPreferenceSummaryToValueWithOwnChangeListener(infoMessage, new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    EventFilterService.alertConfigIsChanged = true;
                    return SettingsActivity.onPreferenceChange(preference, newValue);
                }
            });

            setHasOptionsMenu(true);
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //

            bindPreferenceSummaryToValue(findPreference("elcometer_opc_ua_node"));
            bindPreferenceSummaryToValue(findPreference("elcometer_opc_ua_path"));
            bindPreferenceSummaryToValue(findPreference("elcometer_actual_value_node"));
            bindPreferenceSummaryToValue(findPreference("elcometer_actual_value_path"));

            bindPreferenceSummaryToValue(findPreference("elcometer_opc_ua_node"));
            bindPreferenceSummaryToValue(findPreference("elcometer_opc_ua_path"));
            bindPreferenceSummaryToValue(findPreference("elcometer_actual_value_node"));
            bindPreferenceSummaryToValue(findPreference("elcometer_actual_value_path"));

            bindPreferenceSummaryToValue(findPreference("sync_frequency"));

            bindPreferenceSummaryToValue(findPreference("opc_ua_login_node"));
            bindPreferenceSummaryToValue(findPreference("opc_ua_login_path"));
            bindPreferenceSummaryToValue(findPreference("opc_ua_scan_node"));
            bindPreferenceSummaryToValue(findPreference("opc_ua_scan_path"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public class DatabaseQueriesDefinitions extends AlertDialog
    {

        protected DatabaseQueriesDefinitions(Context context) {
            super(context, true, new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                }
            });
            this.setTitle("SQL definitions");

        }

    }

}
