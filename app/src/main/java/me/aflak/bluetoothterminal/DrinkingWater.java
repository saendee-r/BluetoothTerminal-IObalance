package me.aflak.bluetoothterminal;

import android.os.Handler;
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

import java.util.logging.LogRecord;

/**
 * Created by SanSan on 16-Jul-18.
 */

public class DrinkingWater{
    private int hn;
    private final int ALLDAY = 0;
    private final int MORNING = 1;
    private final int AFTERNOON = 2;
    private final int NIGHT = 3;
    private float maxWater = (float) 1000.0;
    private float lastV = maxWater;
    private float bottleWt = (float) 105.9;
    private float leftV;
    private boolean addingProcess = false;
    public DrinkingWaterCallback drinkingWaterCallback;
    public DrinkingWater(int hn){
        this.hn = hn;
    }

    public void setBottleWt(String strV){
        float newW = Float.valueOf( strV );
        bottleWt = newW;
    }
    public void firstConnectWater(String strV){
        float weight = Float.valueOf(strV);
        lastV = weight-bottleWt;
    }
    public void addDrink(String strV) {
        float weight = Float.valueOf(strV);
        if(weight < bottleWt){
            Log.e( "addDrink" ,"No bottle weight");
            return;
        }
        float currentV = weight - bottleWt;
        if(addingProcess){
            if(currentV-leftV > (float) 10.0){
                addingProcess = false;
            }
            else{
                Log.e( "addDrink" ,"addingProcess");
                return;
            }
        }
        float decreasedV = lastV - currentV;
        if( decreasedV < (float) 1.0){
            Log.e("addDrink", "no volume added");
            return;
        }
        lastV = currentV;
        ParseObject drinkWater = new ParseObject("DrinkWater");
        drinkWater.put("HN", hn);
        drinkWater.put("volume", decreasedV);
        drinkWater.put("timestamp", new Date());
        drinkWater.put("mode", "drink");
        drinkWater.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.i("addDrink", "add Success");
                } else {
                    Log.i("addDrink", "add to Parse failed" + e.toString());
                }
            }
        });
    }
    public void beforeAddWater(String strV){
        addingProcess = true;
        addDrink(strV);
        float weight = Float.valueOf(strV);
        if(weight < bottleWt){
            Log.e( "beforeAddWater" ,"No bottle weight");
            return;
        }
        leftV = weight-bottleWt;
        lastV = maxWater;
        ParseObject drinkWater = new ParseObject("DrinkWater");
        drinkWater.put("HN", hn);
        drinkWater.put("volume",maxWater-leftV);
        drinkWater.put("timestamp", new Date());
        drinkWater.put("mode","add");
        drinkWater.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.i("addWater","add Success");
                } else {
                    Log.i("addWater","add to Parse failed"+e.toString());
                }
            }
        });
    }
    private void queryWater(Calendar startTime, Calendar endTime){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("DrinkWater");
        query.whereEqualTo("HN", hn);
        query.whereGreaterThan("timestamp",startTime.getTime());
        query.whereLessThan("timestamp",endTime.getTime());
        query.findInBackground( new FindCallback<ParseObject>() {
            public void done(List<ParseObject> waterList, ParseException e) {
                if (e == null) {
                    Log.d("query", "Retrieved " + waterList.size() + " drinkLists");
                    drinkingWaterCallback.printWaterListToLog( waterList );
                } else {
                    Log.d("query", "Error: " + e.getMessage());
                }
            }
        });
    }
    public void queryWaterShift(int mode,int date, int month, int year){
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
        queryWater(startTime,endTime);
    }

//    public void printParseListToLog(List<ParseObject> list){
//        for(ParseObject a:list){
//            Log.i("printParseList",dateFormat.format(a.getDate("timestamp"))+"   Volume "+a.get("volume").toString()+"ml");
//            drinkingWaterCallback.drinkDisplay(dateFormat.format(a.getDate("timestamp"))+"   Volume "+a.get("volume").toString()+"ml");
//        }
//    }
//    public void printSumOfDrinkList(List<ParseObject> list){
//        float sumV=0;
//        for(ParseObject a:list){
//            if(a.getString( "type" ) == "drink"){
//                sumV = a.getNumber( "volume" ).floatValue();
//            }
//        }
//        drinkingWaterCallback.drinkDisplay( "WaterInput:" + );
//    }
    public void setDrinkingWaterCallback(DrinkingWaterCallback drinkingWaterCallback){
        this.drinkingWaterCallback = drinkingWaterCallback;
    }
    public interface DrinkingWaterCallback{
        void printWaterListToLog(List<ParseObject> list);
    }
}
