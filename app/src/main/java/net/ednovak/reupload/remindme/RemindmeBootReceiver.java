package net.ednovak.reupload.remindme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class RemindmeBootReceiver extends BroadcastReceiver {
    private final static String TAG = RemindmeBootReceiver.class.getName();
    public RemindmeBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        //Log.d(TAG, "RemindmeBootReciever Launched!!");
        //Toast.makeText(context, "RemindmeBootReceiver!", Toast.LENGTH_LONG).show();
        // Turn on their alarms


        SharedPreferences sp = context.getSharedPreferences(Main.PREFS_DEADLINES, Context.MODE_PRIVATE);

        Object[] items = sp.getAll().keySet().toArray();

        for(int i = 0; i < items.length; i++){
            //Log.d(TAG, "setting alarm for: " + items[i].toString() + "  in boot receiver");
            String name = items[i].toString();
            long finalEpoch = sp.getLong(name, -1);
            long nextAlarmEpoch = AlarmHelper.getNextTime(finalEpoch);
            AlarmHelper.setAlarm(context, items[i].toString(), nextAlarmEpoch);
            //Log.d(TAG, "setting alarm at: " + nextAlarmEpoch);
        }
    }
}
