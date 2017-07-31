package com.apps.swapyx.focuslist.Events;

import com.apps.swapyx.focuslist.ToDoItem;

/**
 * Created by SwapyX on 29-07-2017.
 */

public class CurrentTaskEditedEvent {
    public static ToDoItem editedItem;
    public CurrentTaskEditedEvent(ToDoItem editedItem) {
        CurrentTaskEditedEvent.editedItem = editedItem;
    }
}
