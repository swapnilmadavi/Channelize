package com.apps.swapyx.focuslist.Events;

/**
 * Created by SwapyX on 13-06-2017.
 */

public class CountdownEvent {
    public static int remainingTime;

    public CountdownEvent(int remainingTime) {
        CountdownEvent.remainingTime = remainingTime;
    }
}
