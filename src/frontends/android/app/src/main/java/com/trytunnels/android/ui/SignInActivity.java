package com.trytunnels.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

//import com.heapanalytics.android.Heap;
import com.trytunnels.android.utils.ApiUtil;
import com.trytunnels.android.utils.ConfirmedConstants;
import com.trytunnels.android.utils.PreferenceStoreUtil;

import org.json.JSONObject;
import org.strongswan.android.R;
import org.strongswan.android.logic.StrongSwanApplication;

import java.io.IOException;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";

    private Button btnCloseSignIn;
    private Button btnDoSignIn;
    private Button btnForgotPass;
    private Button btnHelp;
    private EditText txtEmail, txtPassword;

    int startingVersion = 0;

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

        setContentView(R.layout.tt_sign_in_page);

        btnCloseSignIn = (Button) findViewById(R.id.btn_close_sign_in);
        btnDoSignIn = (Button) findViewById(R.id.btn_sign_in_with_cred);
        btnForgotPass = (Button) findViewById(R.id.btn_sign_in_forgot_pass);
        btnHelp = (Button) findViewById(R.id.btn_sign_in_help);
        txtEmail = (EditText) findViewById(R.id.sign_in_email_txt);
        txtPassword = (EditText) findViewById(R.id.sign_in_password_txt);

        boolean hasSubscription = false;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            hasSubscription = extras.getBoolean("HAS_SUBSCRIPTION");
        }

        // making notification bar transparent
        changeStatusBarColor();

        btnCloseSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSignIn();
            }
        });

        btnDoSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLogIn();
            }
        });

        btnForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browswerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://confirmedvpn.com/forgot-password"));
                startActivity(browswerIntent);
            }
        });

        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.sendLogEmail(SignInActivity.this);
            }
        });
    }

    private void closeSignIn() {
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

    private void checkLogIn()
    {
        ApiUtil.getInstance().initiateClient(getApplicationContext());

        final Handler mHandler = new Handler(Looper.getMainLooper());

        btnDoSignIn.setEnabled(false);

        ApiUtil.getInstance().clearCookieJar();

        startingVersion = ConfirmedConstants.getVersionNumber();

        ApiUtil.getInstance().signIn(txtEmail.getText().toString(), txtPassword.getText().toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Log in check failure: " + e.getMessage());

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //enableVpnPower();
                        btnDoSignIn.setEnabled(true);
                        Toast.makeText(SignInActivity.this, "Error connecting", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    onFailure(call, new IOException("Empty response body"));
                    return;
                }

                boolean signedIn = false;

                if(ApiUtil.getInstance().checkSignedIn())
                {
                    signedIn = true;
                }
                // check other two version (switch to do/while loop if ever increases)
//                else
//                {
//                    ConfirmedConstants.incrementVersion();
//
//                    ApiUtil.getInstance().signInSync(txtEmail.getText().toString(), txtPassword.getText().toString());
//
//                    if(ApiUtil.getInstance().checkSignedIn())
//                    {
//                        signedIn = true;
//                    }
//                    else
//                    {
//                        ConfirmedConstants.incrementVersion();
//
//                        ApiUtil.getInstance().signInSync(txtEmail.getText().toString(), txtPassword.getText().toString());
//
//                        if(ApiUtil.getInstance().checkSignedIn())
//                        {
//                            signedIn = true;
//                        }
//                    }
//                }

                if(signedIn)
                {
                    // valid login
                    ApiUtil.getInstance().getKey(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "Get key check failure: " + e.getMessage());

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //enableVpnPower();
                                    btnDoSignIn.setEnabled(true);
                                    Toast.makeText(SignInActivity.this, "Error connecting", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseString = response.body().string();
                            response.body().close();
                            Log.d(TAG, "Response String: " + responseString);



                            try {
                                JSONObject jsonObj;
                                jsonObj = new JSONObject(responseString);
                                Log.d(TAG, "JSON OBJ: " + jsonObj.toString());

                                if (jsonObj == null) {
                                    throw new Exception("Invalid json");
                                }

                                String b64 = jsonObj.getString("b64");

                                if (b64.equals("")) {
                                    throw new Exception("Invalid json");
                                }

                                PreferenceStoreUtil.getInstance().putString(getString(R.string.tt_email_store_key), txtEmail.getText().toString());
                                PreferenceStoreUtil.getInstance().putString(getString(R.string.tt_password_store_key), txtPassword.getText().toString());

                                setResult(1);
                                finish();
                            }
                            catch(Exception e)
                            {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //enableVpnPower();

                                        Toast.makeText(SignInActivity.this, "Invalid login.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                        }
                    });
                }
                else
                {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //enableVpnPower();

                            Toast.makeText(SignInActivity.this, "Invalid login.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //enableVpnPower();

                        btnDoSignIn.setEnabled(true);
                    }
                });


            }
        });
    }
}