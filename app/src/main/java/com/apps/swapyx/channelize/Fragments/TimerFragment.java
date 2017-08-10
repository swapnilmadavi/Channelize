package com.apps.swapyx.channelize.Fragments;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
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
import android.widget.Toast;

import com.apps.swapyx.channelize.Events.FocusTaskChangedEvent;
import com.apps.swapyx.channelize.Utils.BusProvider;
import com.apps.swapyx.channelize.Events.CurrentTaskCheckedEvent;
import com.apps.swapyx.channelize.Events.CountdownEvent;
import com.apps.swapyx.channelize.Events.PauseTimerEvent;
import com.apps.swapyx.channelize.Events.StartTimerEvent;
import com.apps.swapyx.channelize.Events.StopTimerEvent;
import com.apps.swapyx.channelize.R;
import com.apps.swapyx.channelize.Activities.SettingsActivity;
import com.apps.swapyx.channelize.TimerMode;
import com.apps.swapyx.channelize.TimerService;
import com.apps.swapyx.channelize.ToDoItem;
import com.apps.swapyx.channelize.Utils.AppNotifications;
import com.apps.swapyx.channelize.Utils.AppPreferences;
import com.apps.swapyx.channelize.Utils.TimerProperties;
import com.pascalwelsch.holocircularprogressbar.HoloCircularProgressBar;
import com.squareup.otto.Subscribe;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static com.apps.swapyx.channelize.TimerMode.BREAK;
import static com.apps.swapyx.channelize.TimerMode.LONG_BREAK;
import static com.apps.swapyx.channelize.TimerMode.WORK;

/**
 * A simple {@link Fragment} subclass.
 */
public class TimerFragment extends Fragment implements
        SharedPreferences.OnSharedPreferenceChangeListener{
    //Constants
    public static final String TAG = TimerFragment.class.getSimpleName();
    private static final int NOTIFICATION_ID = 2;

    private View view;
    private TextView mTextViewTimer;
    private HoloCircularProgressBar mCircularProgressBar;
    private Button mButtonStart, mButtonStop, mButtonPause, mButtonResume;
    private AlertDialog mAlertDialog;

    // Load Settings
    private AppPreferences appPreferences;
    private SharedPreferences sPref;

    //Timer variables
    private int timerDuration;
    private TimerMode mTimerMode;
    private boolean mIsWorkActive;
    private int mPreviousRingerMode;
    private boolean mPreviousWifiMode;
    private boolean ringerModeChanged = false;
    private boolean wifiChanged = false;
    private boolean screenKeptOn = false;

    private BroadcastReceiver mBroadcastReceiver;
    private static ChangeToolbarColor listener;


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

//        PreferenceManager.setDefaultValues(getActivity(), R.xml.app_prefs, false);

        sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sPref.registerOnSharedPreferenceChangeListener(this);
        appPreferences = new AppPreferences(sPref);

        setupBroadcastReceiver();

        timerDuration = appPreferences.getWorkDuration();
        mIsWorkActive = false;
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
        if(mTimerMode == WORK && mIsWorkActive && FocusTaskChangedEvent.currentFocusTask.getToDoId()!=99999){
            SharedPreferences.Editor ed = sPref.edit();
            ed.putBoolean("AppDestroyedDuringWork", true);
            ed.commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mAlertDialog != null){
            mAlertDialog.dismiss();
        }
        removeCompletionNotification();
        if(mIsWorkActive){
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
        }else if (id == R.id.action_free_task){
            // Set Free task as current task
            if(FocusTaskChangedEvent.currentFocusTask.getToDoId() == 99999){
                Toast.makeText(getActivity(),"Free task already selected",Toast.LENGTH_SHORT).show();
            }else {
                if(!mIsWorkActive){
                    BusProvider.getInstance()
                            .post(new FocusTaskChangedEvent(new ToDoItem(99999,"Free task")));
                    Log.d("Free task","posted");
                }else {
                    Toast.makeText(getActivity(),R.string.stop_ongoing_task,Toast.LENGTH_SHORT).show();
                }
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

        if (mTextViewTimer != null) {
            mTextViewTimer.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Light.ttf"));
        }

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
    }

    private void loadWorkUI(){
        mButtonStart.setVisibility(View.GONE);
        mButtonStart.setEnabled(false);
        mButtonPause.setVisibility(View.VISIBLE);
        mButtonPause.setEnabled(true);
    }

    private void setButtonListeners() {
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadWorkUI();
                startTimer(WORK);
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
                mIsWorkActive = true;
                timerDuration = appPreferences.getWorkDuration();
                BusProvider.getInstance().post(new StartTimerEvent(timerMode));
                listener.onTimerModeChanged(WORK);
                view.setBackgroundColor(ContextCompat.getColor(getActivity(),R.color.colorPrimary));
                mCircularProgressBar.setProgressBackgroundColor(ContextCompat.getColor(getActivity(),R.color.colorPrimaryDark));
                if(appPreferences.getDisableSoundAndVibration()){
                    disableSoundAndVibration();
                }
                if(appPreferences.getDisableWifi()){
                    disableWifi();
                }
                break;
            case BREAK:
                timerDuration = appPreferences.getBreakDuration();
                BusProvider.getInstance().post(new StartTimerEvent(timerMode));
                listener.onTimerModeChanged(BREAK);
                view.setBackgroundColor(ContextCompat.getColor(getActivity(),R.color.colorBreak));
                mCircularProgressBar.setProgressBackgroundColor(ContextCompat.getColor(getActivity()
                        ,R.color.colorBreakDark));
                break;
            case LONG_BREAK:
                timerDuration = appPreferences.getLongBreakDuration();
                BusProvider.getInstance().post(new StartTimerEvent(timerMode));
                listener.onTimerModeChanged(BREAK);
                view.setBackgroundColor(ContextCompat.getColor(getActivity(),R.color.colorBreak));
                mCircularProgressBar.setProgressBackgroundColor(ContextCompat.getColor(getActivity()
                        ,R.color.colorBreakDark));
                break;
        }
        if(appPreferences.keepScreenON()){
            getActivity().getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
            screenKeptOn = true;
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
        mIsWorkActive = false;
        BusProvider.getInstance().post(new StopTimerEvent());
        initVisibility();
        updateLabelToWork();
        if(ringerModeChanged){
            restoreSound();
            ringerModeChanged = false;
        }
        if(wifiChanged){
            restoreWifi();
            wifiChanged = false;
        }
        if(screenKeptOn){
            clearScreenOnFlag();
            screenKeptOn = false;
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
        mButtonStart.setVisibility(View.GONE);
        mButtonStart.setEnabled(false);
        mButtonResume.setVisibility(View.INVISIBLE);
        mButtonResume.setEnabled(false);
        mButtonStop.setVisibility(View.INVISIBLE);
        mButtonStop.setEnabled(false);
        mButtonPause.setText(R.string.skip);
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
        if(screenKeptOn){
            clearScreenOnFlag();
            screenKeptOn = false;
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
        initVisibility();
        switch (timerMode){
            case WORK:
                mIsWorkActive = false;
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
        mButtonPause.setText(R.string.pause);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.break_over)
                .setMessage(R.string.resume_work)
                .setPositiveButton(R.string.resume, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCompletionNotification();
                        loadWorkUI();
                        startTimer(WORK);
                    }
                })
                .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCompletionNotification();
                        updateLabelToWork();
                    }
                });

        mAlertDialog = builder.create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();
    }

    private void showBreakDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.work_session_complete);

        if((TimerProperties.getInstance().getNumberOfSessions() %
                appPreferences.getSessionsBeforeLongBreak()) == 0){
            builder.setMessage(R.string.start_long_break)
                    .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeCompletionNotification();
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
                            loadBreakUI();
                            startTimer(BREAK);
                        }
                    });
        }

        builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeCompletionNotification();
                loadWorkUI();
                startTimer(WORK);
            }
        })
                .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCompletionNotification();
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
            if(!mIsWorkActive){
                updateLabelToWork();
            }
        }
    }
}
