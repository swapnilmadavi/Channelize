package com.apps.swapyx.channelize.Events;


import com.apps.swapyx.channelize.TimerMode;

/**
 * Created by SwapyX on 13-06-2017.
 */

public class StartTimerEvent {

    public static TimerMode timerMode;

    public StartTimerEvent(TimerMode timerMode) {
        StartTimerEvent.timerMode = timerMode;
    }
}
