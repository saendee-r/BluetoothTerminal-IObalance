package me.aflak.bluetoothterminal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;

import static java.util.Calendar.DATE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback, Urination.UninationCallback,DrinkingWater.DrinkingWaterCallback {
    public int hn = 1337;
    private String name;
    private Bluetooth b;
    private EditText message;
    private Button send;
    private Button btn;
    private TextView text;
    private ScrollView scrollView;
    private boolean registered = false;
    private static final String TAG = "Chat";
    private final int ALLDAY = 0;
    private final int MORNING = 1;
    private final int AFTERNOON = 2;
    private final int NIGHT = 3;
    private Urination urination = new Urination(hn);
    private DrinkingWater drinkingWater =new DrinkingWater(hn);
    private SimpleDateFormat dateFormat = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss");
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("myParseServ")
                //.clientKey("YOUR_CLIENT_KEY")
                .server("http://n66.info:1337/parse")
                .build()
        );


        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.text);
        message = (EditText) findViewById(R.id.message);
        send = (Button) findViewById(R.id.send);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        text.setMovementMethod(new ScrollingMovementMethod());
        send.setEnabled(false);

        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);
        urination.setUrineCallback( this );
        drinkingWater.setDrinkingWaterCallback( this );
        int pos = getIntent().getExtras().getInt("pos");
        name = b.getPairedDevices().get(pos).getName();

        Display("Connecting...");
        b.connectToDevice(b.getPairedDevices().get(pos));

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = message.getText().toString();
                message.setText("");

                if (command(msg) == 0) {

                    b.send(msg);
                }
                Display("You: " + msg);

//                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
//                msg = df.format(new Date());
//                b.send(msg);
//                Display("You: "+msg);
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered = true;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (registered) {
            unregisterReceiver(mReceiver);
            registered = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.close:
                b.removeCommunicationCallback();
                b.disconnect();
                Intent intent = new Intent(this, Select.class);
                startActivity(intent);
                finish();
                return true;

            case R.id.rate:
                Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void Display(final String s) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.append(s + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Display("Connected to " + device.getName() + " - " + device.getAddress());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                send.setEnabled(true);
            }
        });
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Display("Disconnected!");
        Display("Connecting again...");
        b.connectToDevice(device);
    }

    @Override
    public void onMessage(String message) {
        String weight;
        Log.i(name + ": " ,message);
        if (message.startsWith("urination:")) {
            weight = message.substring("urination:".length());
            urination.addUrination(weight);
            return;
        }
        if (message.startsWith("FCWater:")) {
            weight = message.substring("FCWater:".length());
            drinkingWater.firstConnectWater(weight);
            return;
        }
        if (message.startsWith("drink:")) {
            weight = message.substring("drink:".length());
            drinkingWater.addDrink(weight);
            return;
        }
        if (message.startsWith("beforeAdd:")) {
            weight = message.substring("beforeAdd:".length());
            drinkingWater.beforeAddWater(weight);
            return;
        }
        if (message.startsWith("bottleWt:")) {
            weight = message.substring("bottleWt:".length());
            drinkingWater.setBottleWt(weight);
            return;
        }
        Display(name + ": " + message);

    }

    @Override
    public void onError(String message) {
        Display("Error: " + message);
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
        Display("Error: " + message);
        Display("Trying again in 3 sec.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.connectToDevice(device);
                    }
                }, 2000);
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(Chat.this, Select.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if (registered) {
                            unregisterReceiver(mReceiver);
                            registered = false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if (registered) {
                            unregisterReceiver(mReceiver);
                            registered = false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }
    };

    private int command(String cmd) {

        if (cmd.startsWith("urine.today")) {
            urination.queryUrinationShift(ALLDAY, Calendar.getInstance().get(DATE), Calendar.getInstance().get(MONTH), Calendar.getInstance().get(YEAR));
            return 1;
        }
        if (cmd.startsWith("urine.morning")) {
            urination.queryUrinationShift(MORNING, Calendar.getInstance().get(DATE), Calendar.getInstance().get(MONTH), Calendar.getInstance().get(YEAR));
            return 1;
        }
        if (cmd.startsWith("urine.afternoon")) {
            urination.queryUrinationShift(AFTERNOON, Calendar.getInstance().get(DATE), Calendar.getInstance().get(MONTH), Calendar.getInstance().get(YEAR));
            return 1;
          }
        if(cmd.startsWith("urine.night")){
            urination.queryUrinationShift(NIGHT,Calendar.getInstance().get(DATE),Calendar.getInstance().get(MONTH),Calendar.getInstance().get(YEAR));
            return 1;
        }

        if(cmd.startsWith("drink.start")){
            autoCollect();
            return 2;
        }
        if(cmd.startsWith("drink.setbottle")){
            b.send("setBottle");
            return 2;
        }
        if (cmd.startsWith("drink.today")) {
            drinkingWater.queryWaterShift(ALLDAY, Calendar.getInstance().get(DATE), Calendar.getInstance().get(MONTH), Calendar.getInstance().get(YEAR));
            return 2;
        }
        if (cmd.startsWith("drink.morning")) {
            drinkingWater.queryWaterShift(MORNING, Calendar.getInstance().get(DATE), Calendar.getInstance().get(MONTH), Calendar.getInstance().get(YEAR));
            return 2;
        }
        if (cmd.startsWith("drink.afternoon")) {
            drinkingWater.queryWaterShift(AFTERNOON, Calendar.getInstance().get(DATE), Calendar.getInstance().get(MONTH), Calendar.getInstance().get(YEAR));
            return 2;
        }
        if(cmd.startsWith("drink.night")){
            drinkingWater.queryWaterShift(NIGHT,Calendar.getInstance().get(DATE),Calendar.getInstance().get(MONTH),Calendar.getInstance().get(YEAR));
            return 2;
        }
        if(cmd.startsWith("drink.beforeAdd")){
            b.send( "beforeAdd" );
            return 2;
        }
        Log.d( "cmd","no match command" );
        return 0;

    }
    public void autoCollect(){
        thread = new Thread( new Runnable() {
            private Handler mHandler = new Handler();

            @Override
            public void run() {
                b.send( "drink" );
                mHandler.postDelayed(this,1000);
            }
        } );
        thread.start();
    }
    public void printSumOfUrineList(List<ParseObject> list){
        float sumV=0;
        for(ParseObject a:list){
            sumV= sumV + a.getNumber("volume").floatValue();
        }
        Log.i("printSum","Sum: "+String.valueOf(sumV));
        Display("UrineOutput: "+String.valueOf(sumV));
    }
    public void printSumOfDrinkList(List<ParseObject> list){
        float sumV=0;
        for(ParseObject a:list){
            if(a.getString( "mode" ).equals( "drink" )){
                sumV = sumV + a.getNumber( "volume" ).floatValue();
            }
            else{
                Log.d("mode",a.getString("mode"));
            }
        }
        Display( "Total water input:" + String.valueOf( sumV ));
    }
    @Override
    public void printUrineListToLog(List<ParseObject> list){
        for(ParseObject a:list){
            Log.i("printParseList",dateFormat.format(a.getDate("timestamp"))+"   Volume "+a.get("volume").toString()+"ml");
            Display(dateFormat.format(a.getDate("timestamp"))+"   Volume "+a.get("volume").toString()+"ml");
        }
        printSumOfUrineList(list);
    }
    @Override
    public void printWaterListToLog(List<ParseObject> list) {
        for (ParseObject a : list) {
            Log.i( "printWaterList", dateFormat.format( a.getDate( "timestamp" ) ) + "   Volume " + a.get( "volume" ).toString() + "ml" + a.getString( "mode" ) );
            Display( dateFormat.format( a.getDate( "timestamp" ) ) + "   Volume " + a.get( "volume" ).toString() + "ml" + a.getString( "mode" ) );
        }
        printSumOfDrinkList( list );
    }
}
