// This file is part of Fan of Sanderson app,
// Copyright (c) 2016 by Alain Espinosa.
// See LICENSE for details.

package com.alainesp.fan.sanderson;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String pref_key_wifi = "pref_key_wifi";
    private static final String pref_key_sync_schedule = "pref_key_sync_schedule";
    private static final String pref_key_text_size = "pref_key_text_size";
    private static final String pref_key_delete_tweets = "pref_key_delete_tweets";
    private static final String pref_key_debug = "pref_key_debug";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        ListPreference listPref = (ListPreference) findPreference(pref_key_sync_schedule);
        listPref.setSummary(listPref.getEntry());
        listPref = (ListPreference) findPreference(pref_key_delete_tweets);
        listPref.setSummary(listPref.getEntry());

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public static boolean getUseWifiOnly(Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(pref_key_wifi, false);
    }
    public static int getSyncScheduleInSeconds(Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(sharedPref.getString(pref_key_sync_schedule, "8")) * 60*60;
    }
    /**
     * Get the default font size
     * @return The font size
     */
    public static int getTextSize()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.staticRef);
        return sharedPref.getInt(pref_key_text_size, 16);
    }
    public static int decTextSize()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.staticRef);
        int textSize = sharedPref.getInt(pref_key_text_size, 16);

        textSize--;
        sharedPref.edit().putInt(pref_key_text_size, textSize).commit();

        return textSize;
    }
    public static int incTextSize()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.staticRef);
        int textSize = sharedPref.getInt(pref_key_text_size, 16);

        textSize++;
        sharedPref.edit().putInt(pref_key_text_size, textSize).commit();

        return textSize;
    }
    public static int getDeleteTweetsDaysInSeconds(Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(sharedPref.getString(pref_key_delete_tweets, "7")) * 24*60*60;
    }
    public static boolean getShowErrorMessages(Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(pref_key_debug, false);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals(pref_key_sync_schedule))
        {
            ListPreference listPref = (ListPreference) findPreference(key);
            listPref.setSummary(listPref.getEntry());
        }
        else if (key.equals(pref_key_delete_tweets))
        {
            ListPreference listPref = (ListPreference) findPreference(key);
            listPref.setSummary(listPref.getEntry());
        }
    }
}