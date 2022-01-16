package com.group60.project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final Object ALL_PERMISSION_CODE = 100;

    @Override
    protected void onStart() {
        super.onStart();

        createPermissions();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button master= findViewById(R.id.master);
        Button slave= findViewById(R.id.slave);


        final Intent[] intent = new Intent[2];

        master.setOnClickListener(v -> {
            intent[0] = new Intent(getApplicationContext(), Master.class);
            Toast.makeText(getApplicationContext(), "Entering MASTER Mode.", Toast.LENGTH_SHORT).show();
            startActivity(intent[0]);
        });

        slave.setOnClickListener(v -> {
            intent[1] = new Intent(getApplicationContext(), Slave.class);
            Toast.makeText(getApplicationContext(), "Entering SLAVE Mode.", Toast.LENGTH_SHORT).show();
            startActivity(intent[1]);
        });

    }


    public void createPermissions(){
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        String permission3 = Manifest.permission.INTERNET;
        String permission1=Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String permission2=Manifest.permission.ACCESS_COARSE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, permission1) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, permission2) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, permission3) != PackageManager.PERMISSION_GRANTED){
            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, permission) || !ActivityCompat.shouldShowRequestPermissionRationale(this, permission1) || !ActivityCompat.shouldShowRequestPermissionRationale(this, permission2) || !ActivityCompat.shouldShowRequestPermissionRationale(this, permission3)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.INTERNET}, (Integer) ALL_PERMISSION_CODE);
            }
        }
    }
}