package com.trytunnels.android.utils;

import android.util.Log;

public class ConfirmedConstants
{
    private static String tt_api_version_key = "api_version";
//    private static String tt_version_one_key = "v1";
//    private static String tt_version_two_key = "v2";
    private static String tt_version_three_key = "v3";
    private static String tt_paid_sub_info_key = "paid_sub_info";
//    private static String tt_certificate_chain_v2 = "-090718";
    private static String tt_certificate_chain_v3 = "-111818";

    //private static String vpnDomain = "confirmedvpn.com";

    public static int getVersionNumber()
    {

//        if(!PreferenceStoreUtil.getInstance().contains(tt_api_version_key))
//        {
//            // if receipt exists, but version was never set, probably an upgrade so go v1
//            String receipt = PreferenceStoreUtil.getInstance().getString(tt_paid_sub_info_key, "");
//            if(!receipt.equals(""))
//            {
//                ConfirmedConstants.forceVersion1();
//            }
//            else
//            {
//                ConfirmedConstants.forceLatestVersion();
//            }
//        }
//
//        String currentVersion = PreferenceStoreUtil.getInstance().getString(tt_api_version_key, tt_version_three_key);
//
//        if(currentVersion.equals(tt_version_three_key))
//        {
            ConfirmedConstants.forceLatestVersion();
            return 3;
//        }
//        else if(currentVersion.equals(tt_version_two_key))
//        {
//            return 2;
//        }
//        else
//        {
//            return 1;
//        }
    }

    static String getVpnDomain()
    {
//        if(getVersionNumber() == 3)
//        {
//            return "confirmedvpn.com";
//        }
//        else
//        {
            return "confirmedvpn.com";
//        }
    }

//    public static void incrementVersion()
//    {
//        switch(getVersionNumber())
//        {
//            case 1:
//                PreferenceStoreUtil.getInstance().putString(tt_api_version_key, tt_version_two_key);
//                break;
//            case 2:
//                PreferenceStoreUtil.getInstance().putString(tt_api_version_key, tt_version_three_key);
//                break;
//            case 3:
//                PreferenceStoreUtil.getInstance().putString(tt_api_version_key, tt_version_one_key);
//                break;
//                default:
//        }
//    }

//    static void forceVersion1()
//    {
//        PreferenceStoreUtil.getInstance().putString(tt_api_version_key, tt_version_one_key);
//    }

    public static void forceLatestVersion()
    {
        PreferenceStoreUtil.getInstance().putString(tt_api_version_key, tt_version_three_key);
    }

    static String getApiUrl()
    {
//        switch(getVersionNumber())
//        {
//            case 1:
//                Log.d("API_VERSION", "APIVERSION: v1");
//                return "https://v1." + getVpnDomain();
//            case 2:
//                Log.d("API_VERSION", "APIVERSION: v2");
//                return "https://v2." + getVpnDomain();
//            case 3:
//            default:
//                Log.d("API_VERSION", "APIVERSION: v3");
                return "https://" + getVpnDomain();
//        }
    }

    static String getSourceId()
    {
//        switch(getVersionNumber())
//        {
//            case 1:
//                return "";
//            case 2:
//                return tt_certificate_chain_v2;
//            case 3:
//                default:
                return tt_certificate_chain_v3;
//        }
    }

    public static String getEndPoint(String prefix)
    {
        return prefix + getSourceId() + "." + getVpnDomain();
    }

    public static String getRemoteId()
    {
        return "www" + getSourceId() + ".confirmedvpn.com";
    }

    static String getSignInUrl()
    {
        return getApiUrl() + "/signin";
    }

    static String getAddEmailToUserUrl()
    {
        return getApiUrl() + "/convert-shadow-user";
    }

    static String getGetKeyUrl()
    {
        return getApiUrl() + "/get-key";
    }

    static String getSubscriptionInformationUrl()
    {
        return getApiUrl() + "/subscriptions";
    }

    static String getSubscriptionReceiptUploadUrl()
    {
        return getApiUrl() + "/subscription-event";
    }

    static String getActiveSubscriptionsUrl()
    {
        return getApiUrl() + "/active-subscriptions";
    }

    static String getSpeedBucketUrl() { return "https://confirmedvpn.com/download-speed-test"; }
}
