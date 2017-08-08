package com.apps.swapyx.channelize.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.apps.swapyx.channelize.R;
import com.apps.swapyx.channelize.ToDoItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by SwapyX on 08-07-2017.
 */

public class ToDoListAdapter extends RecyclerView.Adapter<ToDoListAdapter.ToDoListViewHolder> {
    private List<ToDoItem> toDoItemArrayList;
    private Context context;
    private ItemMenu listener;

    public interface ItemMenu{
        void editTask(int position);
        void deleteTask(int position);
        void markAsComplete(int position);
        void setFocusTask(int position);
    }

    public ToDoListAdapter(List<ToDoItem> toDoItemArrayList, Context context) {
        this.toDoItemArrayList = toDoItemArrayList;
        this.context = context;
    }

    // Assign the listener implementing events interface that will receive the events
    public void setItemMenuListener(ItemMenu listener) {
        this.listener = listener;
    }

    @Override
    public ToDoListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_todo_list, parent, false);
        return new ToDoListViewHolder(itemView,context,listener);
    }

    @Override
    public void onBindViewHolder(ToDoListViewHolder holder, int position) {
        final ToDoItem toDoItem = toDoItemArrayList.get(position);
        //final int itemPosition = position;
        holder.mTextName.setText(toDoItem.getToDoName());
        String dueDate = toDoItem.getToDoDueDate();
        if(dueDate.equals("")){
            if(holder.mTextColorChanged){
                Log.d("TAG Due Date","Color changed back");
                holder.mTextDueDate.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                holder.mTextColorChanged = false;
            }
            holder.mTextDueDate.setText(R.string.no_due_date);
        }else{
            if(isLate(dueDate)){
                holder.mTextDueDate.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
                holder.mTextColorChanged = true;
                Log.d("TAG Due Date","Color changed");
            }else {
                if(holder.mTextColorChanged){
                    Log.d("TAG Due Date","Color changed back");
                    holder.mTextDueDate.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                    holder.mTextColorChanged = false;
                }
            }
            if(dueDate.equals(getToday())){
                holder.mTextDueDate.setText(R.string.today);
            }else {
                holder.mTextDueDate.setText(dueDate);
            }
        }

        int totalSecondsWorked = toDoItem.getSecondsWorked();
        int hours = totalSecondsWorked/(3600);
        if(hours>0){
            totalSecondsWorked = totalSecondsWorked % (3600);
        }
        int minutes = totalSecondsWorked/60;
        int seconds = totalSecondsWorked%60;
        String timeWorked = "";
        if(hours > 0){
            timeWorked = String.format("%s:%s:%s", String.format(Locale.US, "%02d", hours)
                    , String.format(Locale.US, "%02d", minutes)
                    , String.format(Locale.US, "%02d", seconds));
        }else{
            timeWorked = String.format("%s:%s", String.format(Locale.US, "%02d", minutes)
                    , String.format(Locale.US, "%02d", seconds));
        }
        holder.mTextNumSessions.setText(timeWorked);
    }

    @Override
    public int getItemCount() {
        return toDoItemArrayList.size();
    }

    private boolean isLate(String dueDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date today = calendar.getTime();

        Date testDate;
        SimpleDateFormat formatter=new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);
        try {
            testDate = formatter.parse(dueDate);
            Log.d("Due Date", String.valueOf(testDate));
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return today.after(testDate);
    }

    private String getToday() {
        Date today = Calendar.getInstance().getTime();
        String myFormat = "EEE, dd MMM yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(myFormat, Locale.US);
        return simpleDateFormat.format(today);
    }

    public static class ToDoListViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;
        private TextView mTextName;
        private TextView mTextDueDate;
        private TextView mTextNumSessions;
        private CheckBox mCheckStatus;
        private boolean mTextColorChanged = false;
        private static ItemMenu listener;

        public ToDoListViewHolder(View itemView, Context context, final ItemMenu listener) {
            super(itemView);
            mContext = context;
            ToDoListViewHolder.listener = listener;
            mTextName = (TextView) itemView.findViewById(R.id.text_task_name);
            mTextDueDate = (TextView) itemView.findViewById(R.id.text_due_date);
            mTextNumSessions = (TextView) itemView.findViewById(R.id.text_num_sessions);
            mCheckStatus = (CheckBox) itemView.findViewById(R.id.checkbox_task_status);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.setFocusTask(getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showPopupMenu(v);
                    return false;
                }
            });

            mCheckStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        Log.d("Checkbox","Ticked ; Remove");
                        ToDoListViewHolder.listener.markAsComplete(getAdapterPosition());
                        mCheckStatus.setChecked(false);//Reset the checkbox of the View
                    }
                }
            });
        }

        private void showPopupMenu(View view) {
            final CharSequence[] items = {"Edit", "Delete"};
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

            builder.setTitle(mTextName.getText());
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int position = getAdapterPosition();
                    switch (which){
                        case 0://Edit
                            listener.editTask(position);
                            break;
                        case 1://Delete
                            listener.deleteTask(position);
                            break;
                    }
                }
            });
            builder.show();
        }
    }
}
