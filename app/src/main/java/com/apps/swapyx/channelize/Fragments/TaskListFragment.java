package com.apps.swapyx.channelize.Fragments;


import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
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

import com.apps.swapyx.channelize.Adapters.ToDoListAdapter;
import com.apps.swapyx.channelize.Events.FocusTaskChangedEvent;
import com.apps.swapyx.channelize.Utils.BusProvider;
import com.apps.swapyx.channelize.Activities.AboutActivity;
import com.apps.swapyx.channelize.Activities.CompletedTasksActivity;
import com.apps.swapyx.channelize.Activities.MainActivity;
import com.apps.swapyx.channelize.Activities.SettingsActivity;
import com.apps.swapyx.channelize.Activities.TaskActivity;
import com.apps.swapyx.channelize.Events.CurrentTaskEditedEvent;
import com.apps.swapyx.channelize.Events.CurrentTaskCheckedEvent;
import com.apps.swapyx.channelize.Events.StartTimerEvent;
import com.apps.swapyx.channelize.R;
import com.apps.swapyx.channelize.TimerMode;
import com.apps.swapyx.channelize.TimerService;
import com.apps.swapyx.channelize.TimerStatus;
import com.apps.swapyx.channelize.ToDoItem;
import com.apps.swapyx.channelize.Utils.DBHelper;
import com.apps.swapyx.channelize.Utils.TimerProperties;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.apps.swapyx.channelize.Activities.TaskActivity.CHANGES_MADE;
import static com.apps.swapyx.channelize.Activities.TaskActivity.TASK_DUE_DATE;
import static com.apps.swapyx.channelize.Activities.TaskActivity.TASK_NAME;
import static com.apps.swapyx.channelize.Activities.TaskActivity.TASK_NOTE;
import static com.apps.swapyx.channelize.Utils.DBHelper.COLUMN_DUE_DATE;
import static com.apps.swapyx.channelize.Utils.DBHelper.COLUMN_NAME;
import static com.apps.swapyx.channelize.Utils.DBHelper.COLUMN_NOTE;
import static com.apps.swapyx.channelize.Utils.DBHelper.COLUMN_STATUS;
import static com.apps.swapyx.channelize.Utils.DBHelper.DML_INSERT;
import static com.apps.swapyx.channelize.Utils.DBHelper.DML_TYPE;
import static com.apps.swapyx.channelize.Utils.DBHelper.DML_UPDATE;
import static com.apps.swapyx.channelize.Utils.DBHelper.INCOMPLETE;

/**
 * A simple {@link Fragment} subclass.
 */
public class TaskListFragment extends Fragment {
    //Constants
    public static final int REQUEST_CODE_INSERT = 0;
    public static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_COMPLETED = 2;

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
    private SharedPreferences sPref;

    //Task list variables
    private int toBeEditedPosition;
    private int mCurrentFocusTaskPosition;
    private static final int NOT_DEFINED = 9999;
    private boolean mIsListEmpty;
    private boolean mCurrentTaskChecked;
    private boolean mTasksUnTicked = false;

    public TaskListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mDbHelper = new DBHelper(getActivity().getApplicationContext());
        mToDoList = new ArrayList<>();
        mAdapter = new ToDoListAdapter(mToDoList,getActivity());

        sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if(sPref.getBoolean("AppDestroyedDuringWork",false)){
            long id = sPref.getLong("focusTaskBeforeDestroy",NOT_DEFINED);
            int secondsWorked = sPref.getInt("secondsWorkedBeforeDestroyed",0);
            if(id != NOT_DEFINED){
                mDbHelper.updateTaskTime(id,secondsWorked);
                SharedPreferences.Editor ed = sPref.edit();
                ed.putBoolean("AppDestroyedDuringWork", false);
                ed.commit();
                Log.d("TaskList","Updated by" + String.valueOf(secondsWorked));
            }
        }

        mCurrentFocusTaskPosition = NOT_DEFINED;
        Log.d("CurrentFocusPos",String.valueOf(NOT_DEFINED));

        MainActivity.setFreeTaskSetListener(new MainActivity.FreeTaskListener() {
            @Override
            public void resetCurrentTaskPosition() {
                mCurrentFocusTaskPosition = NOT_DEFINED;
            }
        });
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
    public void onStop() {
        super.onStop();

        /*save the position of current task running before closing the app
        so that its seconds worked can be updated on next launch*/
        if(mCurrentFocusTaskPosition != NOT_DEFINED &&
                StartTimerEvent.timerMode == TimerMode.WORK &&
                TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED){
            SharedPreferences.Editor ed = sPref.edit();
            ed.putLong("focusTaskBeforeDestroy", mToDoList.get(mCurrentFocusTaskPosition).getToDoId());
            ed.commit();
        }
    }

    @Override
    public void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
        //unregister the broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("TaskListFragment","in onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            if(requestCode == REQUEST_CODE_COMPLETED){
                Log.d("mTasksUnTicked","true");
                mTasksUnTicked = true;
            } else if (requestCode == REQUEST_CODE_INSERT) {
                    ToDoItem newItem = new ToDoItem();
                    newItem.setToDoName(data.getStringExtra(TASK_NAME));
                    newItem.setToDoDueDate(data.getStringExtra(TASK_DUE_DATE));
                    newItem.setToDoNote(data.getStringExtra(TASK_NOTE));
                    long newRowID = mDbHelper.addTask(newItem);
                    newItem.setToDoId(newRowID);
                    mToDoList.add(0,newItem);
                    mAdapter.notifyItemInserted(0);
                    if(mCurrentFocusTaskPosition != NOT_DEFINED){
                        ++mCurrentFocusTaskPosition;
                    }
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.task_added, Toast.LENGTH_SHORT).show();
            } else if(requestCode == REQUEST_CODE_EDIT){
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
                                R.string.task_updated, Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getActivity().getApplicationContext(), R.string.no_change, Toast.LENGTH_SHORT).show();
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
            Intent completedTasksIntent = new Intent(getActivity(), CompletedTasksActivity.class);
            startActivityForResult(completedTasksIntent, REQUEST_CODE_COMPLETED);
            return true;
        }else if (id == R.id.action_settings) {
            // Launch settings activity
            Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }else if (id == R.id.action_about){
            // Launch about activity
            Intent aboutIntent = new Intent(getActivity(), AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        }else if (id == R.id.action_rate_app){
            rateThisApp();
        }else if (id == R.id.action_send_feedback) {
            sendFeedback();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setClickListeners() {
        //add task
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
            //edit task
            @Override
            public void editTask(int position) {
                if(position == mCurrentFocusTaskPosition &&
                        TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED){
                    Toast.makeText(getActivity(), R.string.cannot_edit, Toast.LENGTH_SHORT).show();
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

            //delete task
            @Override
            public void deleteTask(int position) {
                final int taskPosition = position;

                //if selected task is the current task and it is running
                if(taskPosition == mCurrentFocusTaskPosition &&
                        TimerProperties.getInstance().getTimerStatus() != TimerStatus.STOPPED){
                    Toast.makeText(getActivity(), R.string.cannot_delete, Toast.LENGTH_SHORT).show();
                }else{
                    AlertDialog.Builder deleteDialogOk = new AlertDialog.Builder(getActivity());
                    deleteDialogOk.setTitle(R.string.delete_task);
                    deleteDialogOk.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mDbHelper.deleteTask(mToDoList.get(taskPosition).getToDoId());
                            mToDoList.remove(taskPosition);
                            mAdapter.notifyItemRemoved(taskPosition);
                            Toast.makeText(getActivity().getApplicationContext(), R.string.task_deleted,
                                    Toast.LENGTH_SHORT).show();
                            if(mToDoList.isEmpty()){
                                setEmptyListText();
                            }
                            //if current task is deleted
                            if(taskPosition == mCurrentFocusTaskPosition){
                                mCurrentFocusTaskPosition = NOT_DEFINED;
                                ToDoItem item = new ToDoItem(99999,"Free task");
                                BusProvider.getInstance().post(new FocusTaskChangedEvent(item));
                            }else if(mCurrentFocusTaskPosition != NOT_DEFINED
                                    && taskPosition < mCurrentFocusTaskPosition){
                                //deleted task is above the current task in the list
                                --mCurrentFocusTaskPosition;
                            }
                        }
                    });
                    deleteDialogOk.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    deleteDialogOk.show();
                }
            }

            //mark a task as complete
            @Override
            public void markAsComplete(int position) {
                if(mCurrentFocusTaskPosition == position){
                    //marked task is the current task
                    BusProvider.getInstance().post(new CurrentTaskCheckedEvent());
                    mCurrentTaskChecked = true;
                    Log.d("currentChecked",String.valueOf(mCurrentTaskChecked));
                }else if(mCurrentFocusTaskPosition != NOT_DEFINED
                        && position < mCurrentFocusTaskPosition){
                    //marked task is above the current task in the list
                    --mCurrentFocusTaskPosition;
                    Log.d("new curr pos",String.valueOf(mCurrentFocusTaskPosition));
                }

                //Update the status of the task as completed
                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_STATUS,1);
                mDbHelper.updateTask(contentValues,mToDoList.get(position).getToDoId());

                //Remove the task from the ToDoList and add to CompletedList
                mToDoList.remove(position);
                mAdapter.notifyItemRemoved(position);
                Toast.makeText(getActivity().getApplicationContext(), R.string.marked_complete,
                        Toast.LENGTH_SHORT).show();
                if(mToDoList.isEmpty()){
                    setEmptyListText();
                }
            }


            //on single click on list item
            @Override
            public void setFocusTask(int position) {
                if(mCurrentFocusTaskPosition == position){
                    //current task is selected again
                    Toast.makeText(getActivity(), R.string.already_selected, Toast.LENGTH_SHORT).show();
                }else{
                    if(TimerProperties.getInstance().getTimerStatus() == TimerStatus.STOPPED){
                        //other task is selected
                        mCurrentFocusTaskPosition = position;
                        BusProvider.getInstance().post(new FocusTaskChangedEvent(mToDoList.get(position)));
                        Log.d("TaskListFragment pos",String.valueOf(mCurrentFocusTaskPosition));
                    }else{
                        //other task is selected when current task is active
                        Toast.makeText(getActivity(), R.string.stop_ongoing_task, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    // setup broadcast receiver for receiving the seconds worked after work session completion
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

    private void rateThisApp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getActivity().getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getActivity().getPackageName())));
        }
    }

    private void sendFeedback() {
        Intent feedback = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dev.swapyx@gmail.com"));
        feedback.putExtra(Intent.EXTRA_SUBJECT, "Feedback : Channelize app");
        try {
            startActivity(Intent.createChooser(feedback, "Complete action using"));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(getActivity(), "No email client installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void onWorkSessionFinished(int secondsWorked) {
        Log.d("TaskListFragment","in onWorkSessionFinished");
        ToDoItem item;

        if(mCurrentTaskChecked){
            //executed when running task is marked complete
            item = FocusTaskChangedEvent.previousFocusTask;
            Log.d("Checked Task",item.getToDoName());
            mCurrentFocusTaskPosition = NOT_DEFINED;
        }else {
            //executed when the work mode is stopped or completed
            item = mToDoList.get(mCurrentFocusTaskPosition);
        }

        //Update the seconds worked of the task
        mDbHelper.updateTaskTime(item.getToDoId(),secondsWorked);

        if(!mCurrentTaskChecked){
            //update the seconds worked and relay the changes in the list item
            int newSecondsWorked = item.getSecondsWorked();
            newSecondsWorked += secondsWorked;
            item.setSecondsWorked(newSecondsWorked);
            mToDoList.set(mCurrentFocusTaskPosition,item);
            mAdapter.notifyItemChanged(mCurrentFocusTaskPosition);
        }
        mCurrentTaskChecked = false;
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
                mTextEmptyList.setVisibility(View.GONE);

                mToDoList.clear();
                mToDoList.addAll(toDoItemList);
                mAdapter.notifyDataSetChanged();

                if(mTasksUnTicked){
                    /*update the current task's position on return from CompletedTasks activity
                    if some task is marked incomplete*/
                    Log.d("TaskListFragment","in 'if' of onPostExecute");
                    int position = NOT_DEFINED;
                    long id = FocusTaskChangedEvent.currentFocusTask.getToDoId();

                    //search the index of current task in mToDoList
                    for (ToDoItem item : mToDoList){
                        if(item.getToDoId() == id){
                            position = mToDoList.indexOf(item);
                        }
                    }
                    Log.d("pos after",String.valueOf(position));
                    //update the value of mCurrentFocusTaskPosition
                    if(position != NOT_DEFINED){
                        mCurrentFocusTaskPosition = position;
                        Log.d("TaskListFragment",String.valueOf(mCurrentFocusTaskPosition));
                    }
                    mTasksUnTicked = false;
                }
            }
        }
    }

}
