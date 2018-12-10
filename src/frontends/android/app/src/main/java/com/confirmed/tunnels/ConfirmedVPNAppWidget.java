package com.confirmed.tunnels;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.net.VpnService;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.content.Intent;
import android.app.PendingIntent;
import android.util.Log;

import com.trytunnels.android.ui.MainActivity;
import com.trytunnels.android.ui.WelcomeActivity;
import com.trytunnels.android.utils.SpeedTestService;

import org.strongswan.android.R;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.CharonVpnService;
import org.strongswan.android.logic.StrongSwanApplication;

import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class ConfirmedVPNAppWidget extends AppWidgetProvider {

    private static final String powerClicked = "widget_toggle_power";
    private static final String speedTestClicked = "widget_speed_test";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            // Create an Intent to launch ExampleActivity
            //Intent intent = new Intent(context, MainActivity.class);
            Intent toggleIntent = new Intent(context, getClass());
            toggleIntent.setAction(powerClicked);
            //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            PendingIntent togglePendingIntent = PendingIntent.getBroadcast(context, 0, toggleIntent,0);

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.confirmed_vpnapp_widget);
            remoteViews.setOnClickPendingIntent(R.id.btn_widget_power, togglePendingIntent);

            Intent speedTestIntent = new Intent(context, getClass());
            speedTestIntent.setAction(speedTestClicked);
            PendingIntent speedTestPendingIntent = PendingIntent.getBroadcast(context, 0, speedTestIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.txt_widget_speed_test, speedTestPendingIntent);

            //updateAppWidget(context, appWidgetManager, appWidgetId);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);
        String action = intent.getAction();
        Log.d("ConfirmedWidget", "This is test. "
                + action);
        if(action.equals(powerClicked))
        {

            VpnProfileDataSource mVpnProfileDataSource = new VpnProfileDataSource(context.getApplicationContext());

            mVpnProfileDataSource.open();
            List<VpnProfile> existingVpnProfiles = mVpnProfileDataSource.getAllVpnProfiles();
            mVpnProfileDataSource.close();

            if(existingVpnProfiles.size() > 0) {
                VpnProfile profile = existingVpnProfiles.get(0);

                Bundle profileInfo = new Bundle();
                profileInfo.putLong(VpnProfileDataSource.KEY_ID, profile.getId());
                profileInfo.putString(VpnProfileDataSource.KEY_USERNAME, profile.getUsername());
                profileInfo.putString(VpnProfileDataSource.KEY_PASSWORD, profile.getPassword());
                profileInfo.putBoolean("org.strongswan.android.MainActivity.REQUIRES_PASSWORD", profile.getVpnType().has(VpnType.VpnTypeFeature.USER_PASS));
                profileInfo.putString("org.strongswan.android.MainActivity.PROFILE_NAME", profile.getName());
                profileInfo.putBoolean("VPN_IS_TOGGLE", true);

                try {
                    Context appContext = context.getApplicationContext();

                    Intent prepIntent = VpnService.prepare(appContext);

                    Intent vpnIntent = new Intent(context.getApplicationContext(), CharonVpnService.class);
                    vpnIntent.putExtras(profileInfo);
                    context.startService(vpnIntent);
                } catch (IllegalStateException ex) {

                }
            }
            // else just launch app
            else
            {
                Intent startIntent = new Intent(context, MainActivity.class);
                context.startActivity(startIntent);
            }

            //Intent startIntent = new Intent(context, MainActivity.class);
            //startIntent.putExtra("JUST_TOGGLE", true);
            //context.startActivity(startIntent);

            /*RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.confirmed_vpnapp_widget);
            remoteViews.setImageViewResource(R.id.btn_widget_power, R.drawable.tt_power_button_circle_activated);
            //ImageView mBtnVpnPower = view.findViewById(R.id.btnVpnPower);

            ComponentName watchWidget = new ComponentName(context, ConfirmedVPNAppWidget.class);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(watchWidget, remoteViews);*/
        }
        else if(action.equals(speedTestClicked))
        {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.confirmed_vpnapp_widget);
            remoteViews.setTextViewText(R.id.txt_widget_speed_test, "Speed: ...");
            ComponentName confirmedWidget = new ComponentName(context, ConfirmedVPNAppWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(confirmedWidget, remoteViews);

            Intent speedTestIntent = new Intent(context.getApplicationContext(), SpeedTestService.class);
            speedTestIntent.putExtra(SpeedTestService.forWidgetKey, true);
            context.startService(speedTestIntent);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        /*Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("Installed widget")
                .putContentType("widget")
                .putContentId("install-widget"));*/
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

