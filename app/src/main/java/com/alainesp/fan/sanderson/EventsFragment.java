// This file is part of Fan of Sanderson app,
// Copyright (c) 2015-2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Fragment to show all events
 */
public class EventsFragment extends Fragment
{
    /**
     * The event currently showed to the user
     */
    public static DB.Event readingEvent;

    /**
     * An adapter to show events
     */
    private class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder>
    {
        private final List<DB.Event> mDataset;

        // Provide a reference to the views for each data item
        public class EventViewHolder extends RecyclerView.ViewHolder
        {
            final View eventGroup;
            final View event;
            final TextView title;
            final TextView date;
            final TextView categories;
            DB.Event dbEvent;
            final View eventLocation;
            final View eventAlarm;

            public EventViewHolder(View v)
            {
                super(v);
                eventGroup = v;
                title = (TextView) eventGroup.findViewById(R.id.post_title);
                date = (TextView) eventGroup.findViewById(R.id.post_date);
                categories = (TextView) eventGroup.findViewById(R.id.post_categories);
                event = eventGroup.findViewById(R.id.event_clickable);
                eventLocation = eventGroup.findViewById(R.id.event_location);
                eventAlarm = eventGroup.findViewById(R.id.event_alarm);
                // Show an event when clicked
                event.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (dbEvent != null)
                        {
                            if(!dbEvent.readed)
                                MainActivity.decBadgeNumber(MainActivity.APP_STATE_EVENTS);
                            dbEvent.setRead();
                            Intent intent = new Intent(v.getContext(), ReadEventActivity.class);
                            readingEvent = dbEvent;
                            setEvent(dbEvent);
                            startActivity(intent);
                        }
                    }
                });
                // Show google maps with the event address pointed
                eventLocation.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if (dbEvent != null)
                        {
                            // Map point based on address
                            // Uri location = Uri.parse("geo:0,0?q=1600+Amphitheatre+Parkway,+Mountain+View,+California");
                            Uri location = Uri.parse("geo:0,0?q=" + dbEvent.address.replace(' ', '+'));
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
                            if(MainActivity.isIntentSafe(mapIntent))
                                startActivity(mapIntent);
                        }
                    }
                });
                // Add the event as a Calendar event
                eventAlarm.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        EventsFragment.addCalendarEvent(dbEvent, MainActivity.staticRef);
                    }
                });
            }

            /**
             * Set the event to show for this ViewHolder
             * @param event The event to show
             */
            public void setEvent(DB.Event event)
            {
                dbEvent = event;

                if(event.readed)
                {
                    title.setText(event.title);
                    date.setText(event.getDateString());
                    categories.setText(event.eventType);
                }
                else// Highlight event when not readed
                {
                    title.setText(Html.fromHtml("<b>" + event.title + "</b>"));
                    date.setText(Html.fromHtml("<b>" + event.getDateString() + "</b>"));
                    categories.setText(Html.fromHtml("<b>" + event.eventType + "</b>"));
                }
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public EventsAdapter(List<DB.Event> myDataset)
        {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public EventViewHolder onCreateViewHolder(ViewGroup parent,  int viewType)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event, parent, false);
            return new EventViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(EventViewHolder holder, int position)
        {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.setEvent(mDataset.get(position));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount()
        {
            return mDataset.size();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.content_recycler_view, container, false);

        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.staticRef));
        mRecyclerView.setAdapter(new EventsAdapter(DB.Event.getEventsFromDB()));

        return rootView;
    }

    /**
     * Get the description of the event as Text.
     * @param htmlText The HTML description.
     * @return The description as a String
     */
    private static String parseHTMLText(String htmlText)
    {
        StringBuilder builder = new StringBuilder();

        try
        {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
            parser.setInput(new ByteArrayInputStream(htmlText.replace("&", "&#038;").getBytes()), null);

            // Begin parsing
            boolean complete_parsing = false;
            while (!complete_parsing)
            {
                int next_result = -1;

                try { next_result = parser.next(); }
                catch (Exception ignored){}

                switch (next_result)
                {
                    case XmlPullParser.START_TAG:  case XmlPullParser.END_TAG:
                    if ("p".equals(parser.getName()))
                        builder.append('\n');
                    break;
                    case XmlPullParser.TEXT:
                        builder.append(parser.getText());
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        complete_parsing = true;
                        break;
                }
            }
        }
        catch (Exception ignored) { }

        return builder.toString().trim();
    }

    /**
     * Add an event as a Calendar event.
     * @param event The event to add.
     * @param context Android context.
     */
    public static void addCalendarEvent(DB.Event event, Context context)
    {
        if (event != null)
        {
            Intent calendarIntent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
            calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.initialDate.getTime());
            if(event.endDate != null)
                calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endDate.getTime());
            calendarIntent.putExtra(CalendarContract.Events.TITLE, event.title + " " + event.eventType);
            calendarIntent.putExtra(CalendarContract.Events.DESCRIPTION, parseHTMLText(event.htmlRemain));
            calendarIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.address);

            if(MainActivity.isIntentSafe(calendarIntent))
                context.startActivity(calendarIntent);
        }
    }
}
