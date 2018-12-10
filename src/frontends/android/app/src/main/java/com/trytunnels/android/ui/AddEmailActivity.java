package com.trytunnels.android.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.trytunnels.android.utils.ApiUtil;
import com.trytunnels.android.utils.PreferenceStoreUtil;

import org.json.JSONObject;
import org.strongswan.android.R;

import java.io.IOException;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AddEmailActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";

    private Button btnCloseAddEmail;
    private Button btnCreateSignIn;
    private EditText txtEmail, txtPassword; //, txtConfirmPassword;

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

        setContentView(R.layout.tt_add_email_page);

        btnCloseAddEmail = (Button) findViewById(R.id.btn_close_add_email);
        btnCreateSignIn = (Button) findViewById(R.id.btn_create_sign_in);
        txtEmail = (EditText) findViewById(R.id.add_email_email_txt);
        txtPassword = (EditText) findViewById(R.id.add_email_password_txt);
        //txtConfirmPassword = (EditText) findViewById(R.id.add_email_password_confirm_txt);

        // making notification bar transparent
        changeStatusBarColor();

        btnCloseAddEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAddEmail();
            }
        });

        btnCreateSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSignIn();
            }
        });
    }

    private void closeAddEmail() {
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

    /*private boolean isValidEmail(String email)
    {
        String expression = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}";

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.
    }*/

    private void createSignIn() {
        if (txtEmail.getText().length() != 0 && txtPassword.getText().length() != 0) {

                if(Patterns.EMAIL_ADDRESS.matcher(txtEmail.getText().toString()).matches() && txtPassword.getText().length() >= 8) {
                    final Handler mHandler = new Handler(Looper.getMainLooper());

                    btnCreateSignIn.setEnabled(false);

                    String receipt = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_paid_sub_info_key), "");

                    ApiUtil.getInstance().convertShadowUser(txtEmail.getText().toString(), txtPassword.getText().toString(), receipt, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "CreateShadowUser failure: " + e.getMessage());

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //enableVpnPower();
                                    btnCreateSignIn.setEnabled(true);
                                    Toast.makeText(AddEmailActivity.this, "Error connecting", Toast.LENGTH_SHORT).show();
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
                                JSONObject jsonObj;
                                jsonObj = new JSONObject(responseString);
                                Log.d(TAG, "JSON OBJ: " + jsonObj.toString());

                                if (jsonObj == null) {
                                    throw new Exception("Invalid json");
                                }

                                if (jsonObj.has("code"))
                                {
                                    if (jsonObj.getString("code").equals("3"))
                                    {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                //enableVpnPower();

                                                Toast.makeText(AddEmailActivity.this, "Password must contain a capital letter, contain a number, and contain a special character.", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                    else if(jsonObj.getString("code").equals("1"))
                                    {
                                        // valid login
                                        PreferenceStoreUtil.getInstance().putString(getString(R.string.tt_email_store_key), txtEmail.getText().toString());
                                        PreferenceStoreUtil.getInstance().putString(getString(R.string.tt_password_store_key), txtPassword.getText().toString());

                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                //enableVpnPower();

                                                Toast.makeText(AddEmailActivity.this, "Please confirm your e-mail and your sign-in will be enabled.", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                        setResult(1);
                                        finish();
                                    }
                                    else
                                    {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                //enableVpnPower();

                                                Toast.makeText(AddEmailActivity.this, "Error on user creation.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                }

                                //throw new Exception("Convert user problem");


                            } catch (Exception e) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //enableVpnPower();

                                        Toast.makeText(AddEmailActivity.this, "Error on user creation.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //enableVpnPower();

                                    btnCreateSignIn.setEnabled(true);
                                }
                            });


                        }
                    });
                }
                else
                {
                    Toast.makeText(this, "Please make sure to enter a valid e-mail and at least eight characters for your password.", Toast.LENGTH_LONG).show();
                    btnCreateSignIn.setEnabled(true);
                }
        }
        else
        {
            btnCreateSignIn.setEnabled(true);
        }
    }
}