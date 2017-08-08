package com.apps.swapyx.channelize;

/**
 * Created by SwapyX on 08-07-2017.
 */

public class ToDoItem {
    private long toDoId;
    private int secondsWorked;
    private int isComplete;
    private String toDoName;
    private String toDoNote;
    private String toDoDueDate;

    public ToDoItem() {

    }

    public ToDoItem(long id, String s) {
        this.toDoId = id;
        this.toDoName = s;
    }

    public void setToDoId(long toDoId) {
        this.toDoId = toDoId;
    }

    public long getToDoId() {
        return this.toDoId;
    }

    public void setToDoName(String toDoName) {
        this.toDoName = toDoName;
    }

    public String getToDoName() {
        return this.toDoName;
    }

    public void setToDoNote(String toDoNote){
        this.toDoNote = toDoNote;
    }

    public String getToDoNote(){
        return this.toDoNote;
    }

    public void setToDoDueDate(String toDoDueDate){
        this.toDoDueDate = toDoDueDate;
    }

    public String getToDoDueDate(){
        return this.toDoDueDate;
    }

    public void setToDoStatus(boolean isComplete){
        if (isComplete){
            this.isComplete = 1;
        }else{
            this.isComplete = 0;
        }
    }

    public int getToDoStatus(){
        return this.isComplete;
    }

    public void setSecondsWorked(int secondsWorked){
        this.secondsWorked = secondsWorked;
    }

    public int getSecondsWorked(){
        return this.secondsWorked;
    }

}
