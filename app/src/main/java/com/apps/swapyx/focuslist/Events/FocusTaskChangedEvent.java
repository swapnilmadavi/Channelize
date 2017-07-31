package com.apps.swapyx.focuslist.Events;

import com.apps.swapyx.focuslist.ToDoItem;

/**
 * Created by SwapyX on 27-07-2017.
 */

public class FocusTaskChangedEvent {
    public static ToDoItem currentFocusTask = new ToDoItem(99999,"Free task");
    public static ToDoItem previousFocusTask;
    public FocusTaskChangedEvent(ToDoItem item) {
        if(item.getToDoId() != currentFocusTask.getToDoId()){
            previousFocusTask = currentFocusTask;
        }
        currentFocusTask = item;
    }
}
