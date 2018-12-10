/*
 * Copyright (C) 2012-2017 Tobias Brunner
 * Copyright (C) 2012 Giuliano Grassi
 * Copyright (C) 2012 Ralf Sager
 * HSR Hochschule fuer Technik Rapperswil
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.  See <http://www.fsf.org/copyleft/gpl.txt>.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 */

package com.trytunnels.android.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatSpinner;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.trytunnels.android.data.CountrySelectionModel;
import com.trytunnels.android.ui.adapter.CountrySelectionAdapter;
import com.trytunnels.android.utils.ApiUtil;
import com.trytunnels.android.utils.ConfirmedConstants;
import com.trytunnels.android.utils.PreferenceStoreUtil;
import com.trytunnels.android.utils.SpeedTestService;
import com.trytunnels.android.utils.SubscriptionUtil;

import org.json.JSONObject;
import org.strongswan.android.BuildConfig;
import org.strongswan.android.R;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.data.VpnType.VpnTypeFeature;
import org.strongswan.android.logic.CharonVpnService;
import org.strongswan.android.logic.StrongSwanApplication;
import org.strongswan.android.logic.TrustedCertificateManager;
import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.logic.VpnStateService.State;
import org.strongswan.android.security.TrustedCertificateEntry;
import org.strongswan.android.ui.LogActivity;
import org.strongswan.android.ui.VpnProfileImportActivity;
import org.strongswan.android.ui.TrustedCertificatesActivity;
import com.trytunnels.android.ui.ttVpnStateFragment.OnVpnProfileSelectedListener;
import org.strongswan.android.utils.Constants;
import org.strongswan.android.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnVpnProfileSelectedListener {
    public static final String CONTACT_EMAIL = "android@strongswan.org";
    public static final String START_PROFILE = "org.strongswan.android.action.START_PROFILE";
    public static final String DISCONNECT = "org.strongswan.android.action.DISCONNECT";
    public static final String EXTRA_VPN_PROFILE_ID = "org.strongswan.android.VPN_PROFILE_ID";
    public static final String EXTRA_CRL_LIST = "org.strongswan.android.CRL_LIST";
    /**
     * Use "bring your own device" (BYOD) features
     */
    public static final boolean USE_BYOD = true;
    private static final int PREPARE_VPN_SERVICE = 0;
    private static final int INSTALL_PKCS12 = 1;
    private static final int SIGNED_OUT = 2;
    private static final int ANNUAL_POPUP = 3;
    private static final int VPN_SETTINGS = 4;
    private static final int POSTBOARDING = 5;
    private static final int WELCOME_PAGE = 6;
    private static final String PROFILE_NAME = "org.strongswan.android.MainActivity.PROFILE_NAME";
    private static final String PROFILE_REQUIRES_PASSWORD = "org.strongswan.android.MainActivity.REQUIRES_PASSWORD";
    private static final String PROFILE_RECONNECT = "org.strongswan.android.MainActivity.RECONNECT";
    private static final String PROFILE_DISCONNECT = "org.strongswan.android.MainActivity.DISCONNECT";
    private static final String PROFILE_FOREGROUND = "org.strongswan.android.MainActivity.PROFILE_FOREGROUND";
    private static final String DIALOG_TAG = "Dialog";

    private static final String TAG = "MainActivity";

    //public static final String confirmedServer = "trusty-eu.science";
    //public static final String confirmedServer = "confirmedvpn.com";

    private DrawerLayout mDrawerLayout;

    private boolean mIsVisible;
    private Bundle mProfileInfo;
    private VpnStateService mService;

    // tunnels specific
    private VpnProfileDataSource mVpnProfileDataSource;
    private VpnProfile mVpnProfile;
    private SubscriptionUtil mSubscriptionUtil;
    private AppCompatSpinner mSpinnerCountrySelection;
    ttVpnStateFragment mttVpnStateFragment;

    private TrustedCertificateEntry mCertEntry;
    private String mUserCertLoading;
    private TrustedCertificateEntry mUserCertEntry;

    private boolean isPostBoarding = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((VpnStateService.LocalBinder) service).getService();

            if (START_PROFILE.equals(getIntent().getAction())) {
                startVpnProfile(getIntent(), false);
            } else if (DISCONNECT.equals(getIntent().getAction())) {
                disconnect(false);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.tt_main);


        /*if(BuildConfig.DEBUG)
        {
            Heap.init(getApplicationContext(), "2181445519");
        }
        else
        {
            Heap.init(getApplicationContext(), "911473178");
        }*/

        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        //bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeAsUpIndicator(R.drawable.tt_menu_hamburger_icon_24dp);
        bar.setDisplayShowTitleEnabled(false);
        //bar.setIcon(R.mipmap.tt_ic_launcher);

        this.bindService(new Intent(this, VpnStateService.class),
                mServiceConnection, Service.BIND_AUTO_CREATE);

		/* load CA certificates in a background task */
        new LoadCertificatesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        PreferenceStoreUtil.getInstance().initializeStore(StrongSwanApplication.getContext());

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new ConfirmedDrawerNavigation());

        ttInitVpnProfile();
        ttInitVpnStateFragment();
        ttInitCountrySelection();

        ttLaunchWelcomeActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            this.unbindService(mServiceConnection);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsVisible = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsVisible = false;
    }

    private BroadcastReceiver speedTestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView resultView = findViewById(R.id.tvSpeedTestResult);
            LinearLayout resultLayout = findViewById(R.id.layout_speed_result);
            resultLayout.setVisibility(View.VISIBLE);
            resultView.setText(intent.getStringExtra(SpeedTestService.resultKey));
            resultLayout.clearAnimation();
        }
    };

    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(speedTestReceiver, new IntentFilter(SpeedTestService.intentName));
    }

    /**
     * Due to launchMode=singleTop this is called if the Activity already exists; disabled for now
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d(TAG, "new intent");

        if (START_PROFILE.equals(intent.getAction())) {
            startVpnProfile(intent, mIsVisible);
        } else if (DISCONNECT.equals(intent.getAction())) {
            disconnect(mIsVisible);
        }
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tt_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            menu.removeItem(R.id.menu_import_profile);
        }
        return true;
    }*/

    protected class ConfirmedDrawerNavigation implements NavigationView.OnNavigationItemSelectedListener
    {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem)
        {
            //menuItem.setChecked(true);

            mDrawerLayout.closeDrawers();

            switch(menuItem.getItemId())
            {
                case R.id.menu_show_help:
                    sendLogEmail(MainActivity.this);
                    return true;
                case R.id.menu_show_welcome:
                    Intent welcomeIntent = new Intent(getApplicationContext(), WelcomeActivity.class);
                    welcomeIntent.putExtra("HAS_SUBSCRIPTION", true);
                    startActivity(welcomeIntent);
                    return true;
                /*case R.id.menu_show_account:
                    Intent accountIntent = new Intent(getApplicationContext(), AccountActivity.class);
                    startActivityForResult(accountIntent, SIGNED_OUT);
                    return true;*/
                case R.id.menu_speed_test:
                    TextView resultView = findViewById(R.id.tvSpeedTestResult);
                    LinearLayout resultLayout = findViewById(R.id.layout_speed_result);
                    resultLayout.setVisibility(View.VISIBLE);
                    resultView.setText("... Mbps");
                    Animation alphaAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha_pulse);
                    //alphaAnim.setRepeatCount(Animation.INFINITE);
                    //resultView.startAnimation(alphaAnim);
                    resultLayout.startAnimation(alphaAnim);
                    Intent speedTestIntent = new Intent(getApplicationContext(), SpeedTestService.class);
                    startService(speedTestIntent);
                    return true;
                case R.id.menu_vpn_settings:
                    Intent vpnSettingsIntent = new Intent(getApplicationContext(), VPNSettingsActivity.class);
                    startActivityForResult(vpnSettingsIntent, VPN_SETTINGS);
                default:
            }

            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*case R.id.menu_import_profile:
                Intent intent = new Intent(this, VpnProfileImportActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_manage_certs:
                Intent certIntent = new Intent(this, TrustedCertificatesActivity.class);
                startActivity(certIntent);
                return true;
            case R.id.menu_crl_cache:
                clearCRLs();
                return true;
            case R.id.menu_show_log:
                Intent logIntent = new Intent(this, LogActivity.class);
                startActivity(logIntent);
                return true;*/
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            /*case R.id.menu_show_help:
                sendLogEmail();
                return true;
            case R.id.menu_show_welcome:
                Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
                welcomeIntent.putExtra("HAS_SUBSCRIPTION", true);
                startActivity(welcomeIntent);
                return true;*/
            /*case R.id.menu_show_account:
                Intent accountIntent = new Intent(this, AccountActivity.class);
                startActivityForResult(accountIntent, SIGNED_OUT);
                return true;*/
            /*case R.id.menu_speed_test:
                TextView resultView = findViewById(R.id.tvSpeedTestResult);
                resultView.setVisibility(View.VISIBLE);
                resultView.setText("... Mbps");
                Animation alphaAnim = AnimationUtils.loadAnimation(this, R.anim.alpha_pulse);
                //alphaAnim.setRepeatCount(Animation.INFINITE);
                resultView.startAnimation(alphaAnim);
                Intent speedTestIntent = new Intent(this, SpeedTestService.class);
                startService(speedTestIntent);
                return true;
            case R.id.menu_vpn_settings:
                Intent vpnSettingsIntent = new Intent(this, VPNSettingsActivity.class);
                startActivityForResult(vpnSettingsIntent, VPN_SETTINGS);*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void accountButtonClicked(View v)
    {
        mDrawerLayout.closeDrawers();

        Intent accountIntent = new Intent(this, AccountActivity.class);
        startActivityForResult(accountIntent, SIGNED_OUT);
    }

    public static void sendLogEmail(Context context) {
        Intent helpIntent = new Intent(Intent.ACTION_SENDTO);
        helpIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
        helpIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"team@confirmedvpn.com"});
        helpIntent.putExtra(Intent.EXTRA_SUBJECT, "Confirmed VPN Issue");
        helpIntent.putExtra(Intent.EXTRA_TEXT, "Hey Confirmed Team,\nI have an issue with Confirmed -\n\n");

        File logFileName = extractLogToFile(context);
        //ArrayList<Uri> uris = new ArrayList<Uri>();
        Uri contentUri = FileProvider.getUriForFile(context, "com.confirmed.tunnels.fileprovider", logFileName);
        //uris.add(contentUri);
        helpIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        helpIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(helpIntent, 0);
        if(resolveInfos.size() > 0) {
            String packageName = resolveInfos.get(0).activityInfo.packageName;
            String name = resolveInfos.get(0).activityInfo.name;

            helpIntent.setAction(Intent.ACTION_SEND);
            helpIntent.setComponent(new ComponentName(packageName, name));

            context.startActivity(helpIntent);
        }
    }

    private static File extractLogToFile(Context context) {

        PackageManager manager = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e2) {
        }
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER))
            model = Build.MANUFACTURER + " " + model;

        // Extract to file.
        File file = new File(context.getCacheDir(), "ConfirmedVPNLog.log");

        InputStreamReader reader = null;
        FileWriter writer = null;

        try {
            // For Android 4.0 and earlier, you will get all app's log output, so filter it to
            // mostly limit it to your app's output.  In later versions, the filtering isn't needed.
            String cmd = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) ?
                    "logcat -d -v time MyApp:v dalvikvm:v System.err:v *:s" :
                    "logcat -d -v time";

            // get input stream
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new InputStreamReader(process.getInputStream());

            // write output stream
            writer = new FileWriter(file);
            writer.write("Android version: " + Build.VERSION.SDK_INT + "\n");
            writer.write("Device: " + model + "\n");
            writer.write("App version: " + (info == null ? "(null)" : info.versionCode) + "\n");

            char[] buffer = new char[10000];
            do {
                int n = reader.read(buffer, 0, buffer.length);
                if (n == -1)
                    break;
                writer.write(buffer, 0, n);
            } while (true);

            reader.close();
            writer.close();
        } catch (IOException e) {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e1) {
                }
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                }

            // You might want to write a failure message to the log here.
            return null;
        }

        return file;
    }

    protected void ttInitVpnProfile() {
        mVpnProfileDataSource = new VpnProfileDataSource(this);

        mVpnProfileDataSource.open();
        List<VpnProfile> existingVpnProfiles = mVpnProfileDataSource.getAllVpnProfiles();

        Log.d(TAG, "ttInitVpnProfile: has " + existingVpnProfiles.size() + " existing vpn profiles");

        if (existingVpnProfiles.size() > 0) {

            // clean up if we have more than one profile for some reason (likely from testing)
            int i;
            for (i = 0; i < (existingVpnProfiles.size() - 1); i++) {
                mVpnProfileDataSource.deleteVpnProfile(existingVpnProfiles.get(i));
            }

            // always use the most recent one (should be only one)
            mVpnProfile = existingVpnProfiles.get(i);
            Log.d(TAG, "ttInitVpnProfile: got profile name: " + mVpnProfile.getName() +
                    " user cert: " + mVpnProfile.getUserCertificateAlias() +
                    " server: " + mVpnProfile.getGateway());
        }
        mVpnProfileDataSource.close();
    }

    protected void ttInitVpnStateFragment() {
        mttVpnStateFragment = (ttVpnStateFragment) getSupportFragmentManager().findFragmentById(R.id.tt_vpn_state_frag);
    }

    protected void ttInitCountrySelection() {
        mSpinnerCountrySelection = findViewById(R.id.spinnerCountrySelection);

        ArrayList<CountrySelectionModel> list = new ArrayList<>();
        list.add(new CountrySelectionModel("United States - West", R.drawable.tt_cc_us, "us-west"));
        list.add(new CountrySelectionModel("United States - East", R.drawable.tt_cc_us, "us-east"));
        list.add(new CountrySelectionModel("United Kingdom", R.drawable.tt_cc_gb, "eu-london"));
        list.add(new CountrySelectionModel("Ireland", R.drawable.tt_cc_ie, "eu-ireland"));
        list.add(new CountrySelectionModel("Germany", R.drawable.tt_cc_de, "eu-frankfurt"));
        list.add(new CountrySelectionModel("Canada", R.drawable.tt_cc_ca, "canada"));
        list.add(new CountrySelectionModel("Japan", R.drawable.tt_cc_jp, "ap-tokyo"));
        list.add(new CountrySelectionModel("Australia", R.drawable.tt_cc_au, "ap-sydney"));
        list.add(new CountrySelectionModel("South Korea", R.drawable.tt_cc_kr, "ap-seoul"));
        list.add(new CountrySelectionModel("Singapore", R.drawable.tt_cc_sg, "ap-singapore"));
        list.add(new CountrySelectionModel("India", R.drawable.tt_cc_in, "ap-mumbai"));
        list.add(new CountrySelectionModel("Brazil", R.drawable.tt_cc_br, "sa"));
        CountrySelectionAdapter adapter = new CountrySelectionAdapter(this,
                R.layout.tt_country_selection_spinner_item, R.id.txt);
        adapter.setData(list);
        mSpinnerCountrySelection.setAdapter(adapter);

        // set initial value before we enable on selected listener
        if (mVpnProfile != null) {
            int i = 0;
            for (CountrySelectionModel countrySelectionModel : list) {
                if (mVpnProfile.getGateway().equals(ConfirmedConstants.getEndPoint(countrySelectionModel.getEndpoint()))) {
                    mSpinnerCountrySelection.setSelection(i);
                    break;
                }
                i++;
            }
        }

        mSpinnerCountrySelection.setOnItemSelectedListener(new ttOnCountrySelected());
    }

    private void setCurrentCountry(String country)
    {
        if(mSpinnerCountrySelection != null)
        {
            for (int position = 0; position < mSpinnerCountrySelection.getCount(); position++) {
                CountrySelectionModel countrySelectionModel = (CountrySelectionModel) mSpinnerCountrySelection.getItemAtPosition(position);

                String endpoint = countrySelectionModel.getEndpoint();

                if (country.equals(endpoint)) {
                    final Handler mHandler = new Handler(Looper.getMainLooper());

                    final int setPosition = position;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mSpinnerCountrySelection.setSelection(setPosition);
                        }
                    });
                    break;
                }
            }
        }
    }

    protected class ttOnCountrySelected implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            //Log.d(TAG, "item selected with endpoint: " + selectedItemView.getTag());
            ttUpdateVpnProfileGateway();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
        }
    }

    /**
     * Launch welcome activity if no active sub
     */
    protected void ttLaunchWelcomeActivity() {

        /*Intent welcomeIntent = new Intent(StrongSwanApplication.getContext(), WelcomeActivity.class);
        welcomeIntent.putExtra("HAS_SUBSCRIPTION", false);
        if (mVpnProfileDataSource != null) {
            mVpnProfileDataSource.open();
            mVpnProfileDataSource.deleteAllVPNs();
            mVpnProfileDataSource.close();
        }
        mVpnProfile = null;
        PreferenceStoreUtil.getInstance().putBoolean(getString(R.string.tt_correct_start_key), false);
        PreferenceStoreUtil.getInstance().putString(getString(R.string.tt_email_store_key), "");
        PreferenceStoreUtil.getInstance().remove(getString(R.string.tt_exclude_list));
        startActivityForResult(welcomeIntent, WELCOME_PAGE); */

        mSubscriptionUtil = new SubscriptionUtil(MainActivity.this, new SubscriptionUtil.SubscriptionSetupListener() {
            @Override
            public void onSuccess() {

                final String currReceipt = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_paid_sub_info_key), "");

                List<String> subTypes = Arrays.asList(getString(R.string.tt_paid_unlimited_sub_key), getString(R.string.tt_paid_sub_key), getString(R.string.tt_paid_sub_key_annual), getString(R.string.tt_paid_unlimited_sub_key_annual));
                mSubscriptionUtil.checkAllConfirmedSubs(subTypes, new SubscriptionUtil.SubscriptionInventoryListener() {
                    @Override
                    public void onSuccess(boolean hasPurchase) {
                        if (!hasPurchase) {

                            PreferenceStoreUtil.getInstance().putString(getString(R.string.tt_paid_sub_info_key), "");
                            PreferenceStoreUtil.getInstance().putString(SubscriptionUtil.mCurrProductIdKey, "");
                            PreferenceStoreUtil.getInstance().putLong(getString(R.string.tt_receipt_start_date), 0);

                            final Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {

                                        String email = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_email_store_key), "");
                                        String password = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_password_store_key), "");

                                        if (!email.equals("") && !password.equals("")) {
                                            if(ApiUtil.getInstance().initiateClientSync(getApplicationContext(), email, password)) {

                                                try {
                                                    Response response = ApiUtil.getInstance().getKeySync();

                                                    if (response.body() == null) {
                                                        throw new Exception("Something wrong");
                                                    }

                                                    String responseString = response.body().string();
                                                    response.body().close();
                                                    Log.d(TAG, "Response String: " + responseString);

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

                                                    // we have a valid login
                                                    return;
                                                } catch (Exception e) {
                                                    Log.d(TAG, "getKey error: " + e.getMessage());
                                                }
                                            }
                                        }


                                    Intent welcomeIntent = new Intent(StrongSwanApplication.getContext(), WelcomeActivity.class);
                                    welcomeIntent.putExtra("HAS_SUBSCRIPTION", false);

                                    //ttNullifyVpnProfileGateway();
                                    if (mVpnProfileDataSource != null) {
                                        mVpnProfileDataSource.open();
                                        mVpnProfileDataSource.deleteAllVPNs();
                                        mVpnProfileDataSource.close();
                                    }
                                    mVpnProfile = null;
                                    PreferenceStoreUtil.getInstance().putBoolean(getString(R.string.tt_correct_start_key), false);
                                    PreferenceStoreUtil.getInstance().putString(getString(R.string.tt_email_store_key), "");
                                    PreferenceStoreUtil.getInstance().remove(getString(R.string.tt_exclude_list));
                                    startActivityForResult(welcomeIntent, WELCOME_PAGE);
                                }
                            });

                            thread.start();
                        }
                        // receipt was updated
                        else
                        {
                            // initialize api with receipt login
                            ApiUtil.getInstance().initiateClient(getApplicationContext(), currReceipt);

                            boolean showedAnnualPopup = PreferenceStoreUtil.getInstance().getBoolean(getString(R.string.tt_showed_annual_popup), false);

                            if(!showedAnnualPopup) {
                                // check matches current receipt
                                String currProdId = PreferenceStoreUtil.getInstance().getString(SubscriptionUtil.mCurrProductIdKey, "");

                                if(!currProdId.contains("annual")) {
                                    String newReceipt = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_paid_sub_info_key), "");

                                    if (!newReceipt.equals("") && newReceipt.equals(currReceipt)) {
                                        Long receiptStartTime = PreferenceStoreUtil.getInstance().getLong(getString(R.string.tt_receipt_start_date), 0);

                                        // compare to current time
                                        if (receiptStartTime != 0) {
                                            Long diffTime = System.currentTimeMillis() - receiptStartTime;

                                            Long daysDiff = diffTime / (1000 * 60 * 60 * 24);

                                            // let's go with 40 days
                                            if (daysDiff > 40) {
                                                PreferenceStoreUtil.getInstance().putBoolean(getString(R.string.tt_showed_annual_popup), true);

                                                Intent accountIntent = new Intent(getApplicationContext(), UpgradeAnnualActivity.class);
                                                startActivityForResult(accountIntent, ANNUAL_POPUP);
                                            }
                                        }
                                        else
                                        {
                                            PreferenceStoreUtil.getInstance().putLong(getString(R.string.tt_receipt_start_date), System.currentTimeMillis());
                                        }
                                    } else if (!newReceipt.equals("")) {
                                        PreferenceStoreUtil.getInstance().putLong(getString(R.string.tt_receipt_start_date), System.currentTimeMillis());
                                    }
                                }
                                // own annual so don't bother showing
                                else
                                {
                                    PreferenceStoreUtil.getInstance().putBoolean(getString(R.string.tt_showed_annual_popup), true);
                                }
                            }
                        }
                    }
                });
            }
        });

    }


    /**
     * Process the certJson from server and create a vpn profile
     *
     * @param certJson
     */
    protected void ttCreateVpnProfile(JSONObject certJson) throws Exception {
        String closetRegion = getClosestRegion();
        skipNextCountryChange = true;
        setCurrentCountry(closetRegion);

        mVpnProfile = new VpnProfile();
        JSONObject remoteJson = (JSONObject) certJson.get("remote");

        JSONObject localJson = (JSONObject) certJson.get("local");
        String p12 = localJson.getString("p12");

        mVpnProfile.setVpnType(VpnType.IKEV2_CERT);
        mVpnProfile.setUUID(UUID.fromString(certJson.getString("uuid")));
        mVpnProfile.setName(certJson.getString("name"));
        mVpnProfile.setGateway(ConfirmedConstants.getEndPoint(closetRegion));
        mVpnProfile.setRemoteId(ConfirmedConstants.getRemoteId());
        mVpnProfile.setMTU(certJson.getInt("mtu"));
        mVpnProfile.setSelectedAppsHandling(VpnProfile.SelectedAppsHandling.SELECTED_APPS_DISABLE);
        mVpnProfile.setLocalId(localJson.getString("id"));
        mVpnProfile.setUserCertificateAlias("Certificate for \"" + mVpnProfile.getName() + "\"");
        mVpnProfile.setSplitTunneling(VpnProfile.SPLIT_TUNNELING_BLOCK_IPV6);

        Intent intent = KeyChain.createInstallIntent();
        intent.putExtra(KeyChain.EXTRA_NAME, getString(R.string.profile_cert_alias, mVpnProfile.getName()));
        intent.putExtra(KeyChain.EXTRA_PKCS12, Base64.decode(p12.getBytes(), Base64.DEFAULT));
        startActivityForResult(intent, INSTALL_PKCS12);
    }

    protected void ttCreateVpnProfile(String id, String encodedP12) throws Exception {
        String closetRegion = getClosestRegion();
        skipNextCountryChange = true;
        setCurrentCountry(closetRegion);

        mVpnProfile = new VpnProfile();

        mVpnProfile.setVpnType(VpnType.IKEV2_CERT);
        mVpnProfile.setUUID(UUID.randomUUID());
        mVpnProfile.setName("Confirmed VPN");
        mVpnProfile.setGateway(ConfirmedConstants.getEndPoint(closetRegion));
        mVpnProfile.setRemoteId(ConfirmedConstants.getRemoteId());
        mVpnProfile.setMTU(1280);
        mVpnProfile.setSelectedAppsHandling(VpnProfile.SelectedAppsHandling.SELECTED_APPS_DISABLE);
        mVpnProfile.setLocalId(id);
        mVpnProfile.setUserCertificateAlias("Certificate for \"" + mVpnProfile.getName() + "\"");
        mVpnProfile.setSplitTunneling(VpnProfile.SPLIT_TUNNELING_BLOCK_IPV6);

        Intent intent = KeyChain.createInstallIntent();
        intent.putExtra(KeyChain.EXTRA_NAME, getString(R.string.profile_cert_alias, mVpnProfile.getName()));
        intent.putExtra(KeyChain.EXTRA_PKCS12, Base64.decode(encodedP12.getBytes(), Base64.DEFAULT));
        startActivityForResult(intent, INSTALL_PKCS12);
    }

    private String getClosestRegion()
    {
        String defaultRegion = "us-west";

        String regionCode = Locale.getDefault().getCountry();

        if (regionCode.equals("US"))
        {
            String theTZ = TimeZone.getDefault().getDisplayName();
            if (theTZ.equals("Eastern Standard Time") || theTZ.equals("Central Standard Time"))
            {
                defaultRegion = "us-east";
            }
        }
        if (regionCode.equals("GB"))
        {
            defaultRegion = "eu-london";
        }
        if (regionCode.equals("IE"))
        {
            defaultRegion = "eu-ireland";
        }
        if (regionCode.equals("CA"))
        {
            defaultRegion = "canada";
        }
        if (regionCode.equals("KO"))
        {
            defaultRegion = "ap-seoul";
        }
        if (regionCode.equals("SG"))
        {
            defaultRegion = "ap-singapore";
        }
        if (regionCode.equals("DE") || regionCode.equals("FR") || regionCode.equals("IT") || regionCode.equals("PT") ||
                regionCode.equals("ES") || regionCode.equals("AT") || regionCode.equals("PL") || regionCode.equals("RU") ||
                regionCode.equals("UA"))
        {
            defaultRegion = "eu-frankfurt";
        }
        if (regionCode.equals("AU") || regionCode.equals("NZ"))
        {
            defaultRegion = "ap-sydney";
        }
        if (regionCode.equals("JP"))
        {
            defaultRegion = "ap-tokyo";
        }
        if (regionCode.equals("IN") || regionCode.equals("PK") || regionCode.equals("BD"))
        {
            defaultRegion = "ap-mumbai";
        }
        if (regionCode.equals("BR") || regionCode.equals("CO") || regionCode.equals("VE") || regionCode.equals("AR"))
        {
            defaultRegion = "sa";
        }

        return defaultRegion;
    }

    boolean skipNextCountryChange = false;
    /**
     * New country selected
     *
     */
    protected void ttUpdateVpnProfileGateway()  {
        Log.d(TAG, "ttUpdateVpnProfileGateway: hi");

        if (mVpnProfile == null || skipNextCountryChange)
        {
            skipNextCountryChange = false;
            return;
        }

        CountrySelectionModel countrySelectionModel = (CountrySelectionModel ) mSpinnerCountrySelection.getSelectedItem();

        String endpoint = ConfirmedConstants.getEndPoint(countrySelectionModel.getEndpoint());
        if (mVpnProfile.getGateway().equals(endpoint)) {
            return;
        }

        mVpnProfile.setGateway(endpoint);

        mVpnProfileDataSource.open();
        mVpnProfileDataSource.updateVpnProfile(mVpnProfile);
        mVpnProfileDataSource.close();

        Intent intent = new Intent(Constants.VPN_PROFILES_CHANGED);
        intent.putExtra(Constants.VPN_PROFILES_SINGLE, mVpnProfile.getId());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if (mService != null && (mService.getState() == State.CONNECTED || mService.getState() == State.CONNECTING)) {
            mttVpnStateFragment.setReconnect(true);
            mttVpnStateFragment.triggerVpnPowerClick();
        }
    }

    protected void ttNullifyVpnProfileGateway()  {
        Log.d(TAG, "ttNullifyVpnProfileGateway: hi");

        if (mVpnProfile == null) {
            return;
        }

        String endpoint = "local.confirmed";
        if (mVpnProfile.getGateway().equals(endpoint)) {
            return;
        }

        mVpnProfile.setGateway(endpoint);

        mVpnProfileDataSource.open();
        mVpnProfileDataSource.updateVpnProfile(mVpnProfile);
        mVpnProfileDataSource.close();

        Intent intent = new Intent(Constants.VPN_PROFILES_CHANGED);
        intent.putExtra(Constants.VPN_PROFILES_SINGLE, mVpnProfile.getId());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if (mService != null && (mService.getState() == State.CONNECTED || mService.getState() == State.CONNECTING)) {
            mttVpnStateFragment.setReconnect(true);
            mttVpnStateFragment.triggerVpnPowerClick();
        }
    }

    protected void ttUpdateVpnProfileUserCertificateAlias()  {
        Log.d(TAG, "ttUpdateVpnProfileUserCertificateAlias: came in with alias: " + mUserCertEntry.getAlias());

        mVpnProfile.setUserCertificateAlias(mUserCertEntry.getAlias());

        VpnProfile existingVpnProfile;

        mVpnProfileDataSource.open();
        existingVpnProfile = mVpnProfileDataSource.getVpnProfile(mVpnProfile.getUUID());
        if (existingVpnProfile != null) {
            mVpnProfile.setId(existingVpnProfile.getId());
            mVpnProfileDataSource.updateVpnProfile(mVpnProfile);
        } else {
            mVpnProfileDataSource.insertProfile(mVpnProfile);
        }
        mVpnProfileDataSource.close();

        Intent intent = new Intent(Constants.VPN_PROFILES_CHANGED);
        intent.putExtra(Constants.VPN_PROFILES_SINGLE, mVpnProfile.getId());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if(isPostBoarding)
        {
            Intent startPostboardingIntent = new Intent(this, StartPostboardingActivity.class);
            startPostboardingIntent.putExtra("IS_VPN_ON", true);
            startActivityForResult(startPostboardingIntent, POSTBOARDING);
        }
        // should be first time starting VPN
        else if(!PreferenceStoreUtil.getInstance().getBoolean(getString(R.string.tt_correct_start_key), false))
        {
            Intent vpnSettingsIntent = new Intent(this, VPNSettingsActivity.class);
            vpnSettingsIntent.putExtra("USE_DEFAULT", true);
            startActivityForResult(vpnSettingsIntent, VPN_SETTINGS);
        }

        PreferenceStoreUtil.getInstance().putBoolean(getString(R.string.tt_correct_start_key), true);

        // launch vpn
        //ttOnVpnProfileSelected();
        mttVpnStateFragment.triggerVpnPowerClick();
    }

    private class ttKeychainHelper implements KeyChainAliasCallback {
        @Override
        public void alias(final String alias) {
            if (alias != null)  {	/* otherwise the dialog was canceled, the request denied */
                Log.d(TAG, "ttKeychainHelper::alias: came in with alias: " + alias);
                try {
                    ttUserCertificateLoader loader = new ttUserCertificateLoader(StrongSwanApplication.getContext(), alias);
                    mUserCertLoading = alias;
                    loader.execute();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
            {
                mttVpnStateFragment.setPowerButtonObjects();
            }
        }
    }

    /**
     * Load the selected user certificate asynchronously.  This cannot be done
     * from the main thread as getCertificateChain() calls back to our main
     * thread to bind to the KeyChain service resulting in a deadlock.
     */
    private class ttUserCertificateLoader extends AsyncTask<Void, Void, X509Certificate> {
        private final Context mContext;
        private final String mAlias;

        public ttUserCertificateLoader(Context context, String alias) {
            mContext = context;
            mAlias = alias;
        }

        @Override
        protected X509Certificate doInBackground(Void... params) {
            X509Certificate[] chain = null;
            try {
                Log.d(TAG, "ttUserCertificateLoader::doInBackground: came in with alias: " + mAlias);
                chain = KeyChain.getCertificateChain(mContext, mAlias);
            }
            catch (KeyChainException | InterruptedException e) {
                e.printStackTrace();
            }
            if (chain != null && chain.length > 0) {
                return chain[0];
            }
            return null;
        }

        @Override
        protected void onPostExecute(X509Certificate result)
        {
            if (result != null) {
                mUserCertEntry = new TrustedCertificateEntry(mAlias, result);
            }
            else {	/* previously selected certificate is not here anymore */
                mUserCertEntry = null;
            }
            mUserCertLoading = null;

            ttUpdateVpnProfileUserCertificateAlias();
        }
    }

    /**
     * Prepare the VpnService. If this succeeds the current VPN profile is
     * started.
     *
     * @param profileInfo a bundle containing the information about the profile to be started
     */
    protected void prepareVpnService(Bundle profileInfo) {
        Intent intent;
        try {
            intent = VpnService.prepare(this);
        } catch (IllegalStateException ex) {
            /* this happens if the always-on VPN feature (Android 4.2+) is activated */
            VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported_during_lockdown);
            return;
        } catch (NullPointerException ex) {
			/* not sure when this happens exactly, but apparently it does */
            VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported);
            return;
        }
		/* store profile info until the user grants us permission */
        mProfileInfo = profileInfo;
        if (intent != null) {
            try {
                startActivityForResult(intent, PREPARE_VPN_SERVICE);
            } catch (ActivityNotFoundException ex) {
				/* it seems some devices, even though they come with Android 4,
				 * don't have the VPN components built into the system image.
				 * com.android.vpndialogs/com.android.vpndialogs.ConfirmDialog
				 * will not be found then */
                VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported);
            }
        } else {	/* user already granted permission to use VpnService */
            onActivityResult(PREPARE_VPN_SERVICE, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PREPARE_VPN_SERVICE:
                if (resultCode == RESULT_OK && mProfileInfo != null) {
                    Intent intent = new Intent(this, CharonVpnService.class);
                    intent.putExtras(mProfileInfo);
                    this.startService(intent);
                }
                break;
            case INSTALL_PKCS12:
                if (resultCode == Activity.RESULT_OK && mVpnProfile != null) {
                    String userCertAlias = mVpnProfile.getUserCertificateAlias();
                    ttKeychainHelper ttKeychainHelper = new ttKeychainHelper();
                    KeyChain.choosePrivateKeyAlias(this, ttKeychainHelper, new String[]{"RSA"}, null, null, -1, userCertAlias);
                }
                else
                {
                    mVpnProfile = null;
                    mttVpnStateFragment.setPowerButtonObjects();
                }
                break;
            case SIGNED_OUT:
                if(resultCode == 1)
                {
                    // signed out no receipt
                    if(PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_paid_sub_info_key),"").equals(""))
                    {
                        if(mService != null) {
                            mService.disconnect();
                        }
                        //ttNullifyVpnProfileGateway();
                        if(mVpnProfileDataSource != null) {
                            mVpnProfileDataSource.open();
                            mVpnProfileDataSource.deleteAllVPNs();
                            mVpnProfileDataSource.close();
                        }
                        mVpnProfile = null;
                        PreferenceStoreUtil.getInstance().putBoolean(getString(R.string.tt_correct_start_key), false);
                        ttLaunchWelcomeActivity();
                    }
                }
                break;
            case ANNUAL_POPUP:
                if(resultCode == 1)
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
                break;
            case VPN_SETTINGS:
                if(resultCode == 1)
                {
                    // if vpn running, restart
                    if(mService != null && mService.getState() == State.CONNECTED)
                    {
                        mttVpnStateFragment.setReconnect(true);
                        mttVpnStateFragment.triggerVpnPowerClick();
                    }
                }

                if(isPostBoarding)
                {
                    isPostBoarding = false;
                    Intent addEmailIntent = new Intent(this, AddEmailActivity.class);
                    startActivity(addEmailIntent);
                }
                break;
            case WELCOME_PAGE:
                if(resultCode == 1)
                {
                    if(!PreferenceStoreUtil.getInstance().getBoolean("showedPostboarding", false))
                    {
                        isPostBoarding = true;
                        PreferenceStoreUtil.getInstance().putBoolean("showedPostboarding", true);
                        Intent postBoarding = new Intent(this, StartPostboardingActivity.class);
                        startActivityForResult(postBoarding, POSTBOARDING);
                    }
                }
                break;
            case POSTBOARDING:
                if(resultCode == 1)
                {
                    mttVpnStateFragment.triggerVpnPowerClick();
                }
                else if(resultCode == 2)
                {
                    Intent vpnSettings = new Intent (this, VPNSettingsActivity.class);
                    startActivityForResult(vpnSettings, VPN_SETTINGS);
                }
                break;
            default:
        }
    }

    @Override
    public void onVpnProfileSelected(VpnProfile profile) {
        startVpnProfile(profile, true);
    }

    public void ttOnVpnProfileSelected() {
        startVpnProfile(mVpnProfile, true);
    }

    /*public SecuredPreferenceStore ttGetSecuredPreferenceStore() {
        return mPrefStore;
    }*/

    public VpnProfile ttGetVpnProfile() {
        return mVpnProfile;
    }

    /**
     * Start the given VPN profile
     *
     * @param profile    VPN profile
     * @param foreground whether this was initiated when the activity was visible
     */
    public void startVpnProfile(VpnProfile profile, boolean foreground) {
        Bundle profileInfo = new Bundle();
        profileInfo.putLong(VpnProfileDataSource.KEY_ID, profile.getId());
        profileInfo.putString(VpnProfileDataSource.KEY_USERNAME, profile.getUsername());
        profileInfo.putString(VpnProfileDataSource.KEY_PASSWORD, profile.getPassword());
        profileInfo.putBoolean(PROFILE_REQUIRES_PASSWORD, profile.getVpnType().has(VpnTypeFeature.USER_PASS));
        profileInfo.putString(PROFILE_NAME, profile.getName());

        removeFragmentByTag(DIALOG_TAG);

        if (mService != null && (mService.getState() == State.CONNECTED || mService.getState() == State.CONNECTING)) {
            profileInfo.putBoolean(PROFILE_RECONNECT, mService.getProfile().getId() == profile.getId());
            profileInfo.putBoolean(PROFILE_FOREGROUND, foreground);

            ConfirmationDialog dialog = new ConfirmationDialog();
            dialog.setArguments(profileInfo);
            dialog.show(this.getSupportFragmentManager(), DIALOG_TAG);
            return;
        }
        startVpnProfile(profileInfo);
    }

    /**
     * Start the given VPN profile asking the user for a password if required.
     *
     * @param profileInfo data about the profile
     */
    private void startVpnProfile(Bundle profileInfo) {
        if (profileInfo.getBoolean(PROFILE_REQUIRES_PASSWORD) &&
                profileInfo.getString(VpnProfileDataSource.KEY_PASSWORD) == null) {
            LoginDialog login = new LoginDialog();
            login.setArguments(profileInfo);
            login.show(getSupportFragmentManager(), DIALOG_TAG);
            return;
        }
        prepareVpnService(profileInfo);
    }

    /**
     * Start the VPN profile referred to by the given intent. Displays an error
     * if the profile doesn't exist.
     *
     * @param intent     Intent that caused us to start this
     * @param foreground whether this was initiated when the activity was visible
     */
    private void startVpnProfile(Intent intent, boolean foreground) {
        long profileId = intent.getLongExtra(EXTRA_VPN_PROFILE_ID, 0);
        if (profileId <= 0) {	/* invalid invocation */
            return;
        }
        VpnProfileDataSource dataSource = new VpnProfileDataSource(this);
        dataSource.open();
        VpnProfile profile = dataSource.getVpnProfile(profileId);
        dataSource.close();

        if (profile != null) {
            startVpnProfile(profile, foreground);
        } else {
            Toast.makeText(this, R.string.profile_not_found, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Disconnect the current connection, if any (silently ignored if there is no connection).
     */
    private void disconnect(boolean foreground) {
        removeFragmentByTag(DIALOG_TAG);

        if (mService != null && (mService.getState() == State.CONNECTED || mService.getState() == State.CONNECTING)) {
            Bundle args = new Bundle();
            args.putBoolean(PROFILE_DISCONNECT, true);
            args.putBoolean(PROFILE_FOREGROUND, foreground);

            ConfirmationDialog dialog = new ConfirmationDialog();
            dialog.setArguments(args);
            dialog.show(this.getSupportFragmentManager(), DIALOG_TAG);
        }
    }

    /**
     * Ask the user whether to clear the CRL cache.
     */
    private void clearCRLs() {
        final String FILE_PREFIX = "crl-";
        ArrayList<String> list = new ArrayList<>();

        for (String file : fileList()) {
            if (file.startsWith(FILE_PREFIX)) {
                list.add(file);
            }
        }
        if (list.size() == 0) {
            Toast.makeText(this, R.string.clear_crl_cache_msg_none, Toast.LENGTH_SHORT).show();
            return;
        }
        removeFragmentByTag(DIALOG_TAG);

        Bundle args = new Bundle();
        args.putStringArrayList(EXTRA_CRL_LIST, list);

        CRLCacheDialog dialog = new CRLCacheDialog();
        dialog.setArguments(args);
        dialog.show(this.getSupportFragmentManager(), DIALOG_TAG);
    }

    /**
     * Class that loads the cached CA certificates.
     */
    private class LoadCertificatesTask extends AsyncTask<Void, Void, TrustedCertificateManager> {
        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected TrustedCertificateManager doInBackground(Void... params) {
            return TrustedCertificateManager.getInstance().load();
        }

        @Override
        protected void onPostExecute(TrustedCertificateManager result) {
            setProgressBarIndeterminateVisibility(false);
        }
    }

    /**
     * Dismiss dialog if shown
     */
    public void removeFragmentByTag(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment login = fm.findFragmentByTag(tag);
        if (login != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(login);
            ft.commit();
        }
    }

    /**
     * Class that displays a confirmation dialog if a VPN profile is already connected
     * and then initiates the selected VPN profile if the user confirms the dialog.
     */
    public static class ConfirmationDialog extends AppCompatDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle profileInfo = getArguments();
            int icon = android.R.drawable.ic_dialog_alert;
            int title = R.string.connect_profile_question;
            int message = R.string.replaces_active_connection;
            int button = R.string.connect;

            if (profileInfo.getBoolean(PROFILE_RECONNECT)) {
                icon = android.R.drawable.ic_dialog_info;
                title = R.string.vpn_connected;
                message = R.string.vpn_profile_connected;
                button = R.string.reconnect;
            } else if (profileInfo.getBoolean(PROFILE_DISCONNECT)) {
                title = R.string.disconnect_question;
                message = R.string.disconnect_active_connection;
                button = R.string.disconnect;
            }

            DialogInterface.OnClickListener connectListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.startVpnProfile(profileInfo);
                }
            };
            DialogInterface.OnClickListener disconnectListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity.mService != null) {
                        activity.mService.disconnect();
                    }
                }
            };
            DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                    if (!profileInfo.getBoolean(PROFILE_FOREGROUND)) {	/* if the app was not in the foreground before this action was triggered
						 * externally, we just close the activity if canceled */
                        getActivity().finish();
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setIcon(icon)
                    .setTitle(String.format(getString(title), profileInfo.getString(PROFILE_NAME)))
                    .setMessage(message);

            if (profileInfo.getBoolean(PROFILE_DISCONNECT)) {
                builder.setPositiveButton(button, disconnectListener);
            } else {
                builder.setPositiveButton(button, connectListener);
            }

            if (profileInfo.getBoolean(PROFILE_RECONNECT)) {
                builder.setNegativeButton(R.string.disconnect, disconnectListener);
                builder.setNeutralButton(android.R.string.cancel, cancelListener);
            } else {
                builder.setNegativeButton(android.R.string.cancel, cancelListener);
            }
            return builder.create();
        }
    }

    /**
     * Class that displays a login dialog and initiates the selected VPN
     * profile if the user confirms the dialog.
     */
    public static class LoginDialog extends AppCompatDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle profileInfo = getArguments();
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.login_dialog, null);
            EditText username = (EditText) view.findViewById(R.id.username);
            username.setText(profileInfo.getString(VpnProfileDataSource.KEY_USERNAME));
            final EditText password = (EditText) view.findViewById(R.id.password);

            AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
            adb.setView(view);
            adb.setTitle(getString(R.string.login_title));
            adb.setPositiveButton(R.string.login_confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    MainActivity activity = (MainActivity) getActivity();
                    profileInfo.putString(VpnProfileDataSource.KEY_PASSWORD, password.getText().toString().trim());
                    activity.prepareVpnService(profileInfo);
                }
            });
            adb.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
            return adb.create();
        }
    }

    /**
     * Class representing an error message which is displayed if VpnService is
     * not supported on the current device.
     */
    public static class VpnNotSupportedError extends AppCompatDialogFragment {
        static final String ERROR_MESSAGE_ID = "org.strongswan.android.VpnNotSupportedError.MessageId";

        public static void showWithMessage(AppCompatActivity activity, int messageId) {
            Bundle bundle = new Bundle();
            bundle.putInt(ERROR_MESSAGE_ID, messageId);
            VpnNotSupportedError dialog = new VpnNotSupportedError();
            dialog.setArguments(bundle);
            dialog.show(activity.getSupportFragmentManager(), DIALOG_TAG);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle arguments = getArguments();
            final int messageId = arguments.getInt(ERROR_MESSAGE_ID);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.vpn_not_supported_title)
                    .setMessage(messageId)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    }).create();
        }
    }

    /**
     * Confirmation dialog to clear CRL cache
     */
    public static class CRLCacheDialog extends AppCompatDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final List<String> list = getArguments().getStringArrayList(EXTRA_CRL_LIST);
            String size;
            long s = 0;

            for (String file : list) {
                File crl = getActivity().getFileStreamPath(file);
                s += crl.length();
            }
            size = Formatter.formatFileSize(getActivity(), s);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.clear_crl_cache_title)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
                    .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            for (String file : list) {
                                getActivity().deleteFile(file);
                            }
                        }
                    });
            builder.setMessage(getActivity().getResources().getQuantityString(R.plurals.clear_crl_cache_msg, list.size(), list.size(), size));
            return builder.create();
        }
    }
}
