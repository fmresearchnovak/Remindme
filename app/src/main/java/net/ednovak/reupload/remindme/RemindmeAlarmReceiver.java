package net.ednovak.reupload.remindme;


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
import android.os.Bundle;

import android.support.v4.app.NotificationCompat;


public class RemindmeAlarmReceiver extends BroadcastReceiver {
    private final static String TAG = RemindmeAlarmReceiver.class.getName();
    public final static String NOTIFICATION_CHANNEL = "net.ednovak.reupload.remindme.channel0";

    public RemindmeAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "Got intent in AlarmReceiver");

        String name = intent.getStringExtra("itemName");
        //Log.d(TAG, "name in b-receiver: " + name + "  creating notification");



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



        PendingIntent piANNOY = genActionIntent(context, name);
        NotificationCompat.Action actionANNOY = new NotificationCompat.Action.Builder(R.drawable.cross, "Remind Me Later", piANNOY).build();
        mBuilder.addAction(actionANNOY);

        mBuilder.setDeleteIntent(piANNOY); // Swipe the notification away is the same as "delete"


        Intent i = new Intent(context, Main.class);
        i.putExtra("itemName", name);
        PendingIntent openMain = PendingIntent.getActivity(context, Main.simpleHash(name), i, 0);
        mBuilder.setContentIntent(openMain);

        Notification n = mBuilder.build();
        n.flags |= Notification.FLAG_AUTO_CANCEL; // Cancels if they click the notification


        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        // Check real quick that this thing still exists so we don't bother the user in case they
        // deleted it from the app!
        boolean itemExists = Main.checkItemExists(context, name);
        if(itemExists) {
            //Log.d(TAG, "Creating notification for real!");
            mNotificationManager.notify(Main.simpleHash(name), n);
        }

    }


    // Used to direct the user to main activity (and tell main activity which item this is)
    // when they click the BUTTON on the notification
    private static PendingIntent genActionIntent(Context context, String name){
        Intent intent = new Intent(context, AlarmHelper.class);
        Bundle extras = new Bundle();
        extras.putString("itemName", name);
        //extras.putBoolean("annoyed", annoyed);
        intent.putExtras(extras);
        int code = (true) ? 1 : 0;
        // getBroadcast will return the same pending intent if everything is the same as last time
        // extras are NOT CONSIDERED in the comparison
        // So, if you try to create two pendingintents, but all of the inputs below match'
        // You will not create a new pendingintent at all
        // http://stackoverflow.com/questions/20204284/is-it-possible-to-create-multiple-pendingintents-with-the-same-requestcode-and-d
        // The code int takes care of that problem for me.
        // I'm not sure how code solves this problem (it used to somtimes be false, now it's always true.  It still solves the problem though.
        PendingIntent pIntent = PendingIntent.getBroadcast(context, Main.simpleHash(name) + code, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
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
