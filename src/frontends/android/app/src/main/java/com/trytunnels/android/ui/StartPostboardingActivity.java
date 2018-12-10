package com.trytunnels.android.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.trytunnels.android.utils.ApiUtil;

import org.strongswan.android.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

//import com.heapanalytics.android.Heap;

public class StartPostboardingActivity extends AppCompatActivity {

    private static final String TAG = "StartPostboardingActivity";

    private Button btnSecure;
    private TextView txtDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        //getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().hide();

        setContentView(R.layout.tt_postboard_start);

        btnSecure = (Button) findViewById(R.id.btn_postboard_secure);
        txtDescription = (TextView) findViewById(R.id.txt_start_postboarding_details);

        // making notification bar transparent
        changeStatusBarColor();

        boolean vpnIsOn = false;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            vpnIsOn = extras.getBoolean("IS_VPN_ON");
        }

        if(vpnIsOn)
        {
            txtDescription.setText(R.string.tt_postboard_secured);
            btnSecure.setText(R.string.tt_postboard_secured_button);

            btnSecure.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nextScreen();
                }
            });
        }
        else {
            btnSecure.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activateVPN();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void activateVPN()
    {
        setResult(1);
        finish();
    }

    private void nextScreen()
    {
        setResult(2);
        finish();
    }

    /**
     * Making notification bar transparent
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}