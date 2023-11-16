package net.ednovak.reupload.remindme;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;


// Does not extend AppCompat because it will be a dailog (without any ActionBar)
public class Detail extends Activity {
    private final static String TAG = Detail.class.getName().toString();

    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent i = getIntent();
        name = i.getStringExtra("name");


        // Could be a function I guess (taking only the name to display)
        TextView title = (TextView)findViewById(R.id.detail_tv_title);
        title.setText(name);


        boolean itemExists = Main.checkItemExists(this, name);
        if(!itemExists){
            //Log.d(TAG, "Something is wrong!  There is no reminder with this name!");
            Toast.makeText(this, "Reminder Invalid", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        SharedPreferences sp = getSharedPreferences(Main.PREFS_DEADLINES, MODE_PRIVATE);
        long epoch = sp.getLong(name, -1);


        // Check if it's overdue
        long now = System.currentTimeMillis();
        if(now >= epoch){
            TextView deadline = (TextView)findViewById(R.id.detail_tv_deadline);
            deadline.setText("Overdue!");
            int red = Color.rgb(172, 28, 28);
            deadline.setTextColor(red);
            deadline.setTextSize(16);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("EEE, MMM dd\n h:mm a z yyyy");
        String dateString = formatter.format(new Date(epoch));

        TextView due = (TextView)findViewById(R.id.detail_tv_due);
        due.setText(dateString);


        sp = getSharedPreferences(Main.PREFS_NEXT_NOTIFICATION_TIMES, MODE_PRIVATE);
        TextView nextNoteTV = (TextView)findViewById(R.id.detail_tv_next_notification);
        long nextNotificationEpoch = sp.getLong(name, 0);
        if (nextNotificationEpoch != 0) {
            long delay = nextNotificationEpoch - System.currentTimeMillis();
            if (delay <= 0) {
                nextNoteTV.setText("(next notification very soon...)");
            } else {
                String s = "(next notification in " + AlarmHelper.msToMinutes(delay) + " minutes ...)";
                nextNoteTV.setText(s);
            }
        }
    }


    public void close(View v){
        this.finish();
    }

    public void delete(View v){
        final String name = ((TextView)v).getText().toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle("Delete This Reminder?");

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Main.deleteReminder(Detail.this, name);
                finish();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }
}
