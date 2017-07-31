package com.apps.swapyx.focuslist.Utils;

import com.apps.swapyx.focuslist.TimerStatus;

/**
 * Created by SwapyX on 29-06-2017.
 */

public final class TimerProperties {
    private static TimerProperties timerProperties;
    private boolean isTimerRunning = false;
    private TimerStatus timerStatus = TimerStatus.STOPPED;
    private int mNumberOfSessions;
    private NumberOfSessionsChangeListener listener;

    public interface NumberOfSessionsChangeListener {
        public void onChange(int numberOfSessions);
    }

    public static TimerProperties getInstance() {
        if(timerProperties == null){
            timerProperties = new TimerProperties();
        }
        return timerProperties;
    }

    private TimerProperties(){

    }

    // Assign the listener implementing events interface that will receive the events
    public void setNumberOfSessionsChangeListener(NumberOfSessionsChangeListener listener) {
        this.listener = listener;
    }

    public void setTimerStatus(TimerStatus timerStatus) {
        this.timerStatus = timerStatus;
        if(timerStatus == TimerStatus.RUNNING){
            this.isTimerRunning = true;
        }else{
            this.isTimerRunning = false;
        }
    }

    public TimerStatus getTimerStatus() {
        return this.timerStatus;
    }

    public boolean isTimerRunning(){
        return this.isTimerRunning;
    }

    public void initNumberOfSessions(){
        this.mNumberOfSessions = 0;
    }

    public void incrementNumberOfSessions(){
        this.mNumberOfSessions++;
        listener.onChange(this.mNumberOfSessions);
    }

    public int getNumberOfSessions(){
        return this.mNumberOfSessions;
    }
}
