package com.trytunnels.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trytunnels.android.utils.ApiUtil;

import org.json.JSONArray;
import org.strongswan.android.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ExplanationActivity extends AppCompatActivity {

    private static final String TAG = "AccountActivity";

    private Button btnCloseExplanationPage;
    private Button btnSetup;
    private TextView txtIpAddressTitle, txtEncryptionTitle;
    private ImageView imgIpAddressIcon, imgEncryptionIcon;
    private RelativeLayout setupButtonLayout;

    final Handler mHandler = new Handler(Looper.getMainLooper());

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

        setContentView(R.layout.tt_explanation_page);

        btnCloseExplanationPage = (Button) findViewById(R.id.btn_close_explanation);
        btnSetup = (Button) findViewById(R.id.btn_explanation_setup);

        imgIpAddressIcon = (ImageView) findViewById(R.id.img_explanation_ip_address);
        imgEncryptionIcon = (ImageView) findViewById(R.id.img_explanation_encryption);

        txtIpAddressTitle = (TextView) findViewById(R.id.txt_explanation_ip_address_title);
        txtEncryptionTitle = (TextView) findViewById(R.id.txt_explanation_encryption_title);

        setupButtonLayout = (RelativeLayout) findViewById(R.id.view_explanation_setup_button);

        // making notification bar transparent
        changeStatusBarColor();

        btnCloseExplanationPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeExplanationPage();
            }
        });

        btnSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(1);
                finish();
            }
        });

        Bundle extras = getIntent().getExtras();
        boolean vpnIsOn = false;
        if (extras != null) {
            vpnIsOn = extras.getBoolean("IS_VPN_ON");
        }


        Drawable mIcon = null;

        if(vpnIsOn) {
            mIcon = ContextCompat.getDrawable(this, R.drawable.checkmark);
            mIcon.setColorFilter(ContextCompat.getColor(this, R.color.checkmark_green), PorterDuff.Mode.SRC_IN);

            txtIpAddressTitle.setText(R.string.tt_explanation_ip_address_title_good);
            txtEncryptionTitle.setText(R.string.tt_explanation_encryption_title_good);

            setupButtonLayout.setVisibility(View.INVISIBLE);
        }
        else
        {
            mIcon = ContextCompat.getDrawable(this, R.drawable.negative_dash);
            mIcon.setColorFilter(ContextCompat.getColor(this, R.color.block_red), PorterDuff.Mode.SRC_IN);

            txtIpAddressTitle.setText(R.string.tt_explanation_ip_address_title_bad);
            txtEncryptionTitle.setText(R.string.tt_explanation_encryption_title_bad);

            setupButtonLayout.setVisibility(View.VISIBLE);
        }
        imgIpAddressIcon.setImageDrawable(mIcon);
        imgEncryptionIcon.setImageDrawable(mIcon);
    }

    private void closeExplanationPage() {
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