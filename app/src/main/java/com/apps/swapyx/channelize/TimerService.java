package com.apps.swapyx.channelize;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.apps.swapyx.channelize.Activities.MainActivity;
import com.apps.swapyx.channelize.Events.CountdownEvent;
import com.apps.swapyx.channelize.Events.PauseTimerEvent;
import com.apps.swapyx.channelize.Events.StartForegroundEvent;
import com.apps.swapyx.channelize.Events.StartTimerEvent;
import com.apps.swapyx.channelize.Events.StopTimerEvent;
import com.apps.swapyx.channelize.Utils.BusProvider;
import com.apps.swapyx.channelize.Utils.TimerProperties;
import com.apps.swapyx.channelize.Utils.AppNotifications;
import com.apps.swapyx.channelize.Utils.AppPreferences;
import com.squareup.otto.Subscribe;

import java.util.concurrent.TimeUnit;

import static android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP;


/**
 * Created by SwapyX on 13-06-2017.
 */

public class TimerService extends Service{
    //Constants
    private static final String TAG = TimerService.class.getSimpleName();
    private static final int MESSAGE_TIMER_UPDATE = 0 ;
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_COUNTDOWN_FINISHED =
            "com.apps.swapyx.channelize.ACTION_COUNTDOWN_FINISHED";
    public final static String ACTION_TIMERSERVICE_ALARM =
            "com.apps.swapyx.channelize.ACTION_TIMERSERVICE_ALARM";
    public static final String ACTION_WORK_MODE_STOPPED =
            "com.apps.swapyx.channelize.ACTION_WORK_MODE_STOPPED";
    public static final String SECONDS_WORKED = "secondsWorked";

    //Timer variables
    private long countdownDuration;
    private long timeRemainingOnPause;
    private long timeRemainingOnStop;

    // Load Settings
    private AppPreferences appPreferences;
    private SharedPreferences sPref;

    private LocalBroadcastManager mBroadcastManager;
    private BroadcastReceiver mAlarmReceiver;
    private AlarmManager mAlarmManager;

    //handler to post remaining time every second
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MESSAGE_TIMER_UPDATE){
                int remainingTime = getRemainingTime();
                if(remainingTime > 0){
                    BusProvider.getInstance().post(new CountdownEvent(remainingTime));
                    mHandler.sendEmptyMessageDelayed(MESSAGE_TIMER_UPDATE,1000);
                }else{
                    sendFinishedMessage();
                    stopTimer();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "Service onCreate");
        BusProvider.getInstance().register(this);
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        appPreferences = new AppPreferences(sPref);
        TimerProperties.getInstance().setTimerStatus(TimerStatus.STOPPED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        BusProvider.getInstance().unregister(this);
        try {
            this.unregisterReceiver(mAlarmReceiver);
        } catch(IllegalArgumentException e) {
            Log.d(TAG, "AlarmReceiver is already unregistered.");
        }
        mHandler.removeMessages(MESSAGE_TIMER_UPDATE);

        //save the seconds worked if the app is closed during an ongoing WORK mode
        TimerStatus status = TimerProperties.getInstance().getTimerStatus();
        if(StartTimerEvent.timerMode == TimerMode.WORK && status != TimerStatus.STOPPED){
            int secondsWorkedBeforeDestroyed = 0;
            int totalSeconds = (int) TimeUnit.MINUTES.toSeconds(appPreferences.getWorkDuration());
            if(status == TimerStatus.PAUSED){
               secondsWorkedBeforeDestroyed = totalSeconds - (int)TimeUnit.MILLISECONDS.toSeconds(timeRemainingOnPause);
            }else if(status == TimerStatus.RUNNING){
                secondsWorkedBeforeDestroyed = totalSeconds - getRemainingTime();
            }
            SharedPreferences.Editor ed = sPref.edit();
            if(secondsWorkedBeforeDestroyed>0){
                ed.putInt("secondsWorkedBeforeDestroyed", secondsWorkedBeforeDestroyed);
                ed.commit();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Subscribe
    public void onStartCommand(StartTimerEvent event){
        if (TimerProperties.getInstance().getTimerStatus() == TimerStatus.PAUSED){
            resumeTimer();
        }else{
            startTimer(StartTimerEvent.timerMode);
        }
    }

    @Subscribe
    public void onPauseCommand(PauseTimerEvent event){
        pauseTimer();
    }

    @Subscribe
    public void onStopCommand(StopTimerEvent event){
        stopTimer();
        if (StartTimerEvent.timerMode == TimerMode.WORK){
            //send seconds worked to TaskListFragment if work mode was stopped
            Intent workStoppedIntent = new Intent(ACTION_WORK_MODE_STOPPED);
            int secondsWorked = (int) (TimeUnit.MINUTES.toSeconds(appPreferences.getWorkDuration())
                    - TimeUnit.MILLISECONDS.toSeconds(timeRemainingOnStop));
            workStoppedIntent.putExtra(SECONDS_WORKED, secondsWorked);
            mBroadcastManager.sendBroadcast(workStoppedIntent);
        }
    }

    private void startTimer(TimerMode timerMode) {
        TimerProperties.getInstance().setTimerStatus(TimerStatus.RUNNING);
        countdownDuration = calculateCountdownDuration(timerMode);
        mHandler.sendEmptyMessage(MESSAGE_TIMER_UPDATE);
        Log.d("TimerService","Timer Start");
    }

    private void stopTimer() {
        if(TimerProperties.getInstance().getTimerStatus() == TimerStatus.PAUSED){
            timeRemainingOnStop = timeRemainingOnPause;
        }else {
            timeRemainingOnStop = TimeUnit.SECONDS.toMillis(getRemainingTime());
        }
        TimerProperties.getInstance().setTimerStatus(TimerStatus.STOPPED);
        mHandler.removeMessages(MESSAGE_TIMER_UPDATE);
        Log.d("TimerService","Timer Stopped");
    }

    public void pauseTimer() {
        timeRemainingOnPause = TimeUnit.SECONDS.toMillis(getRemainingTime());
        mHandler.removeMessages(MESSAGE_TIMER_UPDATE);
        TimerProperties.getInstance().setTimerStatus(TimerStatus.PAUSED);
        Log.d("TimerService","Timer Paused");
    }

    public void resumeTimer() {
        TimerProperties.getInstance().setTimerStatus(TimerStatus.RUNNING);
        countdownDuration = timeRemainingOnPause + SystemClock.elapsedRealtime();
        mHandler.sendEmptyMessage(MESSAGE_TIMER_UPDATE);
        Log.d("TimerService","Timer Resume");
    }

    private long calculateCountdownDuration(TimerMode timerMode) {
        long currentTime = SystemClock.elapsedRealtime();
        switch (timerMode){
            case WORK:
                return currentTime + TimeUnit.MINUTES.toMillis(appPreferences.getWorkDuration());
            case BREAK:
                return currentTime + TimeUnit.MINUTES.toMillis(appPreferences.getBreakDuration());
            case LONG_BREAK:
                return currentTime + TimeUnit.MINUTES.toMillis(appPreferences.getLongBreakDuration());
            default:
                throw new IllegalStateException("Invalid Mode");
        }

    }

    private int getRemainingTime(){
        return (int) TimeUnit.MILLISECONDS.toSeconds(countdownDuration - SystemClock.elapsedRealtime());
    }

    private void sendFinishedMessage() {
        Intent finishedIntent = new Intent(ACTION_COUNTDOWN_FINISHED);
        mBroadcastManager.sendBroadcast(finishedIntent);

        if(StartTimerEvent.timerMode == TimerMode.WORK){
            //send seconds worked to TaskListFragment on work mode completion
            Intent workFinishedIntent = new Intent(ACTION_WORK_MODE_STOPPED);
            int secondsWorked = (int) TimeUnit.MINUTES.toSeconds(appPreferences.getWorkDuration());
            workFinishedIntent.putExtra(SECONDS_WORKED,secondsWorked);
            mBroadcastManager.sendBroadcast(workFinishedIntent);
        }
    }

    @Subscribe
    public void onForegroundCommand(StartForegroundEvent event){
        if(StartForegroundEvent.startForeground){
            startForeground(NOTIFICATION_ID, AppNotifications.createForegroundNotifications(this) );
            if(TimerProperties.getInstance().isTimerRunning()){
                setAlarm();
                mHandler.removeMessages(MESSAGE_TIMER_UPDATE);
            }
            Log.d("TimerService","Foreground");
        }else{
            stopForeground(true);
            if (TimerProperties.getInstance().isTimerRunning()){
                cancelAlarm();
                mHandler.sendEmptyMessage(MESSAGE_TIMER_UPDATE);
            }
            Log.d("TimerService","Background");
        }
    }

    /*set an alarm if a session is active and the app if sent to background.
     And on receiving alarm send finished message*/
    private void setAlarm() {
        mAlarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendFinishedMessage();
                stopForeground(true);
                TimerProperties.getInstance().setTimerStatus(TimerStatus.STOPPED);

                if(appPreferences.resumeAppOnSessionEnd()){
                    //Resume the app when alarm goes OFF
                    Intent resumeIntent = new Intent(context, MainActivity.class);
                    resumeIntent.setFlags(Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(resumeIntent);
                }

                Log.d(TAG, "Countdown finished");
                unregisterReceiver(mAlarmReceiver);
            }
        };

        IntentFilter filter=new IntentFilter(ACTION_TIMERSERVICE_ALARM);
        registerReceiver(mAlarmReceiver, filter);

        mAlarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_TIMERSERVICE_ALARM);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        long wakeUpTime = TimeUnit.SECONDS.toMillis(getRemainingTime()) + SystemClock.elapsedRealtime();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAlarmManager.setExact(ELAPSED_REALTIME_WAKEUP, wakeUpTime, sender);
        } else {
            mAlarmManager.set(ELAPSED_REALTIME_WAKEUP, wakeUpTime, sender);
        }
        Log.d(TAG,"Alarm Set");

    }

    //cancel alarm if service is sent to background on resuming app
    private void cancelAlarm() {
        Intent intent = new Intent(ACTION_TIMERSERVICE_ALARM);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmManager.cancel(sender);
        Log.d(TAG,"Alarm Cancelled");
        try {
            this.unregisterReceiver(mAlarmReceiver);
        } catch(IllegalArgumentException e) {
            Log.d(TAG, "AlarmReceiver is already unregistered.");
        }
    }
}
