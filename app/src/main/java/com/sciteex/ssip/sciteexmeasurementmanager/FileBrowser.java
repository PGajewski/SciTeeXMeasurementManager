package com.sciteex.ssip.sciteexmeasurementmanager;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.sciteex.ssip.sciteexmeasurementmanager.services.EventFilterService;
import com.sciteex.ssip.sciteexmeasurementmanager.services.UserInactiveService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileBrowser extends ListActivity {

    public static View forEventService = null;

    private String path;

    private final static String PROGRAM_EXTENSION_REGEX = "\\.smm$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_browser);

        // Use the current directory as title
        path = "/storage";
        if (getIntent().hasExtra("path")) {
            path = getIntent().getStringExtra("path");
        }
        setTitle(path);



        // Read all files sorted into the values-array
        List dirs = new ArrayList();
        List files = new ArrayList();

        File dir = new File(path);
        if (!dir.canRead()) {
            setTitle(getTitle() + " (inaccessible)");
        }
        String[] list = dir.list();
        if (list != null) {
            for (String file : list) {
                try {
                if (!file.startsWith(".")) {
                    String tempPath;
                    if (path.endsWith(File.separator)) {
                        tempPath = path + file;
                    } else {
                        tempPath = path + File.separator + file;
                    }
                        File dirOrFile = new File(tempPath);
                        if (dirOrFile.isDirectory()) {
                            dirs.add(file);
                        } else {
                            files.add(file);
                        }
                    }
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        Collections.sort(dirs);
        Collections.sort(files);

        int position = dirs.size();
        List allContent = new ArrayList(dirs);
        allContent.addAll(files);
        // Put the data into the list
        FileAdapter adapter = new FileAdapter(this,
                android.R.layout.simple_list_item_activated_2, android.R.id.text1, allContent);
        adapter.setFileFirstPosition(position);
        setListAdapter(adapter);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventFilterService.actualView = findViewById(R.id.file_browser_view);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String filename = (String) getListAdapter().getItem(position);
        if (path.endsWith(File.separator)) {
            filename = path + filename;
        } else {
            filename = path + File.separator + filename;
        }
        if (new File(filename).isDirectory()) {
            Intent intent = new Intent(this, FileBrowser.class);
            intent.putExtra("path", filename);
            startActivity(intent);
        } else {
            Pattern compiledPattern = Pattern.compile(PROGRAM_EXTENSION_REGEX);
            Matcher matcher = compiledPattern.matcher(filename);
            if(matcher.find())
            {
                Intent intent = new Intent(getApplicationContext(), MeasurementManager.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("foundedFile", filename);
                startActivity(intent);
            }
            else
                Toast.makeText(this, getString(R.string.incompatible_file), Toast.LENGTH_SHORT).show();
        }
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
}