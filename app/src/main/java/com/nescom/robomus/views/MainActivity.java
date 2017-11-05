package com.nescom.robomus.views;

import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.nescom.robomus_bongo.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        Button bSend = (Button) findViewById(R.id.button);
        final CheckBox checkArduino = (CheckBox) findViewById(R.id.checkBoxArduino);


        bSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, LogActivity.class);
                EditText etPort = (EditText) findViewById(R.id.editTextPort);
                EditText etInstrument = (EditText) findViewById(R.id.editTextInstrument);
                it.putExtra("port", etPort.getText().toString());
                it.putExtra("instrument", etInstrument.getText().toString());
                if(checkArduino.isChecked()){
                    it.putExtra("arduino", "1");
                }else{
                    it.putExtra("arduino", "0");
                }

                startActivity(it);
            }
        });


        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                .permitDiskWrites()
                .build());
        StrictMode.setThreadPolicy(old);

    }
}
