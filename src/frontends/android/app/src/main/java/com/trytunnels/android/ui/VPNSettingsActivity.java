package com.trytunnels.android.ui;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trytunnels.android.utils.ApiUtil;
import com.trytunnels.android.utils.PreferenceStoreUtil;

import org.json.JSONArray;
import org.strongswan.android.R;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class VPNSettingsActivity extends AppCompatActivity {

    private static final String TAG = "VPNSettingsActivity";

    private ListView listViewExclude;
    private Button btnApply;

    private Set<String> mExcludedPackages;

    private PackageManager pm;
    List<ApplicationInfo> apps;

    final Handler mHandler = new Handler(Looper.getMainLooper());

    private static final int EMAIL_RESULT = 7;
    private static final int UPGRADE_RESULT = 8;

    boolean wasChanged = false;
    boolean useDefault = false;

    ProgressBar progressBar;

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
        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
        //getSupportActionBar().setTitle(Html.fromHtml("<font color=\"red\">" + "Account" + "</font>"));

        //setTheme(R.style.ttAccountTheme);

        setContentView(R.layout.tt_vpn_settings);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            useDefault = extras.getBoolean("USE_DEFAULT");
        }

        listViewExclude = findViewById(R.id.listv_vpn_settings_vpn_exclude);
        btnApply = findViewById(R.id.btn_vpn_settings_apply);
        final ProgressBar progressBar = findViewById(R.id.progressbar_vpn_settings);

        btnApply.setEnabled(false);

        mExcludedPackages = PreferenceStoreUtil.getInstance().getStringSet(getString(R.string.tt_exclude_list), null);
        if(mExcludedPackages == null)
        {
            mExcludedPackages = new HashSet<String>();

            if(useDefault)
            {
                mExcludedPackages.add("com.netflix.mediaclient");
                mExcludedPackages.add("com.hulu.plus");
                mExcludedPackages.add("com.twitter.android");
                mExcludedPackages.add("com.skype.raider");
                wasChanged = true;
            }
        }

        //final PackageManager pm = getApplicationContext().getPackageManager();
        pm = getApplicationContext().getPackageManager();
        apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        /*final ArrayList<String> testList = new ArrayList<String>();

        for(int i=0;i<apps.size();i++)
        {
            ApplicationInfo curApp = apps.get(i);
            if((curApp.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                CharSequence descChar = curApp.loadDescription(pm);
                String description = "";
                if(descChar != null) {
                    description = descChar.toString();
                }
                CharSequence labelChar = curApp.loadLabel(pm);
                String label = "";
                if(labelChar != null) {
                    label = labelChar.toString();
                }

                testList.add(label);
            }
        }*/

        // making notification bar transparent
        changeStatusBarColor();

        final ArrayList<ApplicationDisplayInfo> appDisplayInfo = new ArrayList<ApplicationDisplayInfo>();

        Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i=0;i<apps.size();i++)
                    {
                        ApplicationInfo curApp = apps.get(i);
                        if((curApp.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !curApp.packageName.equals("com.confirmed.tunnels"))
                        {
                            CharSequence labelChar = curApp.loadLabel(pm);
                            String label = "";
                            if(labelChar != null) {
                                label = labelChar.toString();
                            }

                            appDisplayInfo.add(new ApplicationDisplayInfo(label, curApp.packageName, curApp)); //curApp.loadIcon(pm)));
                        }
                    }

                    // sort so selected items go first
                    Collections.sort(appDisplayInfo, new Comparator<ApplicationDisplayInfo>() {
                        @Override
                        public int compare(ApplicationDisplayInfo i1, ApplicationDisplayInfo i2) {
                            boolean i1Ex = mExcludedPackages.contains(i1.packageName);
                            boolean i2Ex = mExcludedPackages.contains(i2.packageName);

                            if(i1Ex == i2Ex)
                            {
                                return i1.name.compareToIgnoreCase(i2.name);
                            }
                            else
                            {
                                if(i1Ex)
                                    return -1;
                                else
                                    return 1;
                            }
                        }
                    });

                    final ApplicationAdapter adapter = new ApplicationAdapter(getApplicationContext(), appDisplayInfo);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listViewExclude.setAdapter(adapter);
                            progressBar.setVisibility(View.GONE);
                        }
                    });

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            btnApply.setEnabled(true);
                        }
                    });
                }
            });
            thread.start();

        //ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, testList);



        //listViewExclude.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        listViewExclude.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ApplicationDisplayInfo curApp = appDisplayInfo.get(i);

                listViewExclude.setItemChecked(i, true);
                Log.d(TAG, curApp.packageName);
            }
        });

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(wasChanged) {
                    cleanUpList(appDisplayInfo);
                    PreferenceStoreUtil.getInstance().putStringSet(getString(R.string.tt_exclude_list), mExcludedPackages);
                    setResult(1);
                }
                else
                {
                    setResult(0);
                }
                finish();
            }
        });
    }

    // take out any packages that don't match an installed program
    private void cleanUpList(ArrayList<ApplicationDisplayInfo> appDisplayInfo)
    {
        ArrayList<String> toRemove = new ArrayList<String>();
        for(String s : mExcludedPackages)
        {
            boolean remove = true;
            for(ApplicationDisplayInfo a : appDisplayInfo)
            {
                if(a.packageName.equals(s))
                {
                    remove = false;
                    break;
                }
            }

            if(remove)
            {
                toRemove.add(s);
            }
        }

        if(toRemove.size() > 0)
        {
            for(String s : toRemove)
            {
                mExcludedPackages.remove(s);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class ApplicationDisplayInfo
    {
        public String name;
        public String packageName;
        //public Drawable icon;
        public ApplicationInfo appInfo;

        ApplicationDisplayInfo(String name, String packageName, ApplicationInfo appInfo) //Drawable icon)
        {
            this.name = name;
            this.packageName = packageName;
            //this.icon = icon;
            this.appInfo = appInfo;
        }
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

    private class ApplicationAdapter extends BaseAdapter
    {
        private Context mContext;
        private LayoutInflater mInflater;
        private List<ApplicationDisplayInfo> applications;
        private boolean gotData = false;

        public ApplicationAdapter(Context context, List<ApplicationDisplayInfo> appInfoList)
        {
            mContext = context;
            applications = appInfoList;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount()
        {
            return applications.size();
            //return apps.size();
        }

        @Override
        public Object getItem(int position)
        {
            return applications.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View rowView = null;

            if(convertView == null) {
                rowView = mInflater.inflate(R.layout.list_item_application, parent, false);
            }
            else
            {
                rowView = convertView;
            }

            TextView appName = (TextView) rowView.findViewById(R.id.txt_app_name);
            ImageView iconView = (ImageView) rowView.findViewById(R.id.img_application_icon);
            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.chk_application_list);
            checkBox.setOnCheckedChangeListener(null);

            final ApplicationDisplayInfo curApp = (ApplicationDisplayInfo) getItem(position);

            appName.setText(curApp.name);
            // font family in listview xml not honored for some reason
            appName.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.lato_light));
            iconView.setImageDrawable(curApp.appInfo.loadIcon(pm));
            checkBox.setChecked(mExcludedPackages.contains(curApp.packageName));

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    wasChanged = true;
                    if (b) {
                        mExcludedPackages.add(curApp.packageName);
                    } else {
                        mExcludedPackages.remove(curApp.packageName);
                    }
                }
            });

            return rowView;
        }
    }
}