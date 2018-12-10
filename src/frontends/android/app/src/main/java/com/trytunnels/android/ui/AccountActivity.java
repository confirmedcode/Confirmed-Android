package com.trytunnels.android.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trytunnels.android.utils.ApiUtil;
import com.trytunnels.android.utils.PreferenceStoreUtil;
import com.trytunnels.android.utils.SubscriptionUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.strongswan.android.R;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AccountActivity extends AppCompatActivity {

    private SubscriptionUtil mSubscriptionUtil;

    private static final String TAG = "AccountActivity";

    private Button btnCloseAccountPage;
    private Button btnAddEmail;
    private Button btnSignOut;
    private RelativeLayout layoutSignOut;
    private Button btnUpgradeAnnual;
    private Button btnUpgradeAllDevices;
    private TextView txtEmail, txtPlanTitle, txtPlanDesc, txtVersion;

    final Handler mHandler = new Handler(Looper.getMainLooper());

    private static final int EMAIL_RESULT = 7;
    private static final int UPGRADE_RESULT = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Making notification bar transparent
        /*if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }*/

        //getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().hide();
        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
        //getSupportActionBar().setTitle(Html.fromHtml("<font color=\"red\">" + "Account" + "</font>"));

        setTheme(R.style.ttAccountTheme);

        setContentView(R.layout.tt_account_page);

        //btnCloseAccountPage = (Button) findViewById(R.id.btn_close_account_page);
        btnAddEmail = (Button) findViewById(R.id.btn_account_add_email);
        btnSignOut = (Button) findViewById(R.id.btn_account_sign_out);
        layoutSignOut = (RelativeLayout) findViewById(R.id.layout_account_sign_out);
        btnUpgradeAnnual = (Button) findViewById(R.id.btn_upgrade_plan);
        btnUpgradeAllDevices = (Button) findViewById(R.id.btn_go_unlimited);
        txtEmail = (TextView) findViewById(R.id.txt_account_email);
        txtPlanTitle = (TextView) findViewById(R.id.txt_account_plan_title);
        txtPlanDesc = (TextView) findViewById(R.id.txt_account_plan_desc);
        txtVersion = (TextView) findViewById(R.id.txt_account_version);

        // making notification bar transparent
        changeStatusBarColor();

        String email = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_email_store_key), "");
        String receipt = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_paid_sub_info_key), "");
        String currentPlan = PreferenceStoreUtil.getInstance().getString(SubscriptionUtil.mCurrProductIdKey, "");

        if(email.equals(""))
        {
            btnAddEmail.setVisibility(View.VISIBLE);
            txtEmail.setVisibility(View.INVISIBLE);
            layoutSignOut.setVisibility(View.INVISIBLE);
        }
        else
        {
            btnAddEmail.setVisibility(View.INVISIBLE);
            txtEmail.setVisibility(View.VISIBLE);
            txtEmail.setText(email);
            if(receipt.equals("")) {
                layoutSignOut.setVisibility(View.VISIBLE);
            }
            else
            {
                layoutSignOut.setVisibility(View.INVISIBLE);
            }
        }

        if(!currentPlan.contains("annual") && !currentPlan.equals(""))
        {
            btnUpgradeAnnual.setVisibility(View.VISIBLE);
        }
        else
        {
            btnUpgradeAnnual.setVisibility(View.INVISIBLE);
        }

        if(currentPlan.contains("android"))
        {
            btnUpgradeAllDevices.setVisibility(View.VISIBLE);
        }
        else
        {
            btnUpgradeAllDevices.setVisibility(View.INVISIBLE);
        }

        getPlanData();

        /*btnCloseAccountPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAccountPage();
            }
        });*/

        btnAddEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEmail();
            }
        });

        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        btnUpgradeAnnual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent upgradeIntent = new Intent(v.getContext(), UpgradeAnnualActivity.class);
                //welcomeIntent.putExtra("HAS_SUBSCRIPTION", true);
                /*signUpIntent.putExtra("UNLIMITED_PRICE", unlimitedPrice);
                signUpIntent.putExtra("UNLIMITED_CC", unlimitedCurrencyCode);
                signUpIntent.putExtra("UNLIMITED_PRICE_BD", unlimitedPriceBD);
                signUpIntent.putExtra("ANDROID_PRICE", androidPrice);
                signUpIntent.putExtra("ANDROID_CC", androidCurrencyCode);
                signUpIntent.putExtra("ANDROID_PRICE_BD", androidPriceBD);*/
                startActivityForResult(upgradeIntent, UPGRADE_RESULT);
            }
        });

        btnUpgradeAllDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upgradeToAllDevices();
            }
        });

        mSubscriptionUtil = new SubscriptionUtil(this, new SubscriptionUtil.SubscriptionSetupListener() {
            @Override
            public void onSuccess() {

            }
        });

        try
        {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            txtVersion.setText(pInfo.versionName);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            txtVersion.setText("");
        }
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
        if (!mSubscriptionUtil.getIabHelper().handleActivityResult(requestCode, resultCode, data)) {
            if(requestCode == EMAIL_RESULT)
            {
                String email = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_email_store_key), "");
                if (!email.equals("")) {
                    btnAddEmail.setVisibility(View.INVISIBLE);
                    txtEmail.setVisibility(View.VISIBLE);
                    txtEmail.setText(email);
                }
            }
            else if(requestCode == UPGRADE_RESULT)
            {
                if(resultCode == 1)
                {
                    btnUpgradeAnnual.setVisibility(View.INVISIBLE);
                    handleNewPlan();
                }
            }
        }
    }

    private void getPlanData()
    {
        txtPlanDesc.setVisibility(View.INVISIBLE);
        txtPlanTitle.setText("Loading...");

        String receipt = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_paid_sub_info_key), "");

        if(receipt.equals(""))
        {
            String email = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_email_store_key),"");
            String password = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_password_store_key), "");

            ApiUtil.getInstance().initiateClient(getApplicationContext(), email, password, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Sigin failure: " + e.getMessage());

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            txtPlanTitle.setText(R.string.tt_couldnt_get_plan);
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() == null) {
                        onFailure(call, new IOException("Empty response body"));
                        return;
                    }

                    ApiUtil.getInstance().getSubscription(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "GetSub failure: " + e.getMessage());

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    txtPlanTitle.setText(R.string.tt_couldnt_get_plan);
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.body() == null) {
                                onFailure(call, new IOException("Empty response body"));
                                return;
                            }

                            String responseString = response.body().string();
                            response.body().close();
                            Log.d(TAG, "Response String: " + responseString);


                            try {
                                checkPlanType(responseString);
                            } catch (Exception e) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //enableVpnPower();

                                        txtPlanTitle.setText(R.string.tt_couldnt_get_plan);
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }
        else
        {
            ApiUtil.getInstance().initiateClient(getApplicationContext(), receipt, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "SignIn failure: " + e.getMessage());

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            txtPlanTitle.setText(R.string.tt_couldnt_get_plan);
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() == null) {
                        onFailure(call, new IOException("Empty response body"));
                        return;
                    }

                    ApiUtil.getInstance().getSubscription(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "getSub failure: " + e.getMessage());

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    txtPlanTitle.setText(R.string.tt_couldnt_get_plan);
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.body() == null) {
                                onFailure(call, new IOException("Empty response body"));
                                return;
                            }

                            String responseString = response.body().string();
                            response.body().close();
                            Log.d(TAG, "Response String: " + responseString);

                            try {
                                checkPlanType(responseString);
                            } catch (Exception e) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //enableVpnPower();

                                        txtPlanTitle.setText(R.string.tt_couldnt_get_plan);
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }
    }

    private void checkPlanType(String responseString) throws Exception
    {
        JSONArray jsonArray;
        jsonArray = new JSONArray(responseString);
        Log.d(TAG, "JSON OBJ: " + jsonArray.toString());

        if (jsonArray == null) {
            throw new Exception("Invalid json");
        }

        String planType = "";

        for(int i=0;i<jsonArray.length();i++) {
            planType = jsonArray.getJSONObject(i).getString("planType");

            if(!planType.equals(getString(R.string.tt_android_only_plan)) && !planType.equals(getString(R.string.tt_android_only_plan_annual)))
            {
                break;
            }
        }

        if (planType.equals("")) {
            throw new Exception("Invalid json");
        }

        if(planType.equals(getString(R.string.tt_android_only_plan)) || planType.equals(getString(R.string.tt_android_only_plan_annual)))
        {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    txtPlanTitle.setText(R.string.tt_android_only_title);
                    txtPlanDesc.setVisibility(View.VISIBLE);
                    txtPlanDesc.setText(R.string.tt_android_only_desc);
                }
            });
        }
        else
        {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    txtPlanTitle.setText(R.string.tt_all_device_plan_title);
                    txtPlanDesc.setVisibility(View.VISIBLE);
                    txtPlanDesc.setText(R.string.tt_all_device_plan_desc);
                }
            });
        }
    }

    private void closeAccountPage() {
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

    private void addEmail() {
        Intent addEmailIntent = new Intent(this, AddEmailActivity.class);
        startActivityForResult(addEmailIntent, EMAIL_RESULT);
    }

    private void signOut()
    {
        PreferenceStoreUtil.getInstance().putString(getString(R.string.tt_email_store_key), "");
        setResult(1);
        finish();
    }

    private void upgradeToAllDevices()
    {
        String currentPlan = PreferenceStoreUtil.getInstance().getString(SubscriptionUtil.mCurrProductIdKey, "");

        if(currentPlan.contains("android")) {
            String newPlan = currentPlan.replace("android", "unlimited");
            mSubscriptionUtil.initSubscriptionUpgradeWithExtras(newPlan,
                    currentPlan, new SubscriptionUtil.SubscriptionFinishedListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AccountActivity.this, "success", Toast.LENGTH_SHORT).show();

                            Log.d(TAG, "Did an upgrade to unlimited!");
                            btnUpgradeAllDevices.setVisibility(View.INVISIBLE);
                            handleNewPlan();
                            getPlanData();
                        }
                    });
        }
    }

    // if switch plans, move login if it exists
    private void handleNewPlan()
    {
        String email = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_email_store_key),"");
        String password = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_password_store_key), "");
        final String receipt = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_paid_sub_info_key), "");

        // has a log in
        if(!email.equals("") && !password.equals("") && !receipt.equals(""))
        {
            //ApiUtil apiUtil = new ApiUtil(email, password);
            ApiUtil.getInstance().subscriptionEvent(receipt, new Callback() {
            @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "subscriptionEvent failure: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() == null) {
                        onFailure(call, new IOException("Empty response body"));
                        return;
                    }

                    String responseString = response.body().string();
                    response.body().close();
                    Log.d(TAG, "Response String: " + responseString);

                    // new receipt login
                    ApiUtil.getInstance().initiateClient(getApplicationContext(), receipt);
                }
            });
        }
    }
}