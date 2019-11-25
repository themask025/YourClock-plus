package com.example.myalarmclock;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    TableLayout table;
    DatabaseHelper myDB;
    TextView noAlarmsMessage;

    AlarmManager alarm_manager;
    Context context;
    PendingIntent pending_intent = null;
    int choose_whale_sound;

    int alarmID;

    Calendar calendar;
    Intent my_intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        table = (TableLayout)this.findViewById(R.id.alarmsTableView);
        myDB = new DatabaseHelper(this);
        noAlarmsMessage = (TextView)this.findViewById(R.id.textViewNoAlarms);



        /*
        all the code from whaleclock responsible for alarms
         */

        this.context = this;
        // initialize our alarm manager
        alarm_manager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // create an instance of a calendar
        calendar = Calendar.getInstance();

        // create an intent to the Alarm Receiver class
        my_intent = new Intent(this.context, AlarmReceiver.class);

        /*
        ends here
         */

        populateAlarmsTable();
        setAlarms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateAlarmsTable();
        setAlarms();
    }

    //gets alarms from database in a tableView
    public void populateAlarmsTable() {

        //cleans the table for the new entries
        table.removeAllViews();

        Cursor cur = myDB.getData();

        //fills the table with entries
        try {
            //checks whether there are alarms in the database
            if (cur.getCount() != 0) {
                noAlarmsMessage.setVisibility(View.INVISIBLE);

                //loop for going through the database table
                if (cur.moveToFirst()) {
                    do {
                        //initializing variables
                        int rows = cur.getCount();
                        int cols = cur.getColumnCount();
                        final String rowIndex = cur.getString(0);
                        final String rowDate = cur.getString(1);
                        final String rowName = cur.getString(2);

                        //creating table row instance for the current record
                        TableRow row = new TableRow(this);
                        row.setLayoutParams(new TableRow.LayoutParams(
                                TableRow.LayoutParams.MATCH_PARENT,
                                TableRow.LayoutParams.WRAP_CONTENT));

                        //setting text view for alarm date and text fields of the current record
                        for (int j = 1; j < cols; j++) {

                            TextView tv = new TextView(this);
                            tv.setLayoutParams(new TableRow.LayoutParams(
                                    TableLayout.LayoutParams.WRAP_CONTENT,
                                    TableLayout.LayoutParams.WRAP_CONTENT));
                            tv.setGravity(Gravity.CENTER);
                            if(j==1)
                                tv.setTextSize(40);
                            else
                                tv.setTextSize(20);
                            tv.setPadding(15, 10, 0, 10);
                            tv.setText(cur.getString(j));
                            row.addView(tv);
                        }

                        //adding the table row to the table
                        //and setting OnClick listener for the context menu
                        table.addView(row);
                        row.setLongClickable(true);
                        row.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                Intent intent = new Intent();
                                intent.putExtra("rowName", rowName);
                                intent.putExtra("rowDate", rowDate);
                                intent.putExtra("rowID", rowIndex);
                                setIntent(intent);
                                registerForContextMenu(v);
                                openContextMenu(v);
                                unregisterForContextMenu(v);
                                return true;
                            }
                        });

                    } while (cur.moveToNext());
                }
            }
            else{
            noAlarmsMessage.setVisibility(View.VISIBLE);
            }
        } catch (SQLException mSQLException) {
            Toast.makeText(this, "Unlucky, we couldn't get your alarms.", Toast.LENGTH_LONG).show();
            throw mSQLException;
        }
    }

    //activates all the alarms from the tableView
    public void setAlarms() {

        Cursor cur = myDB.getData();
        if (cur.getCount() != 0) {
            if (cur.moveToLast()) {
                do {
                    //initializing variables
                    alarmID = Integer.parseInt(cur.getString(0));
                    Log.e("alarm_logs", String.valueOf(alarmID));

                    //setting the calendar
                    final String[] alarmDate = cur.getString(1).split(":");
                    final int hour = Integer.parseInt(alarmDate[0]);
                    final int minute = Integer.parseInt(alarmDate[1]);

                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);

                    //put in extra string into my_intent
                    //tells the clock that we've activated the alarm
                    my_intent.putExtra("extra", "alarm on");

                    //some extra extras
                    my_intent.putExtra("alarmDate", cur.getString(1));
                    my_intent.putExtra("alarmID", Integer.parseInt(cur.getString(0)));

                    Log.e("alarm_logs", "Read ID in main activity: " + alarmID);
                    Log.e("alarm_logs", "Read Date in main activity: " + alarmDate);


                    //cancelling previous pending intent
                    if (pending_intent != null){
                        alarm_manager.cancel(pending_intent);
                        Log.e("alarm_logs", "intent id " + alarmID + " cancelled.");
                    }


                    // create a pending intent that delays the intent
                    // until the specified calendar time
                    pending_intent = PendingIntent.getBroadcast(MainActivity.this, alarmID, my_intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // set the alarm manager
                    alarm_manager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(),
                            pending_intent), pending_intent);

                    Log.e("alarm_logs", "pending intent sent.");

                } while (cur.moveToNext());
            }
        }
    }

    //context menu for every entry of the alarms table list
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.context_menu_title);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_alarms_table, menu);
    }

    //context menu items
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Intent intent = getIntent();
        String id = intent.getStringExtra("rowID");
        String name = intent.getStringExtra("rowName");
        String date = intent.getStringExtra("rowDate");
        Toast.makeText(this, id, Toast.LENGTH_LONG).show();

        switch (item.getItemId()) {
            case R.id.edit:

                Intent callEditor = new Intent(this, AddAlarmActivity.class);
                startActivity(callEditor);
                return true;

            case R.id.delete:

                alarm_manager.cancel(pending_intent);

                // put extra string into my_intent
                // tells the clock that you pressed the "alarm off" button
                my_intent.putExtra("extra", "alarm off");
                // also put an extra int into the alarm off section
                // to prevent crashes in a Null Pointer Exception
                my_intent.putExtra("whale_choice", choose_whale_sound);

                // stop the ringtone
                sendBroadcast(my_intent);

                if(myDB.removeData(id))
                    Toast.makeText(this, "Alarm deleted successfully.", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(this, "Couldn't delete alarm.", Toast.LENGTH_LONG).show();
                populateAlarmsTable();

                if(table.getChildCount() == 0)
                    myDB.cleanDatabase();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }


    //starts AddAlarmActivity when the plus-button is pressed
    public void sendMessage(View view){
        Intent intent = new Intent(this, AddAlarmActivity.class);
        startActivity(intent);
    }


}
