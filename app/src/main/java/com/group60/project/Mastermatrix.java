package com.group60.project;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Mastermatrix extends AppCompatActivity {

    EditText r1m1,r2m1,r3m1,r4m1,r1m2,r2m2,r3m2,r4m2;
    static TextView r1m5,r2m5,r3m5,r4m5,etma,etd,tpcwod,tpcwd;
    Button calculate;
    Gateway gateway;
    HashMap<String, BluetoothSocket> map;
    String[][] matrix1,matrix2;
    List<String> accepteddevices;
    static LocalTime startTime;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.masterpage2);
        r1m1=findViewById(R.id.r1m1);
        r2m1=findViewById(R.id.r2m1);
        r3m1=findViewById(R.id.r3m1);
        r4m1=findViewById(R.id.r4m1);
        r1m2=findViewById(R.id.r1m2);
        r2m2=findViewById(R.id.r2m2);
        r3m2=findViewById(R.id.r3m2);
        r4m2=findViewById(R.id.r4m2);
        r1m5=findViewById(R.id.r1m5);
        r2m5=findViewById(R.id.r2m5);
        r3m5=findViewById(R.id.r3m5);
        r4m5=findViewById(R.id.r4m5);
        etma=findViewById(R.id.etma);
        etd=findViewById(R.id.etd);
        tpcwod=findViewById(R.id.tpcwod);
        tpcwd=findViewById(R.id.tpcwd);
        calculate=findViewById(R.id.calculate);
        matrix1=new String[4][4];
        matrix2=new String[4][4];

        map=Master.map;
        accepteddevices=Master.accepteddevices;
        BatteryManager mBatteryManager =
                (BatteryManager)getApplicationContext().getSystemService(Context.BATTERY_SERVICE);


        calculate.setOnClickListener(v -> {

            r1m5.setText("");
            r2m5.setText("");
            r3m5.setText("");
            r4m5.setText("");
            etd.setText("");
            etma.setText("");
            tpcwod.setText("");
            tpcwd.setText("");
            Long energystart =
                    mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);



            long masterstart = System.nanoTime();


            int flag=0;
            if(r1m1.getText().toString().compareTo("")==0 || r2m1.getText().toString().compareTo("")==0 || r3m1.getText().toString().compareTo("")==0 || r4m1.getText().toString().compareTo("")==0 ||r1m2.getText().toString().compareTo("")==0 || r2m2.getText().toString().compareTo("")==0 || r3m2.getText().toString().compareTo("")==0 || r4m2.getText().toString().compareTo("")==0 ){
                Toast.makeText(getApplicationContext(), "Fields Empty", Toast.LENGTH_SHORT).show();
            }else{
                int[][] matrixint1=new int[4][4];
                int[][] matrixint2=new int[4][4];
                matrix1[0]=r1m1.getText().toString().split(" ");
                matrixint1[0]=Arrays.stream(matrix1[0]).mapToInt(Integer::parseInt).toArray();
                matrix1[1]=r2m1.getText().toString().split(" ");
                matrixint1[1]=Arrays.stream(matrix1[1]).mapToInt(Integer::parseInt).toArray();
                matrix1[2]=r3m1.getText().toString().split(" ");
                matrixint1[2]=Arrays.stream(matrix1[2]).mapToInt(Integer::parseInt).toArray();
                matrix1[3]=r4m1.getText().toString().split(" ");
                matrixint1[3]=Arrays.stream(matrix1[3]).mapToInt(Integer::parseInt).toArray();

                matrix2[0]=r1m2.getText().toString().split(" ");
                matrixint2[0]=Arrays.stream(matrix2[0]).mapToInt(Integer::parseInt).toArray();
                matrix2[1]=r2m2.getText().toString().split(" ");
                matrixint2[1]=Arrays.stream(matrix2[1]).mapToInt(Integer::parseInt).toArray();
                matrix2[2]=r3m2.getText().toString().split(" ");
                matrixint2[2]=Arrays.stream(matrix2[2]).mapToInt(Integer::parseInt).toArray();
                matrix2[3]=r4m2.getText().toString().split(" ");
                matrixint2[3]=Arrays.stream(matrix2[3]).mapToInt(Integer::parseInt).toArray();


                for(int i=0;i<4;i++){
                    if(matrixint1[i].length!=4 ||matrixint2[i].length!=4){
                        Toast.makeText(getApplicationContext(),"Please check input and try again.",Toast.LENGTH_SHORT).show();
                        flag=1;
                        break;
                    }
                }

                if(flag!=1) {
                    int j=0;
                    startTime= LocalTime.now();

                    for (int i = 0; i < 4; i++,j++) {

                        if(j>=accepteddevices.size()) j=0;
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("matrix_calculation",1);
                            jsonObject.put("output_row",i);
                            jsonObject.put("row", new JSONArray(matrixint1[i]));
                            jsonObject.put("matrix2", new JSONArray(matrixint2));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String jsonString = jsonObject.toString();
                        System.out.println(i+"->"+jsonString);

                        gateway = new Gateway(map.get(accepteddevices.get(j)), Master.handler);
                        gateway.write(jsonString.getBytes());

                    }

                    int[][] output = new int[4][4];
                    for(int i=0;i<4;i++){
                        int[] row = matrixint1[i];
                        int[] productrow = new int[4];
                        for(int l=0; l<4;l++){
                            int sum=0;
                            for(int k=0;k<4;k++){
                                sum = sum + row[k] * matrixint2[k][l];
                            }
                            productrow[l] = sum;
                        }
                        output[i] = productrow;
                    }


                    long diff = System.nanoTime() - masterstart ;
                    etma.setText(String.valueOf(diff) + " nanoseconds");

                    int master_battery_percentage = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    double powerConsumption = master_battery_percentage * 4300 * 0.1 * 4.19 *  3.6 ;
                    TextView tpcwod1=findViewById(R.id.tpcwod);
                    tpcwod1.setText(String.valueOf(powerConsumption));
                }

            }
        });

    }



}