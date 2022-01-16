package com.group60.project;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class Master extends AppCompatActivity implements Serializable {

    private static final int REQUEST_ENABLE_BT = 100;
    BluetoothAdapter bluetoothAdapter;
    String Btname;
    ArrayAdapter<String> adapter;
    static ArrayAdapter<String> chosenadapter;
    BluetoothDevice bdDevice;
    ArrayList<BluetoothDevice> arrayListBluetoothDevices = null;
    ListView devicediscoverylist, chosendeviceslist;
    static TextView status;
    TextView masterbattery;
    TextView masterlattitude;
    TextView masterlongitude;
    static TextView logs;
    BluetoothDevice device;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
    static String phoneId;
    private LocationManager masterlocationManager;
    private LocationListener mastergpslistener;
    static String mastergpslattitude;
    static String mastergpslongitude;
    int batterylevel;
    static List<String> accepteddevices;
    static String filepath;
    static Context c1;
    static int counterVariable=0;
    static int totalPowerConsumtion=0;
    static long distributedtimeTaken=0;

    static HashMap<String, BluetoothSocket> map;


    private BroadcastReceiver masterBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batterylevel = level;
            masterbattery.setText(String.valueOf(level) + "%");
        }
    };

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
            {

                chosenadapter.add(device.getName()+"->Connected");
                phoneId=device.getName();
                chosenadapter.notifyDataSetChanged();
                JSONObject jsonObjectnew = new JSONObject();
                try {
                    jsonObjectnew.put("requesting_details_intial",1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String jsonString= jsonObjectnew.toString();
                Gateway gateway=new Gateway(map.get(device.getName()),handler);
                gateway.start();
                try {
                    Thread.sleep(3000);
                    gateway.write(jsonString.getBytes());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NullPointerException e1){
                    Toast.makeText(getApplicationContext(), "NOT CONNECTED", Toast.LENGTH_SHORT).show();
                }
            }else if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Discover Finished", Toast.LENGTH_SHORT).show();
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

                chosenadapter.remove(device.getName() + "->" + "ELIGIBLE");
                chosenadapter.remove(device.getName() + "->" + "NOT-ELIGIBLE");
                chosenadapter.remove(device.getName() +"->Connected");
                status.setText("Disconnected from "+ device.getName());
                chosenadapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.masterpage1);
        ToggleButton bluetoothswitch = findViewById(R.id.Bluetoothswitch);
        Button bluetoothdiscovery = findViewById(R.id.bluetoothdiscovery);
        Button matrixmultiplication = findViewById(R.id.matrixmultiplication);
        logs=findViewById(R.id.logs);
        arrayListBluetoothDevices = new ArrayList<BluetoothDevice>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1);
        devicediscoverylist = findViewById(R.id.devicediscoverylist);
        devicediscoverylist.setAdapter(adapter);
        chosendeviceslist = findViewById(R.id.chosendeviceslist);
        chosenadapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1);
        chosendeviceslist.setAdapter(chosenadapter);
        masterbattery = findViewById(R.id.masterbattery);
        masterlattitude = findViewById(R.id.masterlattitude);
        masterlongitude = findViewById(R.id.masterlongitude);
        status = findViewById(R.id.status);
        map=new HashMap<String, BluetoothSocket>();
        accepteddevices=new ArrayList<>();
        filepath=getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        c1=getApplicationContext();


        getApplicationContext().registerReceiver(this.masterBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        IntentFilter filter=new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        getApplicationContext().registerReceiver(this.bluetoothReceiver,filter);

        masterlocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mastergpslistener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mastergpslongitude = String.valueOf(location.getLongitude());
                mastergpslattitude = String.valueOf(location.getLatitude());
                masterlattitude.setText("Lattitude: " + mastergpslattitude);
                masterlongitude.setText("Longitude: " + mastergpslongitude);
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        masterlocationManager.requestLocationUpdates("gps", 5000, 0, mastergpslistener);


        bluetoothdiscovery.setEnabled(false);
        matrixmultiplication.setEnabled(false);

        // Bluetooth
        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter.isEnabled()){
            bluetoothswitch.setChecked(true);
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            Toast.makeText(getApplicationContext(),"Device discoverable for 5 minutes",Toast.LENGTH_SHORT).show();
            startActivity(discoverableIntent);
            bluetoothdiscovery.setEnabled(true);
        }

        bluetoothswitch.setOnClickListener(new View.OnClickListener() {
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
                     Btname=bluetoothAdapter.getName();
                     bluetoothAdapter.setName("MASTER");
                    bluetoothdiscovery.setEnabled(true);
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    Toast.makeText(getApplicationContext(),"Device discoverable for 5 minutes",Toast.LENGTH_SHORT).show();
                    startActivity(discoverableIntent);
                }else if(bluetoothAdapter.isEnabled()){
                    bluetoothAdapter.disable();
                    bluetoothdiscovery.setEnabled(false);
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                    chosenadapter.clear();
                    status.setText("STATUS");
                    logs.setText("LOGS");
                    chosenadapter.notifyDataSetChanged();
                }
            }

        });


        bluetoothdiscovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clear();
                arrayListBluetoothDevices.clear();
                getPairedDevices();
                IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                Master.this.registerReceiver(myReceiver, intentFilter);
                bluetoothAdapter.startDiscovery();
            }
        });

        devicediscoverylist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bdDevice = arrayListBluetoothDevices.get(position);
                String str=adapter.getItem(position);
                String[] values=str.split("->");
                if(values[2].equals("NotPaired")){
                    bluetoothAdapter.cancelDiscovery();
                    Boolean isBonded = false;
                    try {
                        isBonded = createBond(bdDevice);
                        if(isBonded){
                            adapter.remove(adapter.getItem(position));
                            adapter.insert(bdDevice.getName()+"->"+bdDevice.getAddress()+"->Paired",0);
                            adapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(),"Connected to "+ bdDevice+" status: "+isBonded,Toast.LENGTH_SHORT).show();
                }else if(values[2].equals("Paired")){
                    bluetoothAdapter.cancelDiscovery();
                    device=arrayListBluetoothDevices.get(position);

                    try {
                        map.put(device.getName(),device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("4d040779-adb7-434f-bd74-7d1885bb822d")));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                            try {
                                map.get(device.getName()).connect();
                                Message message = Message.obtain();
                                message.what=STATE_CONNECTED;
                                handler.sendMessage(message);

                            } catch (IOException e) {
                                e.printStackTrace();
                                Message message = Message.obtain();
                                message.what=STATE_CONNECTION_FAILED;
                                handler.sendMessage(message);
                            }


                }
            }
        });

        chosendeviceslist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String[] str=chosenadapter.getItem(position).split("->");
                Gateway gateway=new Gateway(map.get(str[0]),handler);
                gateway.start();
                if(str[1].compareTo("NOTELIGIBLE")==0 || str[1].compareTo("DECLINED")==0){
                    JSONObject jsonObjectnew = new JSONObject();
                    try {
                        jsonObjectnew.put("requesting_details_intial",-1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String jsonString= jsonObjectnew.toString();
                    try {
                        Thread.sleep(1000);
                        gateway.write(jsonString.getBytes());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e1){
                        Toast.makeText(getApplicationContext(), "NOT CONNECTED", Toast.LENGTH_SHORT).show();
                    }
                }
                else if(str[1].compareTo("ELIGIBLE")==0){
                    JSONObject jsonObjectnew = new JSONObject();
                    try {
                        jsonObjectnew.put("requesting_details_for_popup",-1);
                        jsonObjectnew.put("name",str[0]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String jsonString= jsonObjectnew.toString();
                    gateway.write(jsonString.getBytes());

                    displaychosendetails(str[0],gateway);
                    matrixmultiplication.setEnabled(true);
                }
            }
        });
        matrixmultiplication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for(int i=0;i<chosenadapter.getCount();i++){
                    String[] str=chosenadapter.getItem(i).split("->");
                    if(str[1].compareToIgnoreCase("ACCEPTED")==0){
                        accepteddevices.add(str[0]);
                    }
                }
                Intent intent;
                intent = new Intent(getApplicationContext(), Mastermatrix.class);
                startActivity(intent);
            }
        });
    }

    private void displaychosendetails(String s, Gateway gateway){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Device "+ s + " :\n");
        alertDialogBuilder.setPositiveButton("Monitor",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        JSONObject jsonObjectnew = new JSONObject();
                        try {
                            jsonObjectnew.put("monitoring_request",-1);
                            jsonObjectnew.put("name",s);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String jsonString= jsonObjectnew.toString();
                        gateway.write(jsonString.getBytes());
                    }
                });

        alertDialogBuilder.setNegativeButton("ASK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                JSONObject jsonObjectnew = new JSONObject();
                try {
                    jsonObjectnew.put("are_you_ready_for_computation",-1);
                    jsonObjectnew.put("name",s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String jsonString= jsonObjectnew.toString();
                gateway.write(jsonString.getBytes());
            }
        });
        alertDialogBuilder.create().show();
    }

    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
        arrayListBluetoothDevices.clear();
        if(pairedDevice.size()>0)
        {
            for(BluetoothDevice device : pairedDevice)
            {
                adapter.add(device.getName()+"->"+device.getAddress()+"->Paired");
                arrayListBluetoothDevices.add(device);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(arrayListBluetoothDevices.size()<1)
                {
                    adapter.add(device.getName()+"->"+device.getAddress()+"->NotPaired");
                    arrayListBluetoothDevices.add(device);
                    adapter.notifyDataSetChanged();
                }
                else
                {
                    boolean flag = true;    // flag to indicate that particular device is already in the arlist or not
                    for(int i = 0; i<arrayListBluetoothDevices.size();i++)
                    {
                        if(device.getAddress().equals(arrayListBluetoothDevices.get(i).getAddress()))
                        {
                            flag = false;
                        }
                    }
                    if(flag)
                    {
                        adapter.add(device.getName()+"->"+device.getAddress() +"->NotPaired");
                        arrayListBluetoothDevices.add(device);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    public boolean createBond(BluetoothDevice btDevice) throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth ENABLED", Toast.LENGTH_LONG).show();
            }else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Bluetooth NOT ENABLED", Toast.LENGTH_LONG).show();
                ToggleButton btn=findViewById(R.id.Bluetoothswitch);
                Button btn1=findViewById(R.id.bluetoothdiscovery);
                btn1.setEnabled(false);
                btn.setChecked(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.disable();
    }

    private static double distance(double masterlat, double masterlon, double slavelat, double slavelon) {
        double theta = masterlon - slavelon;
        double dist = Math.sin(deg2rad(masterlat))
                * Math.sin(deg2rad(slavelat))
                + Math.cos(deg2rad(masterlat))
                * Math.cos(deg2rad(slavelat))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }



    static Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            if(msg.what==STATE_LISTENING){
                status.setText("LISTENING");
            }else if( msg.what==STATE_CONNECTING){
                status.setText("CONNECTING");
            }else if(msg.what==STATE_CONNECTED){
                status.setText("CONNECTED TO "+phoneId);
            }else if(msg.what==STATE_CONNECTION_FAILED){
                status.setText("CONNECTION FAILED");
            }else if(msg.what==STATE_MESSAGE_RECEIVED){
                byte[] readBuff = (byte [])msg.obj;
                String tempMsg = new String(readBuff,0,msg.arg1);

                if(tempMsg.contains("matrix_calculation_reply")){

                        String[] tempMsg1=tempMsg.split("\\}");
                        for(int i=0;i<tempMsg1.length;i++){
                            tempMsg1[i]=tempMsg1[i]+"}";
                        }
                        for(int v=0;v<tempMsg1.length;v++){
                            JsonObject jsonObject = new JsonParser().parse(tempMsg1[v]).getAsJsonObject();
                            if(jsonObject.get("output_row").getAsInt()==0){
                                Mastermatrix.r1m5.setText(jsonObject.get("output_row_matrix").getAsString());
                            }else if(jsonObject.get("output_row").getAsInt()==1){
                                Mastermatrix.r2m5.setText(jsonObject.get("output_row_matrix").getAsString());
                            }else if(jsonObject.get("output_row").getAsInt()==2){
                                Mastermatrix.r3m5.setText(jsonObject.get("output_row_matrix").getAsString());
                            }else if(jsonObject.get("output_row").getAsInt()==3){
                                Mastermatrix.r4m5.setText(jsonObject.get("output_row_matrix").getAsString());
                            }
                            counterVariable++;
                            totalPowerConsumtion = totalPowerConsumtion + jsonObject.get("powerConsumption").getAsInt();
                            if(jsonObject.get("time_taken").getAsLong() > distributedtimeTaken){
                                distributedtimeTaken = jsonObject.get("time_taken").getAsLong();
                            }
                        }
                        if(counterVariable==4){
                            Mastermatrix.etd.setText(String.valueOf(distributedtimeTaken) + " nanoseconds");
                            Mastermatrix.tpcwd.setText((String.valueOf(totalPowerConsumtion) + " Joules"));
                            distributedtimeTaken = 0;
                            totalPowerConsumtion = 0;
                        }

                }else{

                    JsonObject jsonObject = new JsonParser().parse(tempMsg).getAsJsonObject();

                    if(jsonObject.has("monitoring_reply"))
                    {
                        int batt = jsonObject.get("batterylevel").getAsInt();
                        String pid=jsonObject.get("phoneid").getAsString();

                        logs.setText("\nReceived from :"+ pid+ "->Battery: "+jsonObject.get("batterylevel").getAsString()+"%");
                        String data="";
                        data=java.time.LocalDateTime.now()+"->Received from :"+ pid+ "->Battery: "+jsonObject.get("batterylevel").getAsString()+"%\n";

                        String filename = "batterymonitoringlog";
                        File file = new File(filepath, filename + ".txt");
                        FileOutputStream stream = null;
                        try {
                            stream = new FileOutputStream(file, true);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        try {
                            stream.write(data.getBytes());

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                stream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    if(jsonObject.has("requesting_details_intial_rejected")){
                        chosenadapter.remove(phoneId + "->" + "Connected");
                        chosenadapter.remove(phoneId + "->" + "DECLINED");
                        chosenadapter.remove(phoneId + "->" + "NOTELIGIBLE");
                        chosenadapter.add(phoneId + "->" + "DECLINED");
                        chosenadapter.notifyDataSetChanged();
                    }

                    if(jsonObject.has("batterylevel") && jsonObject.has("phoneid")&&jsonObject.has("lat") && jsonObject.has("lon") && jsonObject.has("requesting_details_intial_response")) {


                        logs.setText("Slave Logs: "+jsonObject.get("phoneid")+" -> "+jsonObject.get("batterylevel")+" -> "+ jsonObject.get("lat").getAsString()+" -> "+ jsonObject.get("lon").getAsString());
                        int batterylevel = jsonObject.get("batterylevel").getAsInt();
                        String pid = jsonObject.get("phoneid").getAsString();
                        double slaveLat = jsonObject.get("lat").getAsDouble();
                        double slaveLon = jsonObject.get("lon").getAsDouble();
                        double x1 = Double.parseDouble(mastergpslattitude);
                        double y1 = Double.parseDouble(mastergpslongitude);
                        double x2 = slaveLat;
                        double y2 = slaveLon;
                        double dis = 1000 * distance(x1, y1, x2, y2);

                        String data = "";


                        if (batterylevel > 25 && dis < 120) {
                            String phd = jsonObject.get("phoneid").getAsString();

                            chosenadapter.remove(phoneId + "->" + "Connected");
                            chosenadapter.remove(phoneId + "->" + "DECLINED");
                            chosenadapter.remove(phoneId + "->" + "NOTELIGIBLE");
                            chosenadapter.add(phoneId + "->" + "ELIGIBLE");
                            chosenadapter.notifyDataSetChanged();

                            data=java.time.LocalDateTime.now()+"->PhoneId: "+pid+"->Battery: "+batterylevel+"%->Latitude: "+slaveLat+"->Longitude: "+slaveLon+"\n";

                            String filename = "ConnectionInformation";
                            File file = new File(filepath, filename + ".txt");
                            FileOutputStream stream = null;
                            try {
                                stream = new FileOutputStream(file, true);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            try {
                                stream.write(data.getBytes());

                                Toast.makeText(c1, "Data written to file " + filename + ".txt", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    stream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        } else {
                            chosenadapter.remove(phoneId + "->" + "Connected");
                            chosenadapter.remove(phoneId + "->" + "DECLINED");
                            chosenadapter.remove(phoneId + "->" + "NOTELIGIBLE");
                            chosenadapter.add(phoneId + "->" + "NOTELIGIBLE");
                            chosenadapter.notifyDataSetChanged();
                        }
                    }

                    if(jsonObject.has("are_you_ready_for_computation_reply")){
                        if(jsonObject.get("reply").getAsString().compareTo("YES")==0){
                            chosenadapter.remove(jsonObject.get("name").getAsString()+"->ELIGIBLE");
                            chosenadapter.add(jsonObject.get("name").getAsString()+"->ACCEPTED");
                            chosenadapter.notifyDataSetChanged();
                        }else if(jsonObject.get("reply").getAsString().compareTo("NO")==0){
                            chosenadapter.remove(jsonObject.get("name").getAsString()+"->ELIGIBLE");
                            chosenadapter.add(jsonObject.get("name").getAsString()+"->DECLINED");
                            chosenadapter.notifyDataSetChanged();
                        }
                    }

                }

            }



            return true;
        }
    });

}
