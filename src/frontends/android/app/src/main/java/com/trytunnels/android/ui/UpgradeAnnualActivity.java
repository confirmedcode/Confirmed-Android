package com.trytunnels.android.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;

import com.trytunnels.android.utils.PreferenceStoreUtil;
import com.trytunnels.android.utils.SubscriptionUtil;

import org.strongswan.android.R;

import java.math.BigDecimal;
import java.util.Currency;

public class UpgradeAnnualActivity extends AppCompatActivity {

    private SubscriptionUtil mSubscriptionUtil;

    private static final String TAG = "UpgradeAnnualActivity";

    private Button btnCloseUpgradeAnnual;
    private Button btnMakeUpgrade;

    private String currSubType;

    private String androidPrice = "$49.99";
    private String unlimitedPrice = "$99.99";
    private Double androidPriceBD = 49.99;
    private Double unlimitedPriceBD = 99.99;
    private String androidCurrencyCode = "USD";
    private String unlimitedCurrencyCode = "USD";

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

        setContentView(R.layout.tt_upgrade_to_annual_page);

        btnCloseUpgradeAnnual = (Button) findViewById(R.id.btn_close_upgrade_annual);
        btnMakeUpgrade = (Button) findViewById(R.id.btn_make_upgrade_annual);

        currSubType = PreferenceStoreUtil.getInstance().getString(SubscriptionUtil.mCurrProductIdKey, "");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // get data and change labels for localized prices
            unlimitedPrice = extras.getString("UNLIMITED_PRICE");
            unlimitedCurrencyCode = extras.getString("UNLIMITED_CC");
            unlimitedPriceBD = extras.getDouble("UNLIMITED_PRICE_BD");
            androidPrice = extras.getString("ANDROID_PRICE");
            androidCurrencyCode = extras.getString("ANDROID_CC");
            androidPriceBD = extras.getDouble("ANDROID_PRICE_BD");

            //rBtnAllDevices.setText(String.format(getString(R.string.tt_sign_up_all_devices_desc_var), unlimitedPrice));
            //rBtnAndroidOnly.setText(String.format(getString(R.string.tt_sign_up_android_only_desc_var), androidPrice));
        }

        // making notification bar transparent
        changeStatusBarColor();

        mSubscriptionUtil = new SubscriptionUtil(this);

        btnCloseUpgradeAnnual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeUpgradeAnnual();
            }
        });

        btnMakeUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPurchaseWorkflow();
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

    private void closeUpgradeAnnual() {
        finish();
    }

    private void launchPurchaseWorkflow() {
        mSubscriptionUtil.initSubscriptionUpgradeWithExtras(currSubType + "annual", currSubType, new SubscriptionUtil.SubscriptionFinishedListener() {
            @Override
            public void onSuccess() {
                //Toast.makeText(SignUpActivity.this, "success", Toast.LENGTH_SHORT).show();

                PreferenceStoreUtil.getInstance().putString(SubscriptionUtil.mCurrProductIdKey, currSubType + "annual");

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
}