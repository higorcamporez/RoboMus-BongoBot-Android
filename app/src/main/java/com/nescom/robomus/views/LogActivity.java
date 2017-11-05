package com.nescom.robomus.views;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nescom.robomus.communication.arduino.UsbService;
import com.nescom.robomus.instrument.percussion.bongo.Bongo;
import com.nescom.robomus_bongo.R;

import java.lang.ref.WeakReference;
import java.util.Set;

public class LogActivity extends AppCompatActivity {

    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    public Bongo bongo = null;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler = new MyHandler(this);
        //search the ip adress
        //WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        final String ipAddress = Formatter.formatIpAddress(ip);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);




        final Button startButton = (Button) findViewById(R.id.buttonStart);
        final Activity thisActivity = this;
        final TextView textLog = (TextView) thisActivity.findViewById(R.id.textViewLog);
        textLog.setText("clicou");
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(startButton.getText().equals("Stop Robot")){
                    Log.i("stop","instrument disconnected");
                    if(bongo != null){
                        bongo.disconnect();
                        bongo = null;
                        startButton.setText("Start Robot");
                    }
                }else {
                    Intent it = getIntent();

                    int port = Integer.parseInt(it.getStringExtra("port"));

                    String oscInstrumentAdress = it.getStringExtra("instrument");
                    String name = oscInstrumentAdress.substring(1);
                    int check = Integer.parseInt(it.getStringExtra("arduino"));



                    if (check == 0) {

                        bongo = new Bongo(  name, oscInstrumentAdress, port,
                                            null, ipAddress, thisActivity, textLog);
                        bongo.listenThread();
                        bongo.handshake();
                        startButton.setText("Stop Robot");
                    } else {

                        if (usbService != null) {


                            bongo = new Bongo(  name, oscInstrumentAdress, port,
                                                usbService, ipAddress, thisActivity, textLog);
                            bongo.listenThread();
                            bongo.handshake();
                            startButton.setText("Stop Robot");

                        } else {

                            Toast.makeText(getApplicationContext(), "Connect the arduino first", Toast.LENGTH_SHORT).show();
                        }
                    }

                }

            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private class MyHandler extends Handler {
        private final WeakReference<LogActivity> mActivity;

        public MyHandler(LogActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:

                    // msg.getData();
                    if(msg == null) {
                        Log.i("teste velocidade", "null-patrao");
                    }else{
                        String data = (String) msg.obj;

                        if(!data.isEmpty()) {
                            //receive a message from arduino
                            Log.i("Arduino-receiver", "string=" + (data));
                            byte dataBytes[] = data.getBytes();
                            for(byte b: dataBytes){
                                //b & 0xFF convert to unsigned byte
                                this.mActivity.get().bongo.sendConfirmActionMessage(b&0xFF);
                                Log.i("Arduino-receiver", "Tempo b=" + (b&0xFF));
                            }

                        }
                    }

                    break;
            }
        }
    }

}
