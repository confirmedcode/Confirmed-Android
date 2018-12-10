package com.trytunnels.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.trytunnels.android.utils.purchaseUtils.IabHelper;
import com.trytunnels.android.utils.purchaseUtils.IabResult;
import com.trytunnels.android.utils.purchaseUtils.Inventory;
import com.trytunnels.android.utils.purchaseUtils.Purchase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.strongswan.android.R;

import static com.trytunnels.android.utils.purchaseUtils.IabHelper.RESPONSE_CODE;
import static com.trytunnels.android.utils.purchaseUtils.IabHelper.RESPONSE_INAPP_PURCHASE_DATA;
import static com.trytunnels.android.utils.purchaseUtils.IabHelper.RESPONSE_INAPP_SIGNATURE;

public class SubscriptionUtil {
    private static final int REQUEST_CODE = 10001;
    private static final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoYuboyqMOwlChZhmLhm791I8eaW/82gwBNJQ3BujaWykydwlc/cRWAMXY9dLJfNLo8TufyvMlBzWN2haNsbcUvaOtj7xtGil57EY+L0KsBohvv4uY6MyDkud3hSlbOLofrzWqOnP+Vx3UribDZfpDb5XKe+VWtBnUrR+QpDtlLjsYY0PuIchr5vj67rGbp6sYzqN6Ng++7cBk5paZ3NIft7YnmuXIGy5FsH2W+8ZT2S1rp5ZoEQP1qYSJV3bvfmkkD2g0mazuwIh5ejxvfbnf4MLtuU9ND14vM98KAw/M5O6YQOkmqNuC5bWN86urdhE1sLiMTsw8ilka670VqiBbQIDAQAB";

    private String mPaidSubKey = "androidtunnels";
    private String mPaidSubInfoKey = "paid_sub_info";

    public static final String mCurrProductIdKey = "currentProductId";

    private Boolean mHasSubscription;
    private String mSubscriptionInfo;
    private IabHelper mIabHelper;
    private Context mContext;

    private static final String TAG = "SubscriptionUtil";

    private SubscriptionUtil() {
        //No instance
    }

    public SubscriptionUtil(Context context) {
        initCommon(context);
        setup();
    }

    public SubscriptionUtil(Context context, final SubscriptionSetupListener subscriptionSetupListener) {
        initCommon(context);
        this.mContext = context;
        setup(subscriptionSetupListener);
    }

    private void initCommon(Context context) {
        this.mContext = context;
        mIabHelper = new IabHelper(context, base64EncodedPublicKey);
        mIabHelper.enableDebugLogging(true, "IabHelper");
        mPaidSubKey = mContext.getString(R.string.tt_paid_sub_key);
        mPaidSubInfoKey = mContext.getString(R.string.tt_paid_sub_info_key);
    }

    private void setup() {
        if (mIabHelper != null) {
            mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (result.isFailure() || !result.isSuccess()) {
                        Log.d(TAG, "Problem setting up In-app Billing: " + result);
                        dispose();
                    }
                }
            });
        }
    }

    private void setup(final SubscriptionSetupListener subscriptionSetupListener) {
        if (mIabHelper != null) {
            mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (result.isFailure() || !result.isSuccess()) {
                        Log.d(TAG, "Problem setting up In-app Billing: " + result);
                        dispose();
                    } else {
                        subscriptionSetupListener.onSuccess();
                    }
                }
            });
        }
    }

    private void updatePrefStore(boolean hasSub, Purchase purchase) {
        if (!hasSub || purchase == null) {
            PreferenceStoreUtil.getInstance().putBoolean(mPaidSubKey, false);
            PreferenceStoreUtil.getInstance().remove(mPaidSubInfoKey);
            return;
        }

        JSONObject subInfo = new JSONObject();

        try {
            JSONObject origJson = new JSONObject(purchase.getOriginalJson());
            subInfo.put(RESPONSE_INAPP_PURCHASE_DATA, origJson);
            subInfo.put(RESPONSE_INAPP_SIGNATURE, purchase.getSignature());
            subInfo.put(RESPONSE_CODE, IabHelper.BILLING_RESPONSE_RESULT_OK	);
            PreferenceStoreUtil.getInstance().putString(mCurrProductIdKey, origJson.getString("productId"));
        } catch (JSONException e) {
            Log.e(TAG, "Error purchasing: " + e.getMessage());
            PreferenceStoreUtil.getInstance().putBoolean(mPaidSubKey, false);
            PreferenceStoreUtil.getInstance().remove(mPaidSubInfoKey);
            return;
        }

        Log.d(TAG, "subInfo: " + subInfo.toString());
        String b64 = new String(Base64.encode(subInfo.toString().getBytes(), Base64.DEFAULT));
        PreferenceStoreUtil.getInstance().putString(mPaidSubInfoKey, b64);
        PreferenceStoreUtil.getInstance().putBoolean(mPaidSubKey, true);
    }

    public void initSubscription(final String subscriptionType,
                                 SubscriptionFinishedListener subscriptionFinishedListener) {
        initSubscriptionWithExtras(subscriptionType, subscriptionFinishedListener, "");
    }

    public void initSubscriptionWithExtras(final String subscriptionType,
                                           final SubscriptionFinishedListener subscriptionFinishedListener,
                                           String payload) {
        if (mIabHelper != null) {
            try {
                mIabHelper.launchSubscriptionPurchaseFlow((Activity) mContext,
                        subscriptionType,
                        REQUEST_CODE,
                        new IabHelper.OnIabPurchaseFinishedListener() {
                            @Override
                            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                                if (result.isFailure()) {
                                    Log.e(TAG, "Error purchasing: " + result);
                                    updatePrefStore(false, null);
                                    return;
                                }
                                if (info.getSku().equals(subscriptionType)) {
                                    updatePrefStore(true, info);

                                    if (subscriptionFinishedListener != null) {
                                        subscriptionFinishedListener.onSuccess();
                                    }
                                    Log.e(TAG, "Thank you for upgrading to premium!");
                                }
                            }
                        },
                        payload
                );
            } catch (Exception e) {
                e.printStackTrace();
                updatePrefStore(false, null);
            }
            //In case you get below error:
            //`Can't start async operation (refresh inventory) because another async operation (launchPurchaseFlow) is in progress.`
            //Include this line of code to end proccess after purchase
            //mIabHelper.flagEndAsync();
        }
    }

    public void initSubscriptionUpgradeWithExtras(final String newSubscriptionType,
                                                  final String oldSubscriptionType,
                                                  final SubscriptionFinishedListener subscriptionFinishedListener) {
        initSubscriptionUpgradeWithExtras(newSubscriptionType, oldSubscriptionType, subscriptionFinishedListener, "");
    }

    public void initSubscriptionUpgradeWithExtras(final String newSubscriptionType,
                                           final String oldSubscriptionType,
                                           final SubscriptionFinishedListener subscriptionFinishedListener,
                                           String payload) {
        if (mIabHelper != null) {
            try {
                /*Bundle extraParams = new Bundle();
                ArrayList<String> oldSubs = new ArrayList<String>();
                oldSubs.add(oldSubscriptionType);

                extraParams.putStringArrayList("skusToReplace", oldSubs);
                extraParams.putBoolean("replaceSkusProration", true);*/

                mIabHelper.launchSubscriptionPurchaseFlow((Activity) mContext,
                        newSubscriptionType,
                        REQUEST_CODE,
                        new IabHelper.OnIabPurchaseFinishedListener() {
                            @Override
                            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                                if (result.isFailure()) {
                                    Log.e(TAG, "Error purchasing: " + result);
                                    updatePrefStore(false, null);
                                    return;
                                }
                                if (info.getSku().equals(newSubscriptionType)) {
                                    updatePrefStore(true, info);

                                    if (subscriptionFinishedListener != null) {
                                        subscriptionFinishedListener.onSuccess();
                                    }
                                    Log.e(TAG, "Thank you for upgrading to premium!");
                                }
                            }
                        },
                        payload,
                        oldSubscriptionType
                );
            } catch (Exception e) {
                e.printStackTrace();
                updatePrefStore(false, null);
            }
            //In case you get below error:
            //`Can't start async operation (refresh inventory) because another async operation (launchPurchaseFlow) is in progress.`
            //Include this line of code to end proccess after purchase
            //mIabHelper.flagEndAsync();
        }
    }

    public void hasSubscription(
            final String subscriptionType,
            final SubscriptionInventoryListener subscriptionInventoryListener
    ) {
        Log.d(TAG, "checking if hasSubscription");
        //if (true) {
        //    return;
        //}

        if (mHasSubscription == null && PreferenceStoreUtil.getInstance().contains(mPaidSubKey)) {
            mHasSubscription = PreferenceStoreUtil.getInstance().getBoolean(mPaidSubKey, false);
            mSubscriptionInfo =  PreferenceStoreUtil.getInstance().getString(mPaidSubInfoKey, null);
        }

        // if we have a cached true we'll let it go through without checking for now
        if (mHasSubscription != null && mHasSubscription == true && mSubscriptionInfo != null) {
            Log.d(TAG, "--hasPurchase cache: " + mHasSubscription);

            if (subscriptionInventoryListener != null) {
                subscriptionInventoryListener.onSuccess(mHasSubscription);
            }

            return;
        }

        if (mIabHelper != null) {
            try {
                mIabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                        Purchase purchase = null;
                        if (inventory != null && inventory.hasPurchase(subscriptionType)) {
                            purchase = inventory.getPurchase(subscriptionType);
                        }

                        if (purchase != null ) {
                            Log.d(TAG, "--hasPurchase");
                            JSONObject subInfo = new JSONObject();

                            try {
                                JSONObject origJson = new JSONObject(inventory.getPurchase(subscriptionType).getOriginalJson());
                                subInfo.put(RESPONSE_INAPP_PURCHASE_DATA, origJson);
                                subInfo.put(RESPONSE_INAPP_SIGNATURE, inventory.getPurchase(subscriptionType).getSignature());
                                subInfo.put(RESPONSE_CODE, result.getResponse());
                            } catch (JSONException e) {
                                Log.e(TAG, "Error purchasing: " + e.getMessage());
                                PreferenceStoreUtil.getInstance().putBoolean(mPaidSubKey, false);
                                return;
                            }

                            Log.d(TAG, "subInfo: " + subInfo.toString());
                            String b64 = new String(Base64.encode(subInfo.toString().getBytes(), Base64.DEFAULT));
                            PreferenceStoreUtil.getInstance().putString(mPaidSubInfoKey, b64);
                            PreferenceStoreUtil.getInstance().putBoolean(mPaidSubKey, true);

                            mHasSubscription = true;
                            updatePrefStore(true, purchase);
                        } else {
                            Log.d(TAG, "--does NOT hasPurchase");
                            mHasSubscription = false;
                            updatePrefStore(false, null);
                        }

                        if (subscriptionInventoryListener != null) {
                            subscriptionInventoryListener.onSuccess(mHasSubscription);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "EXCEPTION:" + e.getMessage());
                updatePrefStore(false, null);
            }
        }
    }

    public void checkAllConfirmedSubs(final List<String> subTypes,
                                      final SubscriptionInventoryListener subscriptionInventoryListener
    ) {
        Log.d(TAG, "checking if hasSubscription");
        //if (true) {
        //    return;
        //}

        if (mIabHelper != null) {
            try {
                mIabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                        boolean hasPurchase = false;
                        String subscriptionType = "";

                        if(inventory != null) {
                            for (int i = 0; i < subTypes.size(); i++) {
                                subscriptionType = subTypes.get(i);
                                if(inventory.hasPurchase(subscriptionType))
                                {
                                    hasPurchase = true;
                                    break;
                                }
                            }
                        }

                        mHasSubscription = hasPurchase;

                        if (hasPurchase) {
                            Log.d(TAG, "--hasPurchase");
                            JSONObject subInfo = new JSONObject();

                            try {
                                JSONObject origJson = new JSONObject(inventory.getPurchase(subscriptionType).getOriginalJson());
                                subInfo.put(RESPONSE_INAPP_PURCHASE_DATA, origJson);
                                subInfo.put(RESPONSE_INAPP_SIGNATURE, inventory.getPurchase(subscriptionType).getSignature());
                                subInfo.put(RESPONSE_CODE, result.getResponse());
                                PreferenceStoreUtil.getInstance().putString(mCurrProductIdKey, origJson.getString("productId"));
                            } catch (JSONException e) {
                                Log.e(TAG, "Error purchasing: " + e.getMessage());
                                PreferenceStoreUtil.getInstance().putBoolean(mPaidSubKey, false);
                                return;
                            }

                            Log.d(TAG, "subInfo: " + subInfo.toString());
                            String b64 = new String(Base64.encode(subInfo.toString().getBytes(), Base64.DEFAULT));
                            PreferenceStoreUtil.getInstance().putString(mPaidSubInfoKey, b64);
                            PreferenceStoreUtil.getInstance().putBoolean(mPaidSubKey, true);
                        } else {
                            Log.d(TAG, "--does NOT hasPurchase");
                            PreferenceStoreUtil.getInstance().putBoolean(mPaidSubKey, false);
                        }

                        if (subscriptionInventoryListener != null) {
                            subscriptionInventoryListener.onSuccess(mHasSubscription);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "EXCEPTION:" + e.getMessage());
            }
        }
    }

    public void getSubInfo(final ArrayList<String> subTypes,
                                      final SubscriptionListListener subscriptionListListener
    ) {
        Log.d(TAG, "getting sub info");
        //if (true) {
        //    return;
        //}

        if (mIabHelper != null) {
            try {
                mIabHelper.queryInventoryAsync(true, subTypes, new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

                        ArrayList<JSONObject> resultList = new ArrayList<JSONObject>();

                        if(inventory != null) {
                            for (int i = 0; i < subTypes.size(); i++) {
                                if (inventory.hasDetails(subTypes.get(i))) {
                                    try {
                                        JSONObject origJson = new JSONObject(inventory.getSkuDetails(subTypes.get(i)).getOriginalJson());
                                        resultList.add(origJson);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error getting sku details: " + e.getMessage());
                                        return;
                                    }
                                }
                            }
                        }

                        if (subscriptionListListener != null) {
                            subscriptionListListener.onSuccess(resultList);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "EXCEPTION:" + e.getMessage());
            }
        }
    }

    public void dispose() {
        if (mIabHelper != null) {
            try {
                mIabHelper.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mIabHelper = null;
        }
    }

    public IabHelper getIabHelper() {
        if (mIabHelper == null) {
            mIabHelper = new IabHelper(mContext, base64EncodedPublicKey);
        }
        return mIabHelper;
    }

    public interface SubscriptionSetupListener {
        void onSuccess();
    }

    public interface SubscriptionInventoryListener {
        void onSuccess(boolean hasPurchase);
    }

    public interface SubscriptionListListener {
        void onSuccess(ArrayList<JSONObject> skuDetails);
    }

    public interface SubscriptionFinishedListener {
        void onSuccess();
    }
}