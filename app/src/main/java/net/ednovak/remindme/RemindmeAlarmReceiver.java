package net.ednovak.remindme;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;


public class RemindmeAlarmReceiver extends BroadcastReceiver {
    private final static String TAG = RemindmeAlarmReceiver.class.getName();
    public final static String NOTIFICATION_CHANNEL = "net.ednovak.remindme.channel0";
    public final static int PENDING_INTENT_FLAGS =   PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

    public RemindmeAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent origIntent) {
        Log.d(TAG, "Got intent in AlarmReceiver");

        String name = origIntent.getStringExtra("itemName");
        Log.d(TAG, "name in b-receiver: " + name + "  creating notification");



        // Build notification
        createNotificationChannel(context);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL);
        mBuilder.setSmallIcon(R.mipmap.note_trans);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.note);
        mBuilder.setLargeIcon(icon);
        mBuilder.setContentTitle(name);
        mBuilder.setContentText("Don't Forget!");
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mBuilder.setChannelId(NOTIFICATION_CHANNEL);


        // This is the action called when the notification is swiped away
        // or user clicks "Remind Me Later" button on notification itself
        // user is annoyed
        Intent iANNOY = new Intent(context, AlarmHelper.class);
        iANNOY.putExtra("itemName", name);
        PendingIntent piANNOY = PendingIntent.getBroadcast(context, Main.simpleHash(name), iANNOY, PENDING_INTENT_FLAGS);
        NotificationCompat.Action actionANNOY = new NotificationCompat.Action.Builder(R.drawable.cross, "Remind Me Later", piANNOY).build();
        mBuilder.addAction(actionANNOY); //
        mBuilder.setDeleteIntent(piANNOY); // Swipe the notification away is the same as "delete"


        // This is the action when the notification is opened
        Intent iOPEN = new Intent(context, Main.class);
        iOPEN.putExtra("itemName", name);
        PendingIntent openMain = PendingIntent.getActivity(context, Main.simpleHash(name), iOPEN, PENDING_INTENT_FLAGS);
        mBuilder.setContentIntent(openMain);


        // finish building it
        Notification n = mBuilder.build();
        n.flags |= Notification.FLAG_AUTO_CANCEL; // clears notification if they click / open it notification
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Check real quick that this thing still exists so we don't bother the user in case they
        // deleted it from the app!
        boolean itemExists = Main.checkItemExists(context, name);
        if(itemExists) {
            //Log.d(TAG, "Creating notification for real!");
            mNotificationManager.notify(Main.simpleHash(name), n);
        }

    }


    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "RemindMe";
            String description = "Reminder";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
