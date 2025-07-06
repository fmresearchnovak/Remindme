package net.ednovak.remindme;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class Main extends AppCompatActivity {
    private final static String TAG = Main.class.getName();

    public final static String PREFS_DEADLINES = "net.ednovak.remindme.preferences.names";
    public final static String PREFS_NEXT_NOTIFICATION_TIMES = "net.ednovak.remindme.preferences.times";

    private String tmpName;
    private int tmpYear;
    private int tmpMonth;
    private int tmpDay;
    private int tmpHour;
    private int tmpMinute;
    private long tmpGoalEpoch;



    Context ctx;

    private ImageView sun;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ActionBar actionBar = getSupportActionBar();
        //actionBar.hide();

        sun = (ImageView)findViewById(R.id.main_iv_sun_done);

        Intent i1 = getIntent();
        if (i1.hasExtra("itemName")) {
            // Forward intent if this is from the user clicking the notification
            // Then the intent will have the info to reset the alarm
            Intent i2 = new Intent(this, AlarmHelper.class);
            i2.putExtras(i1.getExtras());
            sendBroadcast(i2);

            // Send name to updateListView to highlight that item
            String notificationClickedName = i1.getStringExtra("itemName");
            updateListViewItems();
        } else{
            updateListViewItems();
        }

        sun.bringToFront(); // otherwise it's weirdly behind the other stuff in the layout
        sun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sun.getAnimation() == null) {
                    Animation spin = AnimationUtils.loadAnimation(sun.getContext(), R.anim.animation_spin);
                    sun.startAnimation(spin);
                }
            }
        });


        /*
        AlertDialog alertDialog = new AlertDialog.Builder(Main.this).create();
        alertDialog.setTitle("Information Leak!");
        alertDialog.setMessage("'WeatherPlus' is leaking your phone number: 555-3141");

        DialogInterface.OnClickListener dismisser = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", dismisser);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Stop It (Now)", dismisser);
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Stop It (Forever)", dismisser);

        alertDialog.show();
        */

    }

    @Override
    protected void onResume(){
        super.onResume();
        checkNotificationPermission();
        ctx = getApplicationContext();
        updateListViewItems();
    }


    public void newItem(View v){


        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle("Reminder Name?");

        // Set up the input box
        final EditText input = new EditText(this);
        // Specify the type of input expected;
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // Specify the max length so that the name of the reminder is not too long
        // For really long names the detail few overflows the layout and the details
        // cannot be seen.
        //int maxLength = 100;
        //input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                tmpName = input.getText().toString();
                if(checkItemExists(ctx, tmpName)){
                    Toast.makeText(ctx, "A reminder with that name already exists.", Toast.LENGTH_LONG).show();
                } else {
                    getTime();
                }

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

    private void getTime(){
        // Get Current Time

        final Calendar c = Calendar.getInstance();
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMinute = c.get(Calendar.MINUTE);

        // Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {

                        tmpHour = hour;
                        tmpMinute = minute;

                        getDay();
                    }
                }, mHour, mMinute, false);
        timePickerDialog.setTitle("When will it be too late?");
        timePickerDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        timePickerDialog.show();
    }

    private void getDay(){
        // Get Current Date
        final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);


        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        tmpYear = year;
                        tmpMonth = month+1;
                        tmpDay = day;

                        String str = tmpYear + "," + tmpMonth + "," + tmpDay + "," + (tmpHour) + "," + tmpMinute;
                        //Log.d(TAG, "date string: " + str);

                        long epoch = toEpoch(str);
                        tmpGoalEpoch = epoch;

                        //Log.d(TAG, "epoch: " + epoch + "   current system time: " + System.currentTimeMillis());

                        createEntry();

                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        datePickerDialog.show();
    }


    private long toEpoch(String dateStr){
        //dateStr="2016,11,28,17,05";

        //Log.d(TAG, "Date string: " + dateStr);
        SimpleDateFormat df = new SimpleDateFormat("yyyy,MM,dd,HH,mm");
        //SimpleDateFormat df = new SimpleDateFormat("yyyy");
        try{
            Date date = df.parse(dateStr);
            //Log.d(TAG, "Parsed Date: " + date.toString());
            long epoch = date.getTime();
            //Log.d(TAG, "epoch from parsed date: " + epoch + "  actual epoch (now): " + System.currentTimeMillis());
            return epoch;
        } catch (ParseException pe){
            pe.printStackTrace();
        }
        return -1;
    }



    private void createEntry(){
        // Private means only this app can access
        SharedPreferences sp = getSharedPreferences(PREFS_DEADLINES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();


        // Add name
        // Next notificaiton time will be added to that preferences DB in the AlarmHelper
        editor.putLong(tmpName, tmpGoalEpoch);
        editor.commit();


        // Set Alarm
        long epoch = AlarmHelper.getNextTime(tmpGoalEpoch);
        AlarmHelper.setAlarm(this, tmpName, epoch);

        long delay = epoch - System.currentTimeMillis();
        String msg = "You will be reminded about this in approximately " + AlarmHelper.msToMinutes(delay) + " minutes";
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();

        updateListViewItems();

    }




    // Create a message handling object as an anonymous class.
    private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            TextView tv = (TextView)v;
            String name = tv.getText().toString();
            //Log.d(TAG, tv.getText().toString());

            Intent i = new Intent(Main.this, Detail.class);
            i.putExtra("name", tv.getText().toString());
            startActivity(i);
        }
    };

    // Create a message handling object as an anonymous class.
    private AdapterView.OnItemLongClickListener mMessageLongClickedHandler = new AdapterView.OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView parent, View v, final int position, long id) {

            final String name = ((TextView)v).getText().toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
            builder.setTitle("Delete This Reminder?");

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteReminder(ctx, name);
                    updateListViewItems();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();

            return true;
        }
    };



    public static void deleteReminder(final Context ctx, final String name){
        //Log.d(TAG, "Deleting!!");

        // remove name
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_DEADLINES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(name);
        editor.commit();

        // remove next notification time
        sp = ctx.getSharedPreferences(PREFS_NEXT_NOTIFICATION_TIMES, MODE_PRIVATE);
        editor = sp.edit();
        editor.remove(name);
        editor.commit();

        // Delete any notification
        NotificationManager mNotificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(Main.simpleHash(name));
    }


    public static int simpleHash(String s){
        int hash = 7;
        for (int i = 0; i < s.length(); i++) {
            hash = hash*31 + s.charAt(i);
        }
        return hash;
    }

    private void updateListViewItems(){
        // Get keys / reminder items
        SharedPreferences sp = getSharedPreferences(PREFS_DEADLINES, MODE_PRIVATE);
        Map<String, ?> itemsMap= sp.getAll();
        Object[] items = itemsMap.keySet().toArray();


        if(items.length == 0){
            sun.setVisibility(View.VISIBLE);
        } else {
            sun.setVisibility(View.GONE); // Hide sun
        }

        // Show them in the interface
        ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(this,
                R.layout.simple_row, items);

        ListView lv = (ListView) findViewById(R.id.main_listview_items);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(mMessageClickedHandler);
        lv.setOnItemLongClickListener(mMessageLongClickedHandler);

    }

    public static boolean checkItemExists(Context context, String name){
        // Check if preference is still there (or if this event was deleted)
        SharedPreferences sp = (SharedPreferences)context.getSharedPreferences(PREFS_DEADLINES, context.MODE_PRIVATE);
        long endEpoch = sp.getLong(name, -1);
        return endEpoch > 0;
    }

    private void checkNotificationPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String[] perms = new String[]{Manifest.permission.POST_NOTIFICATIONS};
            if (ContextCompat.checkSelfPermission(this, perms[0]) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(perms, 0);
            }
        }
    }
}
