package com.group60.project;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Gateway extends Thread{

    private  final BluetoothSocket bluetoothSocket;
    private  final InputStream inputStream;
    private  final OutputStream outputStream;
    android.os.Handler handler;


    public static final int STATE_MESSAGE_RECEIVED = 5;

    public  Gateway(BluetoothSocket socket, android.os.Handler handler1)
    {
        bluetoothSocket =socket;
        handler=handler1;
        InputStream tempIn = null;
        OutputStream tempOut =null;
        try {
            tempIn=bluetoothSocket.getInputStream();
            tempOut=bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        inputStream=tempIn;
        outputStream=tempOut;

    }

    public void run()
    {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true){
            try {
                bytes=  inputStream. read(buffer);
                handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }


    }

    public void write(byte[] bytes)
    {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }



}
