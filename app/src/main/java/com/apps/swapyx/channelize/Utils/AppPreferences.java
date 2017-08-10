package com.apps.swapyx.channelize.Utils;

import android.content.SharedPreferences;

/**
 * Created by SwapyX on 17-06-2017.
 */

public class AppPreferences {
    private static final String WORK = "pref_work";
    private static final String BREAK = "pref_break";
    private static final String LONG_BREAK = "pref_longBreak";
    private static final String SESSIONS = "pref_sessions";
    private static final String DISABLE_WIFI = "pref_wifi";
    private static final String DISABLE_SOUND_VIBRATION = "pref_ringerMode";
    private static final String NOTIFICATION_SOUND = "pref_sound";
    private static final String NOTIFICATION_VIBRATE = "pref_vibrate";
    private static final String KEEP_SCREEN_ON = "pref_screen_on";
    private static final String RESUME_APP = "pref_resume_app";

    private SharedPreferences sharedPreferences;

    public AppPreferences(SharedPreferences sPref) {
        sharedPreferences = sPref;
    }

    public int getWorkDuration() {
        return sharedPreferences.getInt(WORK,25);
    }

    public int getBreakDuration() {
        return sharedPreferences.getInt(BREAK,5);
    }

    public int getLongBreakDuration() {
        return sharedPreferences.getInt(LONG_BREAK,15);
    }

    public int getSessionsBeforeLongBreak() {
        return sharedPreferences.getInt(SESSIONS,4);
    }

    public boolean getDisableSoundAndVibration() {
        return sharedPreferences.getBoolean(DISABLE_SOUND_VIBRATION,false);
    }

    public boolean getDisableWifi() {
        return sharedPreferences.getBoolean(DISABLE_WIFI,true);
    }

    public boolean playNotificationSound() {
        return sharedPreferences.getBoolean(NOTIFICATION_SOUND,true);
    }

    public boolean vibrateOnNotification() {
        return sharedPreferences.getBoolean(NOTIFICATION_VIBRATE,true);
    }

    public boolean keepScreenON() {
        return sharedPreferences.getBoolean(KEEP_SCREEN_ON,false);
    }

    public boolean resumeAppOnSessionEnd() {
        return sharedPreferences.getBoolean(RESUME_APP,true);
    }
}
