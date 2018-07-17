package me.aflak.bluetoothterminal;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by SanSan on 16-Jul-18.
 */

public class Urination {
    private final int ALLDAY = 0;
    private final int MORNING = 1;
    private final int AFTERNOON = 2;
    private final int NIGHT = 3;
    private int hn;
    private UninationCallback urinationCallback;
    private SimpleDateFormat dateFormat = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss");

    public Urination(int hn){
        this.hn = hn;
        }
    public void addUrination(String strV){
        float volume = Float.valueOf(strV);
        ParseObject urination = new ParseObject("Urination");
        urination.put("HN", hn);
        urination.put("volume", volume);
        urination.put("timestamp", new Date());
        urination.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.i(TAG,"add Success");
                } else {
                    Log.i(TAG,"add to Parse failed"+e.toString());
                }
            }
        });

    }
    private void queryUrination(Calendar startTime, Calendar endTime){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Urination");
        query.whereEqualTo("HN", hn);
        query.whereGreaterThan("timestamp",startTime.getTime());
        query.whereLessThan("timestamp",endTime.getTime());

        query.findInBackground( new FindCallback<ParseObject>() {
            public void done(List<ParseObject> urinationList, ParseException e) {
                if (e == null) {
                    Log.d("queryUrination", "Retrieved " + urinationList.size() + " urinations");
                    urinationCallback.printUrineListToLog( urinationList );

                } else {
                    Log.e("queryUrination", "Error: " + e.getMessage());
                }
            }
        });

    }
    public void queryUrinationShift(int mode,int date, int month, int year){
        Calendar startTime=Calendar.getInstance();
        Calendar endTime=Calendar.getInstance();
        switch (mode){
            case ALLDAY:
                startTime.set(year,month,date,0,0,0);
                endTime.set(year,month,date,23,59,59);
                break;
            case MORNING:
                startTime.set(year,month,date,0,0,0);
                endTime.set(year,month,date,7,59,59);
                break;
            case AFTERNOON:
                startTime.set(year,month,date,8,0,0);
                endTime.set(year,month,date,15,59,59);
                break;
            case NIGHT:
                startTime.set(year,month,date,16,0,0);
                endTime.set(year,month,date,23,59,59);
                break;
            default:
                Log.e("queryUrineShift","mode error");
        }
        queryUrination(startTime,endTime);
    }

    public void setUrineCallback(UninationCallback urinationCallback) {
        this.urinationCallback = urinationCallback;
    }

    public interface UninationCallback {
        void printUrineListToLog(List<ParseObject> list);
    }

}
