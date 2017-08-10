package com.apps.swapyx.channelize.Utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.apps.swapyx.channelize.Activities.MainActivity;
import com.apps.swapyx.channelize.Events.FocusTaskChangedEvent;
import com.apps.swapyx.channelize.TimerMode;
import com.apps.swapyx.channelize.R;

import static android.media.AudioAttributes.USAGE_ALARM;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by SwapyX on 29-06-2017.
 */

public class AppNotifications {

    public static Notification createForegroundNotifications(Context context) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(FocusTaskChangedEvent.currentFocusTask.getToDoName());

        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(context, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder.build();
    }

    public static Notification createFinishNotification(Context context, TimerMode timerMode, boolean sound, boolean vibrate) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getNotificationContentTitle(timerMode,context))
                .setContentText(getNotificationContentText(timerMode,context))
                .setAutoCancel(true);
        if(sound){
            Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (SDK_INT >= LOLLIPOP) {
                mBuilder.setSound(notificationSound, USAGE_ALARM);
            } else {
                mBuilder.setSound(notificationSound, AudioManager.STREAM_ALARM);
            }
        }
        if(vibrate){
            mBuilder.setVibrate(new long[]{0,300,200,500});
        }

        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(context, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder.build();
    }

    private static CharSequence getNotificationContentTitle(TimerMode timerMode, Context context) {
        CharSequence message;
        if(timerMode == TimerMode.WORK){
            message =  context.getString(R.string.work_session_complete);
        }else{
            message = context.getString(R.string.break_over);
        }

        return message;
    }

    private static CharSequence getNotificationContentText(TimerMode timerMode, Context context) {
        CharSequence message;
        if(timerMode == TimerMode.WORK){
            message =  context.getString(R.string.take_a_break);
        }else{
            message = context.getString(R.string.resume_work);
        }

        return message;
    }
}
