// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * Activity to show details of am event
 */
public class ReadEventActivity extends AppCompatActivity
{
    /**
     * Format text as HTML Header: content
     * @param resID The textview id
     * @param header The header text
     * @param value The content text
     * @return The textview
     */
    private TextView setTextFormatted(int resID, String header, String value)
    {
        TextView textView = (TextView) findViewById(resID);
        textView.setText(Html.fromHtml("<b>" + header + ": </b>" + value));

        return textView;
    }
    private TextView date, place, address, phone, eventType, html_remain;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_event);

        setTitle(EventsFragment.readingEvent.title);
        // Set common details
        date = setTextFormatted(R.id.event_date, "Date", EventsFragment.readingEvent.getDateString());
        place = setTextFormatted(R.id.event_place, "Place", EventsFragment.readingEvent.getPlaceHTML());place.setMovementMethod(LinkMovementMethod.getInstance());
        address = setTextFormatted(R.id.event_address, "Address", EventsFragment.readingEvent.address);
        phone = setTextFormatted(R.id.event_phone, "Phone Number", EventsFragment.readingEvent.phoneNumber);
        eventType = setTextFormatted(R.id.event_type, "Event Type", EventsFragment.readingEvent.eventType);
        // Set the notes or description as HTML
        html_remain = (TextView) findViewById(R.id.html_remain);
        html_remain.setText(Html.fromHtml(EventsFragment.readingEvent.htmlRemain, new InternetHelper.UrlImageGetter(), null));
        html_remain.setMovementMethod(LinkMovementMethod.getInstance());

        // Support zoom
        int fontSize = SettingsFragment.getTextSize();
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        place.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        address.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        phone.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        eventType.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        html_remain.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

        // Give the option to add the event as a Calendar event
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final Context eventActivity = this;
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                EventsFragment.addCalendarEvent(EventsFragment.readingEvent, eventActivity);
            }
        });

        // TODO: Show a map with the event location pointed
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.zoom_menu, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int fontSize;
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.zoom_in:
                fontSize = SettingsFragment.incTextSize();
                date.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                place.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                address.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                phone.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                eventType.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                html_remain.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                return true;
            case R.id.zoom_out:
                fontSize = SettingsFragment.decTextSize();
                date.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                place.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                address.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                phone.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                eventType.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                html_remain.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
