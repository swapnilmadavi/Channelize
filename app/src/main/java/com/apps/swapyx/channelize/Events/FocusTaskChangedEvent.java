package com.apps.swapyx.channelize.Events;

import com.apps.swapyx.channelize.ToDoItem;

/**
 * Created by SwapyX on 27-07-2017.
 */

public class FocusTaskChangedEvent {
    public static ToDoItem currentFocusTask;
    public static ToDoItem previousFocusTask;

    public FocusTaskChangedEvent(ToDoItem item) {
        long id = item.getToDoId();
        if (currentFocusTask != null && id != currentFocusTask.getToDoId()) {
            previousFocusTask = currentFocusTask;
        }
        currentFocusTask = item;
    }
}
