package com.trytunnels.android.utils;

import android.content.Context;
import android.util.Log;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.*;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Authenticator;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.Credentials;

/**
 * Created by jeff on 12/8/17.
 */

public class ApiUtil {
    private static ApiUtil INSTANCE = null;

    private OkHttpClient mHttpClient;
    private ClearableCookieJar cookieJar;

    private static final String TAG = "ApiUtil";
    //private static final String API_URL = "https://api.trusty-eu.science";
    //private static final String API_URL = "https://api.confirmedvpn.com";

    private ApiUtil() {
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    };

    public static ApiUtil getInstance()
    {
        if(INSTANCE == null)
        {
            INSTANCE = new ApiUtil();
        }
        return INSTANCE;
    }

    /*public ApiUtil() {
        //No instance
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }*/

    public void clearCookieJar()
    {
        cookieJar.clear();
    }

    public void initiateClient(Context context) {
        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));

        //No instance
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
    }

    public void initiateClient(Context context, String email, String password) {
        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));

        //No instance
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();

        signIn(email, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "signIn Failure: " + e.getMessage());
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
            }
        });
    }

    public void initiateClient(Context context, String email, String password, Callback callback) {
        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));

        //No instance
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();

        signIn(email, password, callback);
    }

    public boolean initiateClientSync(Context context, String email, String password) {
        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));

        //No instance
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();

        Response response = null;

        try {
            int startVersion = ConfirmedConstants.getVersionNumber();
            do {
                response = signInSync(email, password);
                if(response.code() == 200)
                {
                    return true;
                }
                else
                {
                    ConfirmedConstants.incrementVersion();
                }
            } while(ConfirmedConstants.getVersionNumber() != startVersion);

            return false;
        }
        catch(IOException e)
        {
            return false;
        }
    }

    public void initiateClient(Context context, String receipt) {
        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));

        //No instance
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();

        signIn(receipt, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "signIn Failure: " + e.getMessage());
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
            }
        });
    }

    public void initiateClient(Context context, String receipt, Callback callback) {
        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));

        //No instance
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();

        signIn(receipt, callback);
    }

    public boolean checkSignedIn()
    {
        List<Cookie> cookies = cookieJar.loadForRequest(HttpUrl.parse(ConfirmedConstants.getApiUrl()));

        if(cookies.size() > 0)
        {
            return true;
        }

        return false;
    }

    /*public ApiUtil(final String email, final String password)
    {
        mHttpClient = new OkHttpClient.Builder()
                .authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                /*if (responseCount(response) >= 3) {
                    return null; // If we've failed 3 times, give up. - in real life, never give up!!
                }
                        String credential = Credentials.basic(email, password);
                        return response.request().newBuilder().header("Authorization", credential).build();
                    }
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }*/

    public void signIn(String email, String password, Callback callback)
    {
        String url = ConfirmedConstants.getSignInUrl();

        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "sign-in request");


        mHttpClient.newCall(request).enqueue(callback);
    }

    public Response signInSync(String email, String password) throws IOException
    {
        String url = ConfirmedConstants.getSignInUrl();

        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "sign-in request");

        return mHttpClient.newCall(request).execute();
    }

    public void signIn(String receipt, Callback callback)
    {
        String url = ConfirmedConstants.getSignInUrl();

        RequestBody formBody = new FormBody.Builder()
                .add("authtype", "android")
                .add("authreceipt", receipt)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "sign-in request");


        mHttpClient.newCall(request).enqueue(callback);
    }

    /*public void createUserWithReceipt(String receipt, Callback callback) {
        String url = API_URL + "/create-user-with-receipt";

        RequestBody formBody = new FormBody.Builder()
                .add("receipt", receipt)
                .add("type", "android")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "createUserWithReceipt request");
        Log.d(TAG, "createUserWithReceipt receipt: " + receipt);


        mHttpClient.newCall(request).enqueue(callback);
    }*/

    public void getKey(Callback callback) {
        String url = ConfirmedConstants.getGetKeyUrl();

        RequestBody formBody = new FormBody.Builder()
                .build();

        HttpUrl queryUrl;

        if(ConfirmedConstants.getVersionNumber() < 3)
        {
            queryUrl = HttpUrl.parse(url).newBuilder()
                    .addQueryParameter("platform","android")
                    .addQueryParameter("source", "both")
                    .build();
        }
        else
        {
            queryUrl = HttpUrl.parse(url).newBuilder()
                    .addQueryParameter("platform","android")
                    .build();
        }

        Request request = new Request.Builder()
                .url(queryUrl)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "getKey request");


        mHttpClient.newCall(request).enqueue(callback);
    }

    public Response getKeySync() throws IOException
    {
        String url = ConfirmedConstants.getGetKeyUrl();

        RequestBody formBody = new FormBody.Builder()
                .build();

        HttpUrl queryUrl;
        if(ConfirmedConstants.getVersionNumber() < 3)
        {
            queryUrl = HttpUrl.parse(url).newBuilder()
                    .addQueryParameter("platform","android")
                    .addQueryParameter("source", "both")
                    .build();
        }
        else
        {
            queryUrl = HttpUrl.parse(url).newBuilder()
                    .addQueryParameter("platform","android")
                    .build();
        }

        Request request = new Request.Builder()
                .url(queryUrl)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "getKey request");


        return mHttpClient.newCall(request).execute();
    }

    public void convertShadowUser(String email, String password, String receipt, Callback callback)
    {
        String url = ConfirmedConstants.getAddEmailToUserUrl();
        RequestBody formBody = new FormBody.Builder()
                .add("newemail", email)
                .add("newpassword", password)
                .add("authtype", "android")
                .add("authreceipt", receipt)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "convertShadowUser request");
        mHttpClient.newCall(request).enqueue(callback);
    }

    static public void getSpeedBucket(Callback callback)
    {
        OkHttpClient client = new OkHttpClient();

        String url = ConfirmedConstants.getSpeedBucketUrl();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        Log.d(TAG, "getSpeedBucket request");


        client.newCall(request).enqueue(callback);
    }

    /*public void getSubscriptionNoReceipt(Callback callback)
    {
        String url = ConfirmedConstants.getSubscriptionInformationUrl();

        Request request = new Request.Builder()
                .url(HttpUrl.parse(url).newBuilder().addQueryParameter("type","android").build())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        Log.d(TAG, "getKey request");


        mHttpClient.newCall(request).enqueue(callback);
    }

    public void getSubscriptionWithReceipt(String receipt, Callback callback)
    {
        String url = ConfirmedConstants.getSubscriptionInformationUrl();

        RequestBody formBody = new FormBody.Builder()
                .add("authtype", "android")
                .add("authreceipt", receipt)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "subscription request");
        mHttpClient.newCall(request).enqueue(callback);
    }*/

    public void getSubscription(Callback callback)
    {
        String url = ConfirmedConstants.getActiveSubscriptionsUrl();

        RequestBody formBody = new FormBody.Builder()
                .build();

        Request request = new Request.Builder()
                .url(HttpUrl.parse(url).newBuilder().addQueryParameter("platform","android").build())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "getActiveSubscriptions request");


        mHttpClient.newCall(request).enqueue(callback);
    }

    public void subscriptionEvent(String receipt, Callback callback)
    {
        String url = ConfirmedConstants.getSubscriptionReceiptUploadUrl();

        RequestBody formBody;

        if(ConfirmedConstants.getVersionNumber() == 1)
        {
            formBody = new FormBody.Builder()
                    .add("platform", "android")
                    .add("receipt", receipt)
                    .build();
        }
        else
        {
            formBody = new FormBody.Builder()
                    .add("authtype", "android")
                    .add("authreceipt", receipt)
                    .build();
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        Log.d(TAG, "subscription-event request");
        mHttpClient.newCall(request).enqueue(callback);
    }
}