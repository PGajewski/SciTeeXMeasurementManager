package com.sciteex.ssip.sciteexmeasurementmanager;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.sciteex.ssip.sciteexmeasurementmanager.FileBrowser;

import java.io.File;
import java.util.List;

/**
 * Created by Gajos on 11/10/2017.
 */

public class FileAdapter extends ArrayAdapter {

    int fileFirstPosition = -1;


    public FileAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public FileAdapter(FileBrowser fileBrowser, int simple_list_item_activated_2, int text1, List allContent) {
        super(fileBrowser, simple_list_item_activated_2, text1,allContent);
    }

    public void setFileFirstPosition(int position)
    {
        fileFirstPosition = position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v= super.getView(position, convertView, parent);
        if(fileFirstPosition <= position)
            v.setBackgroundColor(Color.WHITE);
        else
            v.setBackgroundColor(Color.LTGRAY);
        return v;
    }
}
