package com.godzen.unreademailsformiband.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by godzen on 14/11/16.
 */
public class SettingsZen {
    private static SettingsZen ourInstance = new SettingsZen();

    public static SettingsZen getInstance() {
        return ourInstance;
    }

    private SettingsZen() {
    }

    public void setMiBandMAC(Context context, String mac)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("mibandMAC", mac);
        editor.apply();
    }

    public String getMiBandMAC(Context context)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString("mibandMAC", "");
    }

}
