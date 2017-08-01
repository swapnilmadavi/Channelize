package com.apps.swapyx.focuslist.Fragments;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.apps.swapyx.focuslist.Activities.AboutActivity;
import com.apps.swapyx.focuslist.Events.CurrentTaskCheckedEvent;
import com.apps.swapyx.focuslist.Events.CountdownEvent;
import com.apps.swapyx.focuslist.Events.FocusTaskChangedEvent;
import com.apps.swapyx.focuslist.Events.PauseTimerEvent;
import com.apps.swapyx.focuslist.Events.StartTimerEvent;
import com.apps.swapyx.focuslist.Events.StopTimerEvent;
import com.apps.swapyx.focuslist.R;
import com.apps.swapyx.focuslist.Activities.SettingsActivity;
import com.apps.swapyx.focuslist.TimerMode;
import com.apps.swapyx.focuslist.TimerService;
import com.apps.swapyx.focuslist.ToDoItem;
import com.apps.swapyx.focuslist.Utils.AppNotifications;
import com.apps.swapyx.focuslist.Utils.AppPreferences;
import com.apps.swapyx.focuslist.Utils.BusProvider;
import com.apps.swapyx.focuslist.Utils.TimerProperties;
import com.pascalwelsch.holocircularprogressbar.HoloCircularProgressBar;
import com.squareup.otto.Subscribe;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static com.apps.swapyx.focuslist.TimerMode.BREAK;
import static com.apps.swapyx.focuslist.TimerMode.LONG_BREAK;
import static com.apps.swapyx.focuslist.TimerMode.WORK;

/**
 * A simple {@link Fragment} subclass.
 */
public class TimerFragment extends Fragment implements
        SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String TAG = TimerFragment.class.getSimpleName();
    private static final int NOTIFICATION_ID = 2;

    private int timerDuration;
    private TimerMode mTimerMode;

    private View view;
    private TextView mTextViewTimer;
    private HoloCircularProgressBar mCircularProgressBar;
    private Button mButtonStart, mButtonStop, mButtonPause, mButtonResume;

    // Load Settings
    private AppPreferences appPreferences;
    private SharedPreferences sPref;

    private boolean mIsTimerActive;
    private boolean timerDurationChanged = false;
    private BroadcastReceiver mBroadcastReceiver;
    private AlertDialog mAlertDialog;
    private int mPreviousRingerMode;
    private boolean mPreviousWifiMode;
    private boolean ringerModeChanged = false;
    private boolean wifiChanged = false;
    private static ChangeToolbarColor listener;
    private boolean screenKeptOn = false;

    public interface ChangeToolbarColor{
        void onTimerModeChanged(TimerMode mTimerMode);
    }

    public TimerFragment() {
        // Required empty public constructor
    }

    public static void setChangeToolbarColorListener(ChangeToolbarColor listener){
            TimerFragment.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.app_prefs, false);

        sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sPref.registerOnSharedPreferenceChangeListener(this);
        appPreferences = new AppPreferences(sPref);

        setupBroadcastReceiver();

        timerDuration = appPreferences.getWorkDuration();
        mIsTimerActive = false;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_timer, container, false);
        setScreenElements();
        initVisibility();
        setButtonListeners();

        return view;

    }

    @Override
    public void onStart() {
        super.onStart();
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        BusProvider.getInstance().unregister(this);
        if(mTimerMode == WORK && mIsTimerActive && FocusTaskChangedEvent.currentFocusTask.getToDoId()!=99999){
            SharedPreferences.Editor ed = sPref.edit();
            ed.putBoolean("AppDestroyedDuringWork", true);
            ed.commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mIsTimerActive && mTimerMode == WORK){
            if(ringerModeChanged){
                restoreSound();
            }
            if(wifiChanged){
                restoreWifi();
            }
        }
        sPref.unregisterOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_timer, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Launch settings activity
            Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }else if (id == R.id.action_about){
            // Launch about activity
            Intent aboutIntent = new Intent(getActivity(), AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        }else if (id == R.id.action_free_task){
            // Set Free task as current task
            if(!mIsTimerActive){
                BusProvider.getInstance()
                        .post(new FocusTaskChangedEvent(new ToDoItem(99999,"Free task")));
                Log.d("Free task","posted");
            }else {
                Snackbar snackbar = Snackbar
                        .make(view, R.string.stop_ongoing_task, Snackbar.LENGTH_SHORT);
                snackbar.show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onCountdownFinished();
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver((mBroadcastReceiver),
                new IntentFilter(TimerService.ACTION_COUNTDOWN_FINISHED)
        );

    }

    private void setScreenElements() {
        mCircularProgressBar = (HoloCircularProgressBar) view.findViewById(R.id.circular_progressBar);
        mTextViewTimer = (TextView)view.findViewById(R.id.text_timer);
        mButtonStart = (Button)view.findViewById(R.id.button_start);
        mButtonPause = (Button)view.findViewById(R.id.button_pause);
        mButtonResume = (Button)view.findViewById(R.id.button_resume);
        mButtonStop = (Button)view.findViewById(R.id.button_stop);

        updateTimerLabel((int) TimeUnit.MINUTES.toSeconds(timerDuration));
    }

    private void initVisibility() {
        mButtonStart.setVisibility(View.VISIBLE);
        mButtonStart.setEnabled(true);
        mButtonPause.setVisibility(View.INVISIBLE);
        mButtonPause.setEnabled(false);
        mButtonResume.setVisibility(View.INVISIBLE);
        mButtonResume.setEnabled(false);
        mButtonStop.setVisibility(View.INVISIBLE);
        mButtonStop.setEnabled(false);
        if(screenKeptOn){
            clearScreenOnFlag();
            screenKeptOn = false;
        }
    }

    private void setButtonListeners() {
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsTimerActive = true;
                startTimer(WORK);
                mButtonStart.setVisibility(View.GONE);
                mButtonStart.setEnabled(false);
                mButtonPause.setVisibility(View.VISIBLE);
                mButtonPause.setEnabled(true);
            }
        });

        mButtonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseSkip();
            }
        });

        mButtonResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BusProvider.getInstance().post(new StartTimerEvent(mTimerMode));
                mButtonResume.setVisibility(View.INVISIBLE);
                mButtonResume.setEnabled(false);
                mButtonStop.setVisibility(View.INVISIBLE);
                mButtonStop.setEnabled(false);
                mButtonPause.setVisibility(View.VISIBLE);
                mButtonPause.setEnabled(true);
            }
        });

        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
            }
        });
    }

    private void startTimer(TimerMode timerMode) {
        mTimerMode = timerMode;
        switch (timerMode){
            case WORK:
                BusProvider.getInstance().post(new StartTimerEvent(timerMode));
                if(appPreferences.getDisableSoundAndVibration()){
                    disableSoundAndVibration();
                }
                if(appPreferences.getDisableWifi()){
                    disableWifi();
                }
                if(appPreferences.keepScreenON()){
                    getActivity().getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
                    screenKeptOn = true;
                }
                break;
            case BREAK:
                BusProvider.getInstance().post(new StartTimerEvent(timerMode));
                break;
            case LONG_BREAK:
                BusProvider.getInstance().post(new StartTimerEvent(timerMode));
                break;
        }
    }

    private void pauseSkip() {
        if(mTimerMode == WORK){
            BusProvider.getInstance().post(new PauseTimerEvent());
            mButtonPause.setVisibility(View.INVISIBLE);
            mButtonPause.setEnabled(false);
            mButtonResume.setVisibility(View.VISIBLE);
            mButtonResume.setEnabled(true);
            mButtonStop.setVisibility(View.VISIBLE);
            mButtonStop.setEnabled(true);
        }else{
            BusProvider.getInstance().post(new StopTimerEvent());
            mTimerMode = WORK;
            view.setBackgroundColor(ContextCompat.getColor(getActivity(),R.color.colorPrimary));
            mCircularProgressBar.setProgressBackgroundColor(ContextCompat.getColor(getActivity(),R.color.colorPrimaryDark));
            listener.onTimerModeChanged(WORK);
            mButtonPause.setText(R.string.pause);
            initVisibility();
            updateLabelToWork();
        }
    }

    private void stopTimer(){
        mIsTimerActive = false;
        BusProvider.getInstance().post(new StopTimerEvent());
        initVisibility();
        if(timerDurationChanged) {
            timerDuration = appPreferences.getWorkDuration();
            timerDurationChanged = false;
        }
        updateTimerLabel((int) TimeUnit.MINUTES.toSeconds(timerDuration));
        if(ringerModeChanged){
            restoreSound();
            ringerModeChanged = false;
        }
        if(wifiChanged){
            restoreWifi();
            wifiChanged = false;
        }
    }

    private void disableSoundAndVibration() {
        AudioManager audioManager = (AudioManager) getActivity().getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        mPreviousRingerMode = audioManager.getRingerMode();
        if(mPreviousRingerMode != AudioManager.RINGER_MODE_SILENT){
            Log.d(TAG, "Disabling sound and vibration");
            ringerModeChanged = true;
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    private void disableWifi() {
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        mPreviousWifiMode = wifiManager.isWifiEnabled();
        if(mPreviousWifiMode){
            Log.d(TAG, "Disabling Wi-Fi");
            wifiChanged = true;
            wifiManager.setWifiEnabled(false);
        }
    }

    private void clearScreenOnFlag() {
        getActivity().getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
    }

    @Subscribe
    public void onCountDown(CountdownEvent event){
        updateTimerLabel(CountdownEvent.remainingTime);
    }

    @Subscribe
    public void onCurrentFocusTaskChecked(CurrentTaskCheckedEvent event){
        stopTimer();
    }


    private void loadBreakUI() {
        view.setBackgroundColor(ContextCompat.getColor(getActivity(),R.color.colorBreak));
        mCircularProgressBar.setProgressBackgroundColor(ContextCompat.getColor(getActivity()
                ,R.color.colorBreakDark));
        listener.onTimerModeChanged(BREAK);
        mButtonStart.setVisibility(View.GONE);
        mButtonStart.setEnabled(false);
        mButtonResume.setVisibility(View.INVISIBLE);
        mButtonResume.setEnabled(false);
        mButtonStop.setVisibility(View.INVISIBLE);
        mButtonStop.setEnabled(false);
        mButtonPause.setText(android.R.string.cancel);
        mButtonPause.setVisibility(View.VISIBLE);
        mButtonPause.setEnabled(true);
    }

    private void onCountdownFinished() {
        boolean sound = appPreferences.playNotificationSound();
        boolean vibrate = appPreferences.vibrateOnNotification();
        Notification finishNotification =
                AppNotifications.createFinishNotification(getActivity(), mTimerMode, sound, vibrate);
        notifyOnCompletion(finishNotification);
        acquireScreenWakelock();
        updateTimerLabel(0);
        if(ringerModeChanged){
            restoreSound();
            ringerModeChanged = false;
        }
        if(wifiChanged){
            restoreWifi();
            wifiChanged = false;
        }
        showAlertDialog(mTimerMode);
    }

    private void notifyOnCompletion(Notification finishNotification) {
        NotificationManager manager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(NOTIFICATION_ID,finishNotification);
    }

    private void acquireScreenWakelock() {
        PowerManager powerManager =
                (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        //noinspection deprecation
        PowerManager.WakeLock wl = powerManager.newWakeLock(
                SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE,
                "wake screen lock"
        );

        wl.acquire();
        wl.release();
    }


    private void restoreSound() {
        Log.d(TAG, "Restoring sound mode");
        AudioManager audioManager = (AudioManager)getActivity().getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        audioManager.setRingerMode(mPreviousRingerMode);
    }

    private void restoreWifi() {
        Log.d(TAG, "Restoring Wifi mode");
        WifiManager wifiManager = (WifiManager)getActivity().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(mPreviousWifiMode);
    }

    private void showAlertDialog(TimerMode timerMode) {
        switch (timerMode){
            case WORK:
                TimerProperties.getInstance().incrementNumberOfSessions();
                showBreakDialog();
                break;
            case BREAK:
                showWorkDialog();
                break;
            case LONG_BREAK:
                showWorkDialog();
                break;
            default:
                throw new IllegalStateException("Invalid Mode");
        }
    }

    private void showWorkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.break_over)
                .setMessage(R.string.resume_work)
                .setPositiveButton(R.string.resume, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCompletionNotification();
                        view.setBackgroundColor(ContextCompat.getColor(getActivity(),R.color.colorPrimary));
                        mCircularProgressBar.setProgressBackgroundColor(ContextCompat.getColor(getActivity()
                                ,R.color.colorPrimaryDark));
                        listener.onTimerModeChanged(WORK);
                        timerDuration = appPreferences.getWorkDuration();
                        mButtonPause.setText(R.string.pause);
                        startTimer(WORK);
                    }
                })
                .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCompletionNotification();
                        view.setBackgroundColor(ContextCompat.getColor(getActivity()
                                ,R.color.colorPrimary));
                        mCircularProgressBar.setProgressBackgroundColor(ContextCompat.getColor(getActivity()
                                ,R.color.colorPrimaryDark));
                        listener.onTimerModeChanged(WORK);
                        initVisibility();
                        mButtonPause.setText(R.string.pause);
                        updateLabelToWork();
                    }
                });

        mAlertDialog = builder.create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();
    }

    private void showBreakDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.work_over);

        if((TimerProperties.getInstance().getNumberOfSessions() %
                appPreferences.getSessionsBeforeLongBreak()) == 0){
            builder.setMessage(R.string.start_long_break)
                    .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeCompletionNotification();
                            timerDuration = appPreferences.getLongBreakDuration();
                            loadBreakUI();
                            startTimer(LONG_BREAK);
                        }
                    });
        }else {
            builder.setMessage(R.string.start_short_break)
                    .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeCompletionNotification();
                            timerDuration = appPreferences.getBreakDuration();
                            loadBreakUI();
                            startTimer(BREAK);
                        }
                    });
        }

        builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeCompletionNotification();
                startTimer(WORK);
            }
        })
                .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCompletionNotification();
                        initVisibility();
                        updateLabelToWork();
                    }
                });

        mAlertDialog = builder.create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();
    }

    private void removeCompletionNotification() {
        NotificationManager manager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
    }

    private void updateLabelToWork() {
        timerDuration = appPreferences.getWorkDuration();
        updateTimerLabel((int) TimeUnit.MINUTES.toSeconds(timerDuration));
    }

    public void updateTimerLabel(int remainingTime) {
        int minutes = remainingTime/60;
        int seconds = remainingTime%60;
        float progress = (float)remainingTime/(TimeUnit.MINUTES.toSeconds(timerDuration));
        String countDownTime = String.format("%s:%s", String.format(Locale.US, "%02d", minutes)
                , String.format(Locale.US, "%02d", seconds));
        mCircularProgressBar.setProgress(progress);
        mTextViewTimer.setText(countDownTime);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals("pref_work") ) {
            Log.i(TAG,"Focus Duration Changed");
            if(!mIsTimerActive){
                timerDuration = appPreferences.getWorkDuration();
                updateTimerLabel((int) TimeUnit.MINUTES.toSeconds(timerDuration));
            }
            else{
                timerDurationChanged = true;
            }
        }
    }
}
