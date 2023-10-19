package net.ednovak.reupload.remindme;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.util.Random;

/**
 * Created by ed on 12/28/16.
 */

public class AlarmHelper extends BroadcastReceiver {
    private final static String TAG = AlarmHelper.class.getName();

    public AlarmHelper(){ }

    // Some convenient times
    public final static long HOURS_2 = 1000 * 60 * 60 * 2;
    public final static long HOURS_3 = 1000 * 60 * 60 * 3;
    //public final static long HOURS_24 = 1000 * 60 * 60 * 24;
    //public final static long TIME_SHORT = 1000 * 60 * 25; // 25 minutes
    //public final static long TIME_SHORT = 2000; // Two seconds (for debugging)
    //public final static long TIME_LONG = 1000 * 60 * 100; // 100 minutes
    //public final static long TIME_LONG = 10000; // ten seconds (for debugging)
    //public final static long MINUTES_10 = 1000 * 60 * 10;

    //public final static long MINUTES_2 = 1000 * 60 * 2;
    //public final static long MINUTES_1 = 1000 * 60 * 2;

    public final static long SECONDS_7 = 1000 * 7;
    //public final static long SECONDS_5 = 1000 * 5;

    public static long msToMinutes(long ms){
        Log.d(TAG, "ms to minutes: ");
        return ms / 1000 / 60;
    }

    // dueTime is the epoch (upper limit) which we cannot pass)
    public static long getNextTime(long dueTime){
        long now = AlarmHelper.now();
        long maxDur = dueTime - now;
        maxDur = Math.min(maxDur, HOURS_2);

        if(maxDur < 1000){
            // For really short (or past) deadlines
            // just notify in 2 seconds
            long newTime = now+2000;
            Log.d(TAG, "delta: " + 2000 + "   next notification time: " + newTime);
            return newTime;
        }

        Random r = new Random();
        long delta = (long)(r.nextFloat() * maxDur);
        long newTime = now + delta;

        Log.d(TAG, "delta: " + delta + "   next notification time: " + newTime);
        return newTime;
    }


    /*
    // dueTime is the epoch (upper limit) which we cannot pass)
    public static long getNextTime(long dueTime){
        Log.d(TAG, "Requesting new notification time   dueTime: " + dueTime);
        //System.out.println("Requesting new notification time   dueTime: " + dueTime);
        long now = AlarmHelper.now();
        long maxDur = dueTime - now;
        if(maxDur < 5000){
            // less than 5 seconds from now (maybe negative)
            // just notify one second from now
            Log.d(TAG, "next notification time: " + (now + 1000));
            //System.out.println("next notification time: " + (now + 1000));
            return now + 1000;
        }
        long newTime = now + fibUnderMax(maxDur);
        newTime = newTime - 20000; // Reduce by 20 seconds
        // this is important because it gives the user some time
        // to dismiss the notification, thereby setting the next
        // notification time.  If we don't do this, then the notification
        // will occur at the exact some moment that the next one would have
        // occured
        Log.d(TAG, "next notification time: " + newTime);
        //System.out.println("next notification time: " + newTime);
        return newTime;
    }
    */


    /*
    |------------------- 33 -----------------|
    now 	                           dueTime
    |13             8        5     3   2  1 1|
    */

    // Returns the largest fib number less than or equal to the given max
    public static long fibUnderMax(long max){
        long yesterday = 1;
        long today = 1;
        long tomorrow = 2;

        long accum = 0;
        while(accum < max){
            accum = accum + tomorrow;

            tomorrow = today + yesterday;
            //System.out.println("tomorrow: " + tomorrow);

            yesterday = today;
            today = tomorrow;
        }
        return yesterday;
    }

    public static long now(){
        return System.currentTimeMillis();
    }


    // @epoch Time for alarm to go off.  Consider  using getNextTime()
    public static void setAlarm(Context ctx, String name, long epoch){
        // Creating Alarm
        AlarmManager alarmMgr = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, RemindmeAlarmReceiver.class);

        //Log.d(TAG, "name for alarm: " + name);
        intent.putExtra("itemName", name);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(ctx, Main.simpleHash(name), intent, FLAG_IMMUTABLE);

        // Check if preference is still there (or if this event was deleted
        boolean itemExists = Main.checkItemExists(ctx, name);
        if(itemExists){
            //Log.d(TAG, "Setting alarm for " + epoch);
            alarmMgr.set(AlarmManager.RTC_WAKEUP, epoch, alarmIntent);

            // Private means only this app can access
            SharedPreferences sp = ctx.getSharedPreferences(Main.PREFS_NEXT_NOTIFICATION_TIMES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            // need to check if there is already a reminder with this name!
            editor.putLong(name, epoch);
            editor.commit();

        } else {
            //Log.d(TAG, "This item has been deleted.  Don't set anymore alarms!");
        }
    }


    public void onReceive(Context context, Intent intent){
        //Log.d(TAG, "AlarmHelper got an intent!");

        Bundle extras = intent.getExtras();
        String name = extras.getString("itemName");
        //boolean annoyed = extras.getBoolean("annoyed");
        //Log.d(TAG, "Got intent!  annoyed: " + annoyed + "  name: " + name);

        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(Main.simpleHash(name));


        // Get the duetime epoch from the name / preferences file
        SharedPreferences sp = context.getSharedPreferences(Main.PREFS_DEADLINES, Context.MODE_PRIVATE);
        long endEpoch = sp.getLong(name, -1);
        long nextAlarmEpoch = getNextTime(endEpoch);

        setAlarm(context, name, nextAlarmEpoch);

    }
}
