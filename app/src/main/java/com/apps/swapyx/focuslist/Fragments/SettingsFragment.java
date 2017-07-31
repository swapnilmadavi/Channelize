package com.apps.swapyx.focuslist.Fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.apps.swapyx.focuslist.R;

/**
 * Created by SwapyX on 28-06-2017.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_prefs);
    }
}
