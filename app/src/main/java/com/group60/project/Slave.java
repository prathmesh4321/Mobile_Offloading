package com.group60.project;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;


@SuppressWarnings("ALL")
public class Slave extends AppCompatActivity{
    ToggleButton bluetoothswitch;
    private LocationManager slavelocationManager;
    private LocationListener slavegpslistener;
    String slavegpslattitude;
    String slavegpslongitude;
    Button sendbatteryinfo;
    ImageButton listeningbutton;
    BluetoothAdapter bluetoothAdapter;
    BluetoothServerSocket serverSocket;
    TextView slavestatus,slavelattitude,slavelongitude,slavebattery,slavelog;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
    Gateway gateway,gatewaybattery,gatewaystatus,gatewaycalculate;
    String bluetooth_name;
    String response="0";
    int batterylevel;
    AlertDialog alertDialog;
    int monitorflag=0;

    private BroadcastReceiver slaveBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batterylevel = level;
            slavebattery.setText(String.valueOf(level) + "%");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.disable();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slavepage);
        bluetoothswitch=findViewById(R.id.bluetoothswitch);
        sendbatteryinfo=findViewById(R.id.sendbatteryinfo);
        listeningbutton=findViewById(R.id.listeningbutton);
        slavestatus=findViewById(R.id.statusslave);
        slavebattery=findViewById(R.id.slavebattery);
        slavelattitude=findViewById(R.id.slaveLattitude);
        slavelongitude=findViewById(R.id.slavelongitude);
        slavelog=findViewById(R.id.slavelog);

        sendbatteryinfo.setEnabled(false);
        listeningbutton.setEnabled(false);


        getApplicationContext().registerReceiver(this.slaveBatteryInfoReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter.isEnabled()){
            bluetoothswitch.setChecked(true);
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            Toast.makeText(getApplicationContext(),"Device discoverable for 5 minutes",Toast.LENGTH_SHORT).show();
            startActivity(discoverableIntent);
            listeningbutton.setEnabled(true);
        }

        bluetoothswitch.setOnClickListener(new View.OnClickListener() {
            private static final int REQUEST_ENABLE_BT = 100;

            @Override
            public void onClick(View v) {
                if (bluetoothAdapter == null) {
                    Toast.makeText(getApplicationContext(),"The device doesn't support bluetooth",Toast.LENGTH_SHORT).show();
                    bluetoothswitch.setEnabled(false);
                    Intent intent =new Intent();
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    Toast.makeText(getApplicationContext(), "Going Back", Toast.LENGTH_SHORT).show();
                    startActivity(intent);
                } else if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    bluetoothAdapter.setName("SLAVE");
                    listeningbutton.setEnabled(true);
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    Toast.makeText(getApplicationContext(),"Device discoverable for 5 minutes",Toast.LENGTH_SHORT).show();
                    startActivity(discoverableIntent);
                }else if(bluetoothAdapter.isEnabled()){
                    bluetoothAdapter.disable();
                    slavestatus.setText("STATUS");
                    listeningbutton.setEnabled(false);
                }
            }

        });

        listeningbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("MYAPP", UUID.fromString("4d040779-adb7-434f-bd74-7d1885bb822d"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread t=new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothSocket socket=null;
                        while (socket==null)
                        {

                            try {

                                Message message=Message.obtain();
                                message.what=STATE_CONNECTING;
                                handler.sendMessage(message);
                                socket=serverSocket.accept();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Message message=Message.obtain();
                                message.what=STATE_CONNECTION_FAILED;
                                handler.sendMessage(message);


                            }

                            if(socket!=null)
                            {
                                Message message=Message.obtain();
                                message.what=STATE_CONNECTED;
                                handler.sendMessage(message);


                                ///FOR MESSAGE
                                gateway=new Gateway(socket,handler);
                                gateway.start();


                                ////FOR BATTERY INFO
                                gatewaybattery= new Gateway(socket,handler);
                                gatewaybattery.start();
                                //for caluclate
                                gatewaycalculate= new Gateway(socket,handler);
                                gatewaycalculate.start();

                                gatewaystatus=new Gateway(socket,handler);
                                gatewaystatus.start();

                                break;

                            }

                        }

                    }
                });
                t.start();

            }
        });

        slavelocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        slavegpslistener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                slavegpslongitude=String.valueOf(location.getLongitude());
                slavegpslattitude=String.valueOf(location.getLatitude());
                slavelattitude.setText("Lattitude: " +slavegpslattitude);
                slavelongitude.setText("Longitude: "+slavegpslongitude);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };
        slavelocationManager.requestLocationUpdates("gps", 5000, 0, slavegpslistener);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Do you wish to proceed?for the computaion");
        alertDialogBuilder.setPositiveButton("yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Toast.makeText(getApplicationContext(), "Start Battery Monitoring Power Tutor Application", Toast.LENGTH_LONG).show();
                        response="1";
                    }
                });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                response="0";
                finish();
            }
        });
        alertDialog = alertDialogBuilder.create();

        sendbatteryinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth_name = Settings.Secure.getString(getContentResolver(), "bluetooth_name");

                JSONObject jsonObjectnew = new JSONObject();
                try {
                    jsonObjectnew.put("phoneid",bluetooth_name);
                    jsonObjectnew.put("batterylevel",String.valueOf(batterylevel));
                    jsonObjectnew.put("lat",String.valueOf(slavegpslattitude));
                    jsonObjectnew.put("lon",String.valueOf(slavegpslongitude));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String jsonString= jsonObjectnew.toString();

                gatewaybattery.write(jsonString.getBytes());

            }
        });


    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){

                case STATE_LISTENING :
                    slavestatus.setText("LISTENING");
                    break;
                case  STATE_CONNECTING:
                    slavestatus.setText("CONNECTING");
                    break;
                case STATE_CONNECTED:
                    slavestatus.setText("CONNECTED");
                    sendbatteryinfo.setEnabled(true);
                    break;
                case STATE_CONNECTION_FAILED:
                    slavestatus.setText("CONN FAILED");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte [])msg.obj;

                    String tempMsg = new String(readBuff,0,msg.arg1);
                    if(tempMsg.contains("matrix_calculation")){
                        String[] tempMsg1=tempMsg.split("\\}");
                        for(int i=0;i<tempMsg1.length;i++){
                            tempMsg1[i]=tempMsg1[i]+"}";
                        }
                        for(int v=0;v<tempMsg1.length;v++){

                            JsonObject jsonObject = new JsonParser().parse(tempMsg1[v]).getAsJsonObject();
                            bluetooth_name = Settings.Secure.getString(getContentResolver(), "bluetooth_name");
                            if(jsonObject.has("matrix_calculation")){

                                slavelog.setText("\nReceived Row Number "+jsonObject.get("output_row")+" (matrix 1) from master  for computation");
                                JsonArray row= jsonObject.getAsJsonArray("row");
                                JsonArray matrix= jsonObject.getAsJsonArray("matrix2");
                                JsonArray output=new JsonArray();
                                BatteryManager mBatteryManager =
                                        (BatteryManager)getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
                                int master_battery_percentage = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                                double powerConsumption = master_battery_percentage * 3700 * 1 * 4.19 *  3.6 ;
                                long start = System.nanoTime();
                                for(int i=0;i<4;i++){
                                    int sum=0;
                                    for(int j=0;j<4;j++){
                                        JsonArray temp = (JsonArray) matrix.get(j);
                                        sum = sum + Integer.parseInt(String.valueOf(row.get(j))) * Integer.parseInt(String.valueOf(temp.get(i)));
                                    }
                                    output.add(sum);
                                }
                                System.out.println("Output -> "+output);
                                long diff = System.nanoTime() - start;
                                System.out.println(">>>>>>>>>>>>>>>>>>>>>>" + diff);
                                JSONObject jsonObjectnew = new JSONObject();
                                try {
                                    jsonObjectnew.put("matrix_calculation_reply","1");
                                    jsonObjectnew.put("output_row",jsonObject.get("output_row"));
                                    jsonObjectnew.put("output_row_matrix",output);
                                    jsonObjectnew.put("time_taken",diff);
                                    jsonObjectnew.put("powerConsumption", powerConsumption);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String jsonString= jsonObjectnew.toString();
                                gatewaybattery.write(jsonString.getBytes());
                            }
                        }
                    }else{

                        JsonObject jsonObject = new JsonParser().parse(tempMsg).getAsJsonObject();
                        bluetooth_name = Settings.Secure.getString(getContentResolver(), "bluetooth_name");

                        if(jsonObject.has("monitoring_request"))
                        {
                            if(monitorflag==0){
                                bluetooth_name = Settings.Secure.getString(getContentResolver(), "bluetooth_name");
                                Toast.makeText(getApplicationContext(), "Master Started Monitoring", Toast.LENGTH_SHORT).show();
                                monitorflag=1;
                                AsyncTask.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        while(true) {
                                            try {
                                                Thread.sleep(10000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            JSONObject jsonObjectnew = new JSONObject();
                                            try {
                                                jsonObjectnew.put("monitoring_reply", "1");
                                                jsonObjectnew.put("phoneid", bluetooth_name);
                                                jsonObjectnew.put("batterylevel", String.valueOf(batterylevel));
                                                jsonObjectnew.put("lat", String.valueOf(slavegpslattitude));
                                                jsonObjectnew.put("lon", String.valueOf(slavegpslongitude));

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }

                                            String jsonString = jsonObjectnew.toString();
                                            gatewaybattery.write(jsonString.getBytes());
                                        }
                                    }
                                });
                            }

                        }

                        if(jsonObject.has("requesting_details_intial")){

                            JSONObject jsonObjectnew = new JSONObject();
                            final AlertDialog.Builder alertDialogBuilder1 = new AlertDialog.Builder(Slave.this);
                            alertDialogBuilder1.setMessage("Master is requesting your battery, location details! Do you give permission?");
                            alertDialogBuilder1.setPositiveButton("yes",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            try {
                                                jsonObjectnew.put("requesting_details_intial_response","1");
                                                jsonObjectnew.put("phoneid",bluetooth_name);
                                                jsonObjectnew.put("batterylevel",String.valueOf(batterylevel));
                                                jsonObjectnew.put("lat",String.valueOf(slavegpslattitude));
                                                jsonObjectnew.put("lon",String.valueOf(slavegpslongitude));

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            String jsonString= jsonObjectnew.toString();
                                            gatewaybattery.write(jsonString.getBytes());
                                        }
                                    });

                            alertDialogBuilder1.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        jsonObjectnew.put("requesting_details_intial_rejected",0);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String jsonString= jsonObjectnew.toString();
                                    gatewaybattery.write(jsonString.getBytes());
                                }
                            });
                            alertDialogBuilder1.create().show();
                        }

                        if(jsonObject.has("are_you_ready_for_computation")){
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Slave.this);
                            alertDialogBuilder.setMessage("Are You Ready For The Computation?");
                            alertDialogBuilder.setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            JSONObject jsonObjectnew = new JSONObject();
                                            try {
                                                jsonObjectnew.put("are_you_ready_for_computation_reply","1");
                                                jsonObjectnew.put("reply","YES");
                                                jsonObjectnew.put("name",Settings.Secure.getString(getContentResolver(), "bluetooth_name"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            String jsonString= jsonObjectnew.toString();
                                            gatewaybattery.write(jsonString.getBytes());
                                        }
                                    });

                            alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    JSONObject jsonObjectnew = new JSONObject();
                                    try {
                                        jsonObjectnew.put("are_you_ready_for_computation_reply","1");
                                        jsonObjectnew.put("reply","NO");
                                        jsonObjectnew.put("name",Settings.Secure.getString(getContentResolver(), "bluetooth_name"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String jsonString= jsonObjectnew.toString();
                                    gatewaybattery.write(jsonString.getBytes());
                                }
                            });
                            alertDialogBuilder.create().show();
                        }

                    }
                    break;
            }
            return true;
        }
    });


}

