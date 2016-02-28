// This file is part of Fan of Sanderson app,
// Copyright (c) 2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment to show The Way of Kings Reread.
 */
public class TorWoKRereadFragment extends Fragment
{
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.content_todo, container, false);

        TextView message = (TextView) rootView.findViewById(R.id.todo_message);
        message.setText(Html.fromHtml("This reread is complete so <em>'Fan of Sanderson'</em> app don't retrieve new data. If you want to read it use a web browser with the link that just open."));
        message.setTextColor(Color.BLACK);

        return rootView;

    }
}
