package com.apps.swapyx.focuslist.Fragments;


import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.swapyx.focuslist.Activities.AboutActivity;
import com.apps.swapyx.focuslist.Activities.CompletedTasksActivity;
import com.apps.swapyx.focuslist.Activities.TaskActivity;
import com.apps.swapyx.focuslist.Adapters.ToDoListAdapter;
import com.apps.swapyx.focuslist.Events.CurrentTaskEditedEvent;
import com.apps.swapyx.focuslist.Events.FocusTaskChangedEvent;
import com.apps.swapyx.focuslist.Events.CurrentTaskCheckedEvent;
import com.apps.swapyx.focuslist.R;
import com.apps.swapyx.focuslist.TimerService;
import com.apps.swapyx.focuslist.TimerStatus;
import com.apps.swapyx.focuslist.ToDoItem;
import com.apps.swapyx.focuslist.Utils.AppPreferences;
import com.apps.swapyx.focuslist.Utils.BusProvider;
import com.apps.swapyx.focuslist.Utils.DBHelper;
import com.apps.swapyx.focuslist.Utils.TimerProperties;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.apps.swapyx.focuslist.Activities.TaskActivity.CHANGES_MADE;
import static com.apps.swapyx.focuslist.Activities.TaskActivity.TASK_DUE_DATE;
import static com.apps.swapyx.focuslist.Activities.TaskActivity.TASK_NAME;
import static com.apps.swapyx.focuslist.Activities.TaskActivity.TASK_NOTE;
import static com.apps.swapyx.focuslist.Utils.DBHelper.COLUMN_DUE_DATE;
import static com.apps.swapyx.focuslist.Utils.DBHelper.COLUMN_NAME;
import static com.apps.swapyx.focuslist.Utils.DBHelper.COLUMN_NOTE;
import static com.apps.swapyx.focuslist.Utils.DBHelper.COLUMN_SECONDS_WORKED;
import static com.apps.swapyx.focuslist.Utils.DBHelper.COLUMN_STATUS;
import static com.apps.swapyx.focuslist.Utils.DBHelper.DML_INSERT;
import static com.apps.swapyx.focuslist.Utils.DBHelper.DML_TYPE;
import static com.apps.swapyx.focuslist.Utils.DBHelper.DML_UPDATE;
import static com.apps.swapyx.focuslist.Utils.DBHelper.INCOMPLETE;

/**
 * A simple {@link Fragment} subclass.
 */
public class TaskListFragment extends Fragment {
    public static final int REQUEST_CODE_INSERT = 0;
    public static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_COMPLETED = 2;

    private static final String STRING_STOP_TASK_FIRST = "Stop task first";
    private static final String STRING_ANOTHER_TASK_ACTIVE = "Another task active";

    private TextView mTextEmptyList;
    private RecyclerView mRecyclerView;
    private ToDoListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<ToDoItem> mToDoList;
    private DBHelper mDbHelper;
    private FloatingActionButton mFab;
    private BroadcastReceiver mBroadcastReceiver;
    private View view;

    // Load Settings
    private AppPreferences appPreferences;
    private SharedPreferences sPref;

    int toBeEditedPosition;
    private boolean mIsListEmpty;
    private boolean mCurrentTaskChecked;
    private int mCurrentFocusTaskPosition;
    private static final int NOT_DEFINED = 9999;
    private boolean mTasksUnticked = false;

    public TaskListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mCurrentFocusTaskPosition = NOT_DEFINED;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_task_list, container, false);

        mTextEmptyList = (TextView) view.findViewById(R.id.text_empty_list);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView_main);
        mFab = (FloatingActionButton) view.findViewById(R.id.fab_add);

        mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mDbHelper = new DBHelper(getActivity().getApplicationContext());
        mToDoList = new ArrayList<>();
        mAdapter = new ToDoListAdapter(mToDoList,getActivity());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);
        setClickListeners();
        setupBroadcastReceiver();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TaskListFragment","OnResume called");
        //Load all saved Tasks
        new LoadTasks().execute();
    }

    @Override
    public void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        /*if(mCurrentFocusTaskPosition != NOT_DEFINED &&
                StartTimerEvent.timerMode == TimerMode.WORK &&
                TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED){
            SharedPreferences.Editor ed = sPref.edit();
            //int secondsWorkedBeforeDestroyed = sPref.getInt("secondsWorkedBeforeDestroyed",0);
            ed.putInt("focusTaskBeforeDestroy", mCurrentFocusTaskPosition);
            ed.commit();
        }*/
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("TaskListFragment","in onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            if(requestCode == REQUEST_CODE_COMPLETED){
                Log.d("mTasksUnticked","true");
                mTasksUnticked = true;
            }else{
                if (requestCode == REQUEST_CODE_INSERT) {
                    ToDoItem newItem = new ToDoItem();
                    newItem.setToDoName(data.getStringExtra(TASK_NAME));
                    newItem.setToDoDueDate(data.getStringExtra(TASK_DUE_DATE));
                    newItem.setToDoNote(data.getStringExtra(TASK_NOTE));
                    long newRowID = mDbHelper.addTask(newItem);
                    newItem.setToDoId(newRowID);
                    mToDoList.add(0,newItem);
                    mAdapter.notifyItemInserted(0);
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Task added", Toast.LENGTH_SHORT).show();
                }
                else if(requestCode == REQUEST_CODE_EDIT){
                    if(data.getBooleanExtra(CHANGES_MADE,false)){
                        String taskName = data.getStringExtra(TASK_NAME);
                        String taskDueDate = data.getStringExtra(TASK_DUE_DATE);
                        String taskNote = data.getStringExtra(TASK_NOTE);

                        ToDoItem toBeEditedItem = mToDoList.get(toBeEditedPosition);
                        toBeEditedItem.setToDoName(taskName);
                        toBeEditedItem.setToDoDueDate(taskDueDate);
                        toBeEditedItem.setToDoNote(taskNote);

                        ContentValues contentValues = new ContentValues();
                        contentValues.put(COLUMN_NAME,taskName);
                        contentValues.put(COLUMN_DUE_DATE,taskDueDate);
                        contentValues.put(COLUMN_NOTE,taskNote);

                        if(toBeEditedPosition == mCurrentFocusTaskPosition){
                            BusProvider.getInstance().post(new CurrentTaskEditedEvent(toBeEditedItem));
                        }

                        mDbHelper.updateTask(contentValues,mToDoList.get(toBeEditedPosition).getToDoId());
                        mToDoList.set(toBeEditedPosition,toBeEditedItem);
                        mAdapter.notifyItemChanged(toBeEditedPosition);
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Task updated", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getActivity().getApplicationContext(), "No changes made", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_tasklist,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_view_completed) {
            // Launch Completed Tasks activity
            Intent completedTasksIntent = new Intent(getActivity(),CompletedTasksActivity.class);
            startActivityForResult(completedTasksIntent,REQUEST_CODE_COMPLETED);
            return true;
        }else if (id == R.id.action_about){
            // Launch about activity
            Intent aboutIntent = new Intent(getActivity(), AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setClickListeners() {

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if(mIsListEmpty){
                    mTextEmptyList.setVisibility(View.GONE);
                    mIsListEmpty = false;
                }
                Intent taskIntent = new Intent(getActivity(),TaskActivity.class);
                taskIntent.putExtra(DML_TYPE, DML_INSERT);
                startActivityForResult(taskIntent,REQUEST_CODE_INSERT);
            }
        });

        mAdapter.setItemMenuListener(new ToDoListAdapter.ItemMenu() {
            @Override
            public void editTask(int position) {
                if(position == mCurrentFocusTaskPosition &&
                        TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED){
                    showSnackBar(STRING_STOP_TASK_FIRST);
                }else{
                    toBeEditedPosition = position;
                    ToDoItem item = mToDoList.get(toBeEditedPosition);
                    Intent taskIntent = new Intent(getActivity(),TaskActivity.class);
                    taskIntent.putExtra(TASK_NAME,item.getToDoName());
                    taskIntent.putExtra(TASK_DUE_DATE,item.getToDoDueDate());
                    taskIntent.putExtra(TASK_NOTE,item.getToDoNote());
                    taskIntent.putExtra(DML_TYPE, DML_UPDATE);
                    startActivityForResult(taskIntent,REQUEST_CODE_EDIT);
                }
            }

            @Override
            public void deleteTask(int position) {
                final int taskPosition = position;
                if(taskPosition == mCurrentFocusTaskPosition &&
                        TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED){
                    showSnackBar(STRING_STOP_TASK_FIRST);
                }else{
                    AlertDialog.Builder deleteDialogOk = new AlertDialog.Builder(getActivity());
                    deleteDialogOk.setTitle("Delete Task?");
                    deleteDialogOk.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mDbHelper.deleteTask(mToDoList.get(taskPosition).getToDoId());
                            mToDoList.remove(taskPosition);
                            mAdapter.notifyItemRemoved(taskPosition);
                            Toast.makeText(getActivity().getApplicationContext(), "Task deleted",
                                    Toast.LENGTH_SHORT).show();
                            if(mToDoList.isEmpty()){
                                setEmptyListText();
                            }
                            if(taskPosition == mCurrentFocusTaskPosition){
                                mCurrentFocusTaskPosition = NOT_DEFINED;
                                ToDoItem item = new ToDoItem("Free task");
                                BusProvider.getInstance().post(new FocusTaskChangedEvent(item));
                            }
                        }
                    });
                    deleteDialogOk.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    deleteDialogOk.show();
                }
            }

            @Override
            public void markAsComplete(int position) {
                if(mCurrentFocusTaskPosition == position){
                    BusProvider.getInstance().post(new CurrentTaskCheckedEvent());
                    mCurrentTaskChecked = true;
                }else if(position < mCurrentFocusTaskPosition && mCurrentFocusTaskPosition != NOT_DEFINED){
                    --mCurrentFocusTaskPosition;
                    Log.d("new curr pos",String.valueOf(mCurrentFocusTaskPosition));
                }

                //Update the status of the task as completed
                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_STATUS,1);
                mDbHelper.updateTask(contentValues,mToDoList.get(position).getToDoId());

                //Remove the task from the ToDolist and add to CompletedList
                //mCompletedList.add(item);
                mToDoList.remove(position);
                mAdapter.notifyItemRemoved(position);
                if(mToDoList.isEmpty()){
                    setEmptyListText();
                }
            }

            @Override
            public void setFocusTask(int position) {
                if(mCurrentFocusTaskPosition == position){
                    Toast.makeText(getActivity(), "Task already selected", Toast.LENGTH_SHORT).show();
                }else{
                    if(TimerProperties.getInstance().getTimerStatus() == TimerStatus.STOPPED){
                        mCurrentFocusTaskPosition = position;
                        BusProvider.getInstance().post(new FocusTaskChangedEvent(mToDoList.get(position)));
                        Log.d("TaskListFragment pos",String.valueOf(mCurrentFocusTaskPosition));

                    }else{
                        showSnackBar(STRING_ANOTHER_TASK_ACTIVE);
                    }
                }
            }
        });
    }


    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(mCurrentFocusTaskPosition != NOT_DEFINED){
                    onWorkSessionFinished(intent.getIntExtra(TimerService.SECONDS_WORKED,0));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerService.ACTION_WORK_MODE_STOPPED);
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver((mBroadcastReceiver), filter);

    }

    private void showSnackBar(String s) {
        Snackbar snackbar = Snackbar
                .make(view, s, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    private void onWorkSessionFinished(int secondsWorked) {
        ToDoItem item;
        if(mCurrentTaskChecked){
            item = FocusTaskChangedEvent.previousFocusTask;
            mCurrentFocusTaskPosition = NOT_DEFINED;
        }else {
            item = mToDoList.get(mCurrentFocusTaskPosition);
        }

        int newSecondsWorked = item.getSecondsWorked();
        newSecondsWorked += secondsWorked;
        item.setSecondsWorked(newSecondsWorked);

        //Update the seconds worked of the task
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SECONDS_WORKED,newSecondsWorked);
        mDbHelper.updateTask(contentValues,item.getToDoId());

        if(!mCurrentTaskChecked){
            mToDoList.set(mCurrentFocusTaskPosition,item);
            mAdapter.notifyItemChanged(mCurrentFocusTaskPosition);
            mCurrentTaskChecked = false;
        }
    }

    private void setEmptyListText() {
        mTextEmptyList.setVisibility(View.VISIBLE);
        mIsListEmpty = true;
    }

    private class LoadTasks extends AsyncTask<Object,Void,List<ToDoItem>> {

        @Override
        protected List<ToDoItem> doInBackground(Object... params) {
            return mDbHelper.getAllTasks(INCOMPLETE);
        }

        @Override
        protected void onPostExecute(List<ToDoItem> toDoItemList) {
            if(toDoItemList.isEmpty()){
                setEmptyListText();
            }else{
                mToDoList.clear();
                mToDoList.addAll(toDoItemList);
                mAdapter.notifyDataSetChanged();

                if(mTasksUnticked){
                    Log.d("TaskListFragment","in 'if' of onPostExecute");
                    int position = NOT_DEFINED;
                    long id = FocusTaskChangedEvent.currentFocusTask.getToDoId();
                    for (ToDoItem item : mToDoList){
                        if(item.getToDoId() == id){
                            position = mToDoList.indexOf(item);
                        }
                    }
                    Log.d("pos after",String.valueOf(position));
                    if(position != NOT_DEFINED){
                        mCurrentFocusTaskPosition = position;
                        Log.d("TaskListFragment",String.valueOf(mCurrentFocusTaskPosition));
                    }
                    mTasksUnticked = false;
                }
            }
        }
    }

}
