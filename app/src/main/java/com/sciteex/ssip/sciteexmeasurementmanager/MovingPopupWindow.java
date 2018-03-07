package com.sciteex.ssip.sciteexmeasurementmanager;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;


/**
 * Created by Gajos on 1/26/2018.
 */

public class MovingPopupWindow {

    static int total_counter = 0;

    /**
     * List of popups in system.
     */
    private static List<MovingPopupWindow> popupList = new ArrayList<>();
    //TODO: Popup list.
    private int position;

    private View popupView;

    private int orgX, orgY;
    private int offsetX, offsetY;

    private PopupWindow window;

    private Context context;
    private boolean buttonPressed = false;
    public MovingPopupWindow(Context context, String title, View view) {
        this.context = context;
        LayoutInflater layoutInflater =
                (LayoutInflater)context
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
        popupView = layoutInflater.inflate(R.layout.movable_popup, null);
        RelativeLayout layout = (RelativeLayout) popupView.findViewById(R.id.movableContainer);

        view.setVisibility(View.VISIBLE);
        layout.addView(view);
        window = new PopupWindow(
                popupView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        TextView titleView = (TextView) popupView.findViewById(R.id.pop_title);
        titleView.setText(title);
        titleView.setBackgroundColor(Color.TRANSPARENT);
        titleView.setTextColor(ContextCompat.getColor(MeasurementManager.thisActivity, R.color.colorAccent));
        Button btnDismiss = (Button)popupView.findViewById(R.id.dismiss);

        btnDismiss.setOnClickListener(new Button.OnClickListener(){

            //Add view.
            Button btnDismiss = (Button)popupView.findViewById(R.id.dismiss);


            @Override
            public void onClick(View v) {
                window.dismiss();
                buttonPressed = true;
            }});

        //popupWindow.showAsDropDown(btnOpenPopup, 50, -30);

        popupView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        orgX = (int) event.getX();
                        orgY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        offsetX = (int)event.getRawX() - orgX;
                        offsetY = (int)event.getRawY() - orgY;
                        window.update(offsetX, offsetY, -1, -1, true);
                        break;
                }
                return true;
            }});
        position = ++total_counter;
        popupList.add(this);
    }

    public boolean wasDissmissButtonPressed() {
        return buttonPressed;
    }

    public void dismiss()
    {
        window.dismiss();
        popupList.remove(this);
    }

    private void restorePosition()
    {
        if(offsetX != 0 || offsetY != 0)
            window.update(offsetX, offsetY, -1, -1, true);
    }

    public void show(View view)
    {
        if(view != null) {
            window.showAsDropDown(view, view.getWidth() / 2, view.getHeight() + 50);
        }
    }

    public int getPositionCounter()
    {
        return position;
    }

    public synchronized static void restorePositions()
    {
        Stream.of(popupList)
                .forEach(p -> p.restorePosition());
    }

    public void setWindowPosition(View view, float xPosition, float yPosition) {

        //Count pixel position.
        offsetX = (int)(view.getHeight()* xPosition);
        offsetY = (int)(view.getWidth() * yPosition);
    }
}
