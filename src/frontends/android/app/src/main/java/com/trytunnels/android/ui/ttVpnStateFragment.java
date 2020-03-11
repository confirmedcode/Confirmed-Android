/*
 * Copyright (C) 2012-2016 Tobias Brunner
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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.security.KeyChain;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.confirmed.tunnels.ConfirmedVPNAppWidget;
import com.trytunnels.android.utils.ApiUtil;
import com.trytunnels.android.utils.ConfirmedConstants;
import com.trytunnels.android.utils.PreferenceStoreUtil;

import org.json.JSONObject;
import org.strongswan.android.R;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.logic.CharonVpnService;
import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.logic.VpnStateService.ErrorState;
import org.strongswan.android.logic.VpnStateService.State;
import org.strongswan.android.logic.VpnStateService.VpnStateListener;
import org.strongswan.android.logic.imc.ImcState;
import org.strongswan.android.logic.imc.RemediationInstruction;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ttVpnStateFragment extends Fragment implements VpnStateListener {
    private static final String KEY_ERROR_CONNECTION_ID = "error_connection_id";
    private static final String KEY_DISMISSED_CONNECTION_ID = "dismissed_connection_id";

    private static final int EXPLANATION_ACTIVITY = 27;

    private static final String TAG = "ttVpnStateFragment";

    public ImageView mBtnVpnPower;
    private ProgressBar mProgressBarVpnPower;
    private TextView mTvVpnState;
    private OnVpnProfileSelectedListener mListener;
    private ApiUtil mApiUtil;
    private Handler mHandler;
    private boolean mReconnect;
    private Activity mActivity;
    private Button mBtnExplanation;
    private Context mContext;

    private AlertDialog mErrorDialog;
    private long mErrorConnectionID;
    private long mDismissedConnectionID;
    private VpnStateService mService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((VpnStateService.LocalBinder) service).getService();
            mService.registerListener(ttVpnStateFragment.this);
            updateView();
        }
    };

    /**
     * The activity containing this fragment should implement this interface
     */
    public interface OnVpnProfileSelectedListener {
        void onVpnProfileSelected(VpnProfile profile);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		/* bind to the service only seems to work from the ApplicationContext */
        mContext = getActivity().getApplicationContext();
        mContext.bindService(new Intent(mContext, VpnStateService.class),
                mServiceConnection, Service.BIND_AUTO_CREATE);


        mHandler = new Handler(Looper.getMainLooper());
        mApiUtil = ApiUtil.getInstance(); // fix

        mErrorConnectionID = 0;
        mDismissedConnectionID = 0;
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_ERROR_CONNECTION_ID)) {
            mErrorConnectionID = (Long) savedInstanceState.getSerializable(KEY_ERROR_CONNECTION_ID);
            mDismissedConnectionID = (Long) savedInstanceState.getSerializable(KEY_DISMISSED_CONNECTION_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_ERROR_CONNECTION_ID, mErrorConnectionID);
        outState.putSerializable(KEY_DISMISSED_CONNECTION_ID, mDismissedConnectionID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tt_vpn_state_fragment, null);

        mBtnVpnPower = view.findViewById(R.id.btnVpnPower);
        mBtnVpnPower.setOnClickListener(new ttOnVpnPowerClick());
        mProgressBarVpnPower = view.findViewById(R.id.progressBarVpnPower);
        mTvVpnState = view.findViewById(R.id.tvVpnState);

        mBtnExplanation = view.findViewById(R.id.btn_vpn_explain);
        mBtnExplanation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchExplanation();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mService != null) {
            mService.registerListener(this);

            State state = mService.getState();

            // if not waiting on button press
            if(mBtnVpnPower.isClickable() || state == State.CONNECTED) {
                updateView();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mService != null) {
            mService.unregisterListener(this);
        }
        hideErrorDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            getActivity().getApplicationContext().unbindService(mServiceConnection);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof Activity) {
            mActivity = (Activity) context;
        }

        if (context instanceof OnVpnProfileSelectedListener) {
            mListener = (OnVpnProfileSelectedListener) context;
        }
    }

    @Override
    public void stateChanged() {
        updateView();
    }

    private void launchExplanation()
    {
        Intent vpnSettingsIntent = new Intent(mContext, ExplanationActivity.class);
        vpnSettingsIntent.putExtra("IS_VPN_ON", mService.getState() == State.CONNECTED);
        startActivityForResult(vpnSettingsIntent, EXPLANATION_ACTIVITY);
    }

    public void updateView() {
        long connectionID = mService.getConnectionID();
        VpnProfile profile = mService.getProfile();
        State state = mService.getState();
        ErrorState error = mService.getErrorState();
        ImcState imcState = mService.getImcState();
        String name = "";

        if (getActivity() == null) {
            return;
        }

        if (profile != null) {
            name = profile.getName();
        }

        if (reportError(connectionID, name, error, imcState)) {
            setPowerButtonObjects();
            return;
        }

        switch (state) {
            case CONNECTED:
                mTvVpnState.setText(R.string.tt_vpn_state_textview_connected);
                //toggleWidgetPower(true);
                enableVpnPower();
                mBtnVpnPower.setActivated(true);
                break;
            case DISABLED:
                mTvVpnState.setText(R.string.tt_vpn_state_textview_disconnected);
                //toggleWidgetPower(false);
                enableVpnPower();
                mBtnVpnPower.setActivated(false);
                if (mReconnect) {
                    mReconnect = false;
                    mBtnVpnPower.callOnClick();
                }
                break;
            case CONNECTING:
                mTvVpnState.setText(R.string.tt_vpn_state_textview_connecting);
                mBtnVpnPower.setActivated(false);
                //toggleWidgetPower(false);
                break;
            case DISCONNECTING:
                mTvVpnState.setText(R.string.tt_vpn_state_textview_disconnecting);
                mBtnVpnPower.setActivated(false);
                //toggleWidgetPower(false);
                break;
        }
    }

    private void toggleWidgetPower(boolean active)
    {
        Context context = getContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.confirmed_vpnapp_widget);
        ComponentName thisWidget = new ComponentName(context, ConfirmedVPNAppWidget.class);
        if(active)
        {
            remoteViews.setImageViewResource(R.id.btn_widget_power, R.drawable.tt_power_button_circle_activated);
            remoteViews.setTextViewText(R.id.txt_widget_connect_state, getString(R.string.tt_vpn_state_textview_connected));
        }
        else
        {
            remoteViews.setImageViewResource(R.id.btn_widget_power, R.drawable.tt_power_button_circle);
            remoteViews.setTextViewText(R.id.txt_widget_connect_state, getString(R.string.tt_vpn_state_textview_disconnected));
        }
        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
    }

    private boolean reportError(long connectionID, String name, ErrorState error, ImcState imcState) {
        if (connectionID > mDismissedConnectionID) {	/* report error if it hasn't been dismissed yet */
            mErrorConnectionID = connectionID;
        } else {	/* ignore all other errors */
            error = ErrorState.NO_ERROR;
        }
        if (error == ErrorState.NO_ERROR) {
            hideErrorDialog();
            return false;
        } else if (mErrorDialog != null) {	/* we already show the dialog */
            return true;
        }

        //showProfile(true);

        switch (error) {
            case AUTH_FAILED:
                if (imcState == ImcState.BLOCK) {
                    showErrorDialog(R.string.error_assessment_failed);
                } else {
                    showErrorDialog(R.string.error_auth_failed);
                }
                break;
            case PEER_AUTH_FAILED:
                showErrorDialog(R.string.error_peer_auth_failed);
                break;
            case LOOKUP_FAILED:
                showErrorDialog(R.string.error_lookup_failed);
                break;
            case UNREACHABLE:
                showErrorDialog(R.string.error_unreachable);
                break;
            default:
                showErrorDialog(R.string.error_generic);
                break;
        }
        return true;
    }

    public void setReconnect(boolean reconnect) {
        mReconnect = reconnect;
    }

    public void triggerVpnPowerClick() {
        mBtnVpnPower.callOnClick();
    }

    protected void enableVpnPower() {
        if(!mBtnVpnPower.isClickable()) {
            mBtnVpnPower.setClickable(true);
            //mProgressBarVpnPower.setVisibility(View.GONE);
            stopCircleProgressAnimation();
        }
        // just make sure objects on power button are set correctly
        else
        {
            setPowerButtonObjects();
        }
    }

    public void setPowerButtonObjects()
    {
        Context context = mContext;//getActivity().getApplicationContext();

        if (context == null)
        {
            return;
        }

        //InsetDrawable insetDrawable = (InsetDrawable)((LayerDrawable)(mBtnVpnPower.getDrawable())).findDrawableByLayerId(R.id.animated_circle);

        // transition center
        InsetDrawable powerButton  = ((InsetDrawable)((LayerDrawable)(mBtnVpnPower.getDrawable())).findDrawableByLayerId(R.id.power_center));

        if(mService != null && mService.getState() == State.CONNECTED) {
            powerButton.setAlpha(255);

            Drawable animatedCircle = ContextCompat.getDrawable(context, R.drawable.tt_animated_circular_progress_activated);
            InsetDrawable newInset = new InsetDrawable(animatedCircle, 0);
            ((LayerDrawable)(mBtnVpnPower.getDrawable())).setDrawableByLayerId(R.id.animated_circle, newInset);
        }
        else
        {
            powerButton.setAlpha(0);

            Drawable animatedCircle = ContextCompat.getDrawable(context, R.drawable.tt_animated_circular_progress);
            InsetDrawable newInset = new InsetDrawable(animatedCircle, 0);
            ((LayerDrawable)(mBtnVpnPower.getDrawable())).setDrawableByLayerId(R.id.animated_circle, newInset);
        }
    }

    private void startCircleProgressAnimation()
    {
        Context context = mContext; //getActivity().getApplicationContext();

        if(context == null)
        {
            return;
        }

        if(mService != null && mService.getState() == State.CONNECTED)
        {
            //InsetDrawable insetDrawable = (InsetDrawable)((LayerDrawable)(mBtnVpnPower.getDrawable())).findDrawableByLayerId(R.id.animated_circle);
            /*insetDrawable.setDrawable(context.getDrawable(R.drawable.tt_animated_circular_progress)); // switch to setdrawable on layer
            ((Animatable)insetDrawable.getDrawable()).start();*/
            InsetDrawable powerButton  = ((InsetDrawable)((LayerDrawable)(mBtnVpnPower.getDrawable())).findDrawableByLayerId(R.id.power_center));

            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(powerButton, PropertyValuesHolder.ofInt("alpha", 255, 0));
            animator.setTarget(powerButton);
            animator.setDuration(1000);
            /*animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    setPowerButtonObjects();
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    setPowerButtonObjects();
                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });*/
            animator.start();

            Drawable animatedCircle = ContextCompat.getDrawable(context, R.drawable.tt_animated_circular_progress_end);
            InsetDrawable newInset = new InsetDrawable(animatedCircle, 0);
            ((LayerDrawable)(mBtnVpnPower.getDrawable())).setDrawableByLayerId(R.id.animated_circle, newInset);

            ((Animatable) animatedCircle).start();
        }
        else {
            Drawable animatedCircle = ContextCompat.getDrawable(context, R.drawable.tt_animated_circular_progress_activated);
            InsetDrawable newInset = new InsetDrawable(animatedCircle, 0);
            ((LayerDrawable)(mBtnVpnPower.getDrawable())).setDrawableByLayerId(R.id.animated_circle, newInset);
            ((Animatable) animatedCircle).start();
        }
    }

    private void stopCircleProgressAnimation()
    {
        Context context = mContext; //getActivity().getApplicationContext();

        if(context == null)
        {
            return;
        }

        // transition center
        InsetDrawable powerButton  = ((InsetDrawable)((LayerDrawable)(mBtnVpnPower.getDrawable())).findDrawableByLayerId(R.id.power_center));

        if(mService != null && mService.getState() == State.CONNECTED) {
            //InsetDrawable insetDrawable = (InsetDrawable)((LayerDrawable)(mBtnVpnPower.getDrawable())).findDrawableByLayerId(R.id.animated_circle);
            //((Animatable) insetDrawable.getDrawable()).stop();

            //transitionDrawable.setCrossFadeEnabled(true);
            //transitionDrawable.startTransition(1000);
            //transitionDrawable.setAlpha(50);
            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(powerButton, PropertyValuesHolder.ofInt("alpha", 0, 255));
            animator.setTarget(powerButton);
            animator.setDuration(1000);
            /*animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    setPowerButtonObjects();
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    setPowerButtonObjects();
                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });*/
            animator.start();

            Drawable animatedCircle = ContextCompat.getDrawable(context, R.drawable.tt_animated_circular_progress_end_activated);
            InsetDrawable newInset = new InsetDrawable(animatedCircle, 0);
            ((LayerDrawable)(mBtnVpnPower.getDrawable())).setDrawableByLayerId(R.id.animated_circle, newInset);
            ((Animatable) animatedCircle).start();
        }
        else
        {
            //transitionDrawable.setCrossFadeEnabled(true);
            //transitionDrawable.reverseTransition(1000);
            //transitionDrawable.setAlpha(100);

            /*ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(powerButton, PropertyValuesHolder.ofInt("alpha", 255, 0));
            animator.setTarget(powerButton);
            animator.setDuration(1000);
            animator.start();

            insetDrawable.setDrawable(context.getDrawable(R.drawable.tt_animated_circular_progress_end));
            ((Animatable) insetDrawable.getDrawable()).start();*/
        }
    }

    protected class ttOnVpnPowerClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // disable button to prevent more clicks
            mBtnVpnPower.setClickable(false);
            // show that progress bar
            //mProgressBarVpnPower.setVisibility(View.VISIBLE);
            startCircleProgressAnimation();

            boolean hadCorrectStart = false;
            hadCorrectStart = PreferenceStoreUtil.getInstance().getBoolean(getString(R.string.tt_correct_start_key), false);

            VpnProfile profile = null;

            if(hadCorrectStart)
            {
                profile = ((MainActivity) getActivity()).ttGetVpnProfile();
            }

            if (profile == null) {
                //setPowerButtonObjects();
                ttGetSswanFromServer();
                return;
            }

            if (mService != null && (mService.getState() == State.CONNECTED || mService.getState() == State.CONNECTING)) {
                mService.disconnect();
                return;
            }

            if (mListener != null) {
                mListener.onVpnProfileSelected(profile);
            }
        }
    }

    /**
     * get swan file from server
     */
    protected void ttGetSswanFromServer() {
        final String subInfoKey = getString(R.string.tt_paid_sub_info_key);
        String receipt = PreferenceStoreUtil.getInstance().getString(subInfoKey, "");

        if(!receipt.equals("")) {
            ApiUtil.getInstance().getKey(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "ttGetSswanFromServer Failure: " + e.getMessage());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            enableVpnPower();
                            if(mActivity != null) {
                                Toast.makeText(mActivity, "Error connecting", Toast.LENGTH_SHORT).show();
                            }
                            setPowerButtonObjects();
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
                        ttProcessSswanResponse(responseString);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to process Sswan response: " + e.getMessage());
                        onFailure(call, new IOException("bad sswan i"));
                        return;
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            enableVpnPower();
                            //Toast.makeText(MainActivity.this, "ttGetSswanFromServer Success", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
        else
        {
                String email = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_email_store_key), "");
                String password = PreferenceStoreUtil.getInstance().getString(getString(R.string.tt_password_store_key), "");

                if(!email.equals("") && !password.equals("")) {
                    //ApiUtil authClient = new ApiUtil(email, password);
                    ApiUtil.getInstance().getKey(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "ttGetSswanFromServer Failure: " + e.getMessage());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    enableVpnPower();
                                    if(mActivity != null) {
                                        Toast.makeText(mActivity, "Error connecting", Toast.LENGTH_SHORT).show();
                                    }
                                    setPowerButtonObjects();
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
                                ttProcessSswanResponse(responseString);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to process Sswan response: " + e.getMessage());
                                onFailure(call, new IOException("bad sswan i"));
                                return;
                            }
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    enableVpnPower();
                                    //Toast.makeText(MainActivity.this, "ttGetSswanFromServer Success", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
                else
                {
                    setPowerButtonObjects();
                    enableVpnPower();
                }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EXPLANATION_ACTIVITY:
                if (resultCode == 1) {
                    triggerVpnPowerClick();
                }
                break;
            default:
        }
    }

    /**
     * Process the response from server for createUserWithReceipt
     * Which returns a b64 encoded sswanfile
     *
     * @param responseString
     */
    protected void ttProcessSswanResponse(String responseString) throws Exception {
        // Response node is JSON Object
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

//        if(ConfirmedConstants.getVersionNumber() == 1)
//        {
//            String decodedB64 = new String(Base64.decode(b64.getBytes(), Base64.DEFAULT));
//            JSONObject certJson = new JSONObject(decodedB64);
//            Log.d(TAG, "certJson: " + certJson.toString());
//            ((MainActivity) getActivity()).ttCreateVpnProfile(certJson);
//        }
//        else
//        {
            String id = jsonObj.getString("id");

            if (b64.equals("")) {
                throw new Exception("Invalid json");
            }

            Log.d(TAG, "v2 cert");
            ((MainActivity) getActivity()).ttCreateVpnProfile(id, b64);
//        }
    }

    private void hideErrorDialog() {
        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }
    }

    private void clearError() {
        if (mService != null) {
            mService.disconnect();
        }
        mDismissedConnectionID = mErrorConnectionID;
        updateView();
    }

    private void showErrorDialog(int textid) {
        final List<RemediationInstruction> instructions = mService.getRemediationInstructions();
        final boolean show_instructions = mService.getImcState() == ImcState.BLOCK && !instructions.isEmpty();
        int text = show_instructions ? R.string.show_remediation_instructions : R.string.show_log;

        mErrorDialog = new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.error_introduction) + " " + getString(textid))
                .setCancelable(false)
/*
            .setNeutralButton(text, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					clearError();
					dialog.dismiss();
					Intent intent;
					if (show_instructions)
					{
						intent = new Intent(getActivity(), RemediationInstructionsActivity.class);
						intent.putParcelableArrayListExtra(RemediationInstructionsFragment.EXTRA_REMEDIATION_INSTRUCTIONS,
														   new ArrayList<RemediationInstruction>(instructions));
					}
					else
					{
						intent = new Intent(getActivity(), LogActivity.class);
					}
					startActivity(intent);
				}
			})
*/
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        clearError();
                        dialog.dismiss();
                    }
                }).create();
        mErrorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mErrorDialog = null;
            }
        });
        mErrorDialog.show();
    }
}
