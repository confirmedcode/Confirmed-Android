package com.trytunnels.android.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trytunnels.android.utils.ApiUtil;
import com.trytunnels.android.utils.ConfirmedConstants;
import com.trytunnels.android.utils.PreferenceStoreUtil;
import com.trytunnels.android.utils.SubscriptionUtil;

import org.strongswan.android.R;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SignUpActivity extends AppCompatActivity {

    private SubscriptionUtil mSubscriptionUtil;

    private static final String TAG = "SignUpActivity";

    private Button btnCloseSignUp;
    private Button btnMakePurchase;
    private Button btnMonthlyChoice;
    private Button btnAnnualChoice;
    private RadioButton rBtnAllDevices;
    private RadioButton rBtnAndroidOnly;
    //private TextView txtHeader;
    private View viewUnderline;

    private String subStype = "unlimitedtunnels";

    private String androidPrice = "$4.99";
    private String unlimitedPrice = "$9.99";
    private String androidAnnualPrice = "$49.99";
    private String unlimitedAnnualPrice = "$99.99";
    private Double androidPriceBD = 4.99;
    private Double unlimitedPriceBD = 9.99;
    private Double androidAnnualPriceBD = 49.99;
    private Double unlimitedAnnualPriceBD = 99.99;
    private String androidCurrencyCode = "USD";
    private String unlimitedCurrencyCode = "USD";
    private String introPrice = "$0.99";

    private boolean annualSelected = false;

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

        setContentView(R.layout.tt_sign_up_page);

        btnCloseSignUp = (Button) findViewById(R.id.btn_close_sign_up);
        btnMakePurchase = (Button) findViewById(R.id.btn_make_purchase);
        rBtnAllDevices = (RadioButton) findViewById(R.id.radio_all_devices);
        rBtnAndroidOnly = (RadioButton) findViewById(R.id.radio_android_only);
        //txtHeader = (TextView) findViewById(R.id.txt_sign_up_header);
        btnMonthlyChoice = (Button) findViewById(R.id.btn_monthly_choice);
        btnAnnualChoice = (Button) findViewById(R.id.btn_annual_choice);
        viewUnderline = (View) findViewById(R.id.view_sign_up_selection_underline);

        rBtnAllDevices.setChecked(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // get data and change labels for localized prices
            unlimitedPrice = extras.getString("UNLIMITED_PRICE");
            unlimitedCurrencyCode = extras.getString("UNLIMITED_CC");
            unlimitedPriceBD = extras.getDouble("UNLIMITED_PRICE_BD");
            androidPrice = extras.getString("ANDROID_PRICE");
            androidCurrencyCode = extras.getString("ANDROID_CC");
            androidPriceBD = extras.getDouble("ANDROID_PRICE_BD");
            introPrice = extras.getString("INTRO_PRICE");
            unlimitedAnnualPrice = extras.getString("UNLIMITED_ANNUAL_PRICE");
            unlimitedAnnualPriceBD = extras.getDouble("UNLIMITED_ANNUAL_PRICE_BD");
            androidAnnualPrice = extras.getString("ANDROID_ANNUAL_PRICE");
            androidAnnualPriceBD = extras.getDouble("ANDROID_ANNUAL_PRICE_BD");

            rBtnAllDevices.setText(String.format(getString(R.string.tt_sign_up_all_devices_desc_var), unlimitedPrice));
            rBtnAndroidOnly.setText(String.format(getString(R.string.tt_sign_up_android_only_desc_var), androidPrice));
            //btnMakePurchase.setText(String.format(getString(R.string.tt_sign_up_make_purchase_button_var), introPrice));
            //txtHeader.setText(String.format(getString(R.string.tt_sign_up_header_var), introPrice));
        }

        // making notification bar transparent
        changeStatusBarColor();

        mSubscriptionUtil = new SubscriptionUtil(this);

        btnCloseSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSignUp();
            }
        });

        btnMakePurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPurchaseWorkflow();
            }
        });

        btnMonthlyChoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthlyTapped();
            }
        });

        btnAnnualChoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                annualTapped();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSubscriptionUtil != null) {
            mSubscriptionUtil.dispose();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!mSubscriptionUtil.getIabHelper().handleActivityResult(requestCode, resultCode, data)) {

        }
    }

    private void closeSignUp() {
        finish();
    }

    private void monthlyTapped()
    {
        if(annualSelected) {
            annualSelected = false;

            btnMonthlyChoice.setTextColor(getResources().getColor(R.color.confirmed_blue));
            btnAnnualChoice.setTextColor(getResources().getColor(R.color.dark_gray));

            subStype = subStype.replaceFirst("annual", "");

            ObjectAnimator animation = ObjectAnimator.ofFloat(viewUnderline, "translationX", 0f);
            animation.setDuration(300);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.start();

            rBtnAllDevices.setText(String.format(getString(R.string.tt_sign_up_all_devices_desc_var), unlimitedPrice));
            rBtnAndroidOnly.setText(String.format(getString(R.string.tt_sign_up_android_only_desc_var), androidPrice));
        }
    }

    private void annualTapped()
    {
        if(!annualSelected) {
            annualSelected = true;

            btnMonthlyChoice.setTextColor(getResources().getColor(R.color.dark_gray));
            btnAnnualChoice.setTextColor(getResources().getColor(R.color.confirmed_blue));

            if(!subStype.contains("annual"))
            {
                subStype += "annual";
            }

            ObjectAnimator animation = ObjectAnimator.ofFloat(viewUnderline, "translationX", getResources().getDimensionPixelOffset(R.dimen.tt_monthly_annual_width));
            animation.setDuration(300);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.start();

            rBtnAllDevices.setText(String.format(getString(R.string.tt_sign_up_all_devices_annual_desc_var), unlimitedAnnualPrice));
            rBtnAndroidOnly.setText(String.format(getString(R.string.tt_sign_up_android_only_annual_desc_var), androidAnnualPrice));
        }
    }

    private void launchPurchaseWorkflow() {
        mSubscriptionUtil.initSubscription(subStype, new SubscriptionUtil.SubscriptionFinishedListener() {
            @Override
            public void onSuccess() {
                //Toast.makeText(SignUpActivity.this, "success", Toast.LENGTH_SHORT).show();

                PreferenceStoreUtil.getInstance().putLong(getString(R.string.tt_receipt_start_date), System.currentTimeMillis());

                String receipt = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_paid_sub_info_key), "");

                // initialize api with new receipt
                ConfirmedConstants.forceLatestVersion();
                ApiUtil.getInstance().initiateClient(getApplicationContext(), receipt);

                setResult(1);
                finish();
            }
        });
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

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_all_devices:
                if (checked)
                    subStype = getString(R.string.tt_paid_unlimited_sub_key);
                    break;
            case R.id.radio_android_only:
                if (checked)
                    subStype = getString(R.string.tt_paid_sub_key);
                    break;
        }

        if(annualSelected)
        {
            subStype += "annual";
        }
    }
}