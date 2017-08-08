package com.apps.swapyx.channelize.Adapters;

import android.content.Context;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.apps.swapyx.channelize.ToDoItem;
import com.apps.swapyx.channelize.R;

import java.util.List;

/**
 * Created by SwapyX on 24-07-2017.
 */

public class CompletedTasksAdapter extends RecyclerView.Adapter<CompletedTasksAdapter.CompletedTasksViewHolder> {
    private List<ToDoItem> completedTasksArrayList;
    private Context context;
    private CompletedItemMenu listener;

    public interface CompletedItemMenu{
        void deleteTask(int position);
        void markAsIncomplete(int position);
    }

    public CompletedTasksAdapter(List<ToDoItem> completedTasksArrayList, Context context) {
        this.completedTasksArrayList = completedTasksArrayList;
        this.context = context;
    }

    // Assign the listener implementing events interface that will receive the events
    public void setCompletedItemMenuListener(CompletedItemMenu listener) {
        this.listener = listener;
    }

    @Override
    public CompletedTasksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_completed_list, parent, false);
        return new CompletedTasksViewHolder(itemView,listener);
    }

    @Override
    public void onBindViewHolder(CompletedTasksViewHolder holder, int position) {
        final ToDoItem toDoItem = completedTasksArrayList.get(position);
        final int itemPosition = position;
        holder.mTextName.setText(toDoItem.getToDoName());
        holder.mCheckStatus.setChecked(true);
    }

    @Override
    public int getItemCount() {
        return completedTasksArrayList.size();
    }

    public static class CompletedTasksViewHolder extends RecyclerView.ViewHolder {
        TextView mTextName;
        CheckBox mCheckStatus;
        private static CompletedItemMenu listener;

        public CompletedTasksViewHolder(View itemView, final CompletedItemMenu listener) {
            super(itemView);
            mTextName = (TextView)itemView.findViewById(R.id.text_completed_taskName);
            mCheckStatus = (CheckBox)itemView.findViewById(R.id.checkbox_completedTask_status);

            mTextName.setPaintFlags(mTextName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            CompletedTasksViewHolder.listener = listener;

            mCheckStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(!isChecked){
                        Log.d("Checkbox","Un-ticked ; Remove");
                        CompletedTasksViewHolder.listener.markAsIncomplete(getAdapterPosition());
                        mCheckStatus.setChecked(true);//Reset the checkbox of the View
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    listener.deleteTask(getAdapterPosition());
                    return false;
                }
            });
        }
    }
}
