package com.apps.swapyx.channelize.Events;

/**
 * Created by SwapyX on 29-06-2017.
 */

public class StartForegroundEvent {
    public static boolean startForeground = false;

    public StartForegroundEvent(boolean b) {
        startForeground = b;
    }
}
