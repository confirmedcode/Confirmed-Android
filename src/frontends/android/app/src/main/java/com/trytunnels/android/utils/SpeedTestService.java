package com.trytunnels.android.utils;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.confirmed.tunnels.ConfirmedVPNAppWidget;
import com.trytunnels.android.ui.SignInActivity;

import org.json.JSONObject;
import org.strongswan.android.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SpeedTestService extends Service
{
    public static String intentName = "com.tunnels.speedTestIntent";
    public static String resultKey = "speed_test_result";
    public static String forWidgetKey = "is_for_widget";

    private static final String TAG = "SpeedTestService";

    private String downloadUrl;
    private int totalBytes = 0;
    private int totalInstances = 1;
    private String fileName = "10MB.bin";
    private long fileTimer = 0;
    private boolean forWidget = false;

    @Override
    public void onCreate()
    {
        super.onCreate();

        totalInstances = 1;

        final SpeedTestService service = this;

        ApiUtil.getSpeedBucket(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getSpeedBucket failure: " + e.getMessage());

                sendBadResult();
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

                    String bucket = jsonObj.getString("bucket");

                    if (bucket.equals("")) {
                        throw new Exception("Invalid json");
                    }

                    final ConnectivityManager connMgr = (ConnectivityManager)
                            service.getSystemService(Context.CONNECTIVITY_SERVICE);
                    final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (wifi.isConnectedOrConnecting ()) {
                        fileName = "50MB.bin";
                    }

                    downloadUrl = "https://" + bucket + ".s3-accelerate.amazonaws.com/" + fileName;

                    ExecutorService tpExecutor = new ThreadPoolExecutor(
                            totalInstances,
                            totalInstances,
                            10,
                            TimeUnit.SECONDS,
                            new LinkedBlockingDeque<Runnable>()
                    );

                    fileTimer = System.currentTimeMillis();

                    for(int i=0;i<totalInstances;i++) {
                        //new DownloadFileFromURL().execute(downloadUrl);
                        tpExecutor.execute(new DownloadFileRunnable());
                    }
                } catch (Exception e) {
                    sendBadResult();
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Bundle bundle = intent.getExtras();

        if(bundle != null) {
            forWidget = bundle.getBoolean(forWidgetKey, false);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private synchronized void reportBytes(int reportedBytes)
    {
        if(reportedBytes != -1 && totalBytes != -1) {
            totalBytes += reportedBytes;
            //fileTimer += downloadTime;
        }
        else
        {
            totalBytes = -1;
        }

        totalInstances--;

        // it's over
        if(totalInstances == 0)
        {
            String result = "N/A";

            if(totalBytes != -1) {
                long timeItTook = System.currentTimeMillis() - fileTimer;
                double secondsTime = (double) timeItTook / 1000.0;

                double mbps = (((double) totalBytes * 8.0) / 1000000.0) / secondsTime;

                Log.d("SPEED_TEST", "Mbps: " + mbps);

                result = String.format("%.1f Mbps", mbps);
            }

            if(forWidget)
            {
                updateWidgetWithSpeed(result);
            }
            else
            {
                Intent intent = new Intent(intentName);
                intent.putExtra(resultKey, result);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }

            stopSelf();
        }
    }

    private void sendBadResult()
    {
        String result = "N/A";

        if(forWidget)
        {
            updateWidgetWithSpeed(result);
        }
        else
        {
            Intent intent = new Intent(intentName);
            intent.putExtra(resultKey, result);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private void updateWidgetWithSpeed(String result)
    {
        Context context = this;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.confirmed_vpnapp_widget);
        ComponentName thisWidget = new ComponentName(context, ConfirmedVPNAppWidget.class);

        remoteViews.setTextViewText(R.id.txt_widget_speed_test, "Speed: " + result);
        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
    }

    class DownloadFileRunnable implements Runnable
    {
        int byteCount = 0;
        //long downloadTime = -1;

        @Override
        public void run()
        {
            int count;
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                //URLConnection conection = url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setUseCaches(false);
                //conection.connect();

                //ReadableByteChannel rbc = Channels.newChannel(url.openStream());

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                //int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(conn.getInputStream(),
                1000000);

                // Output stream
                /*OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory().toString()
                        + "/2011.kml");*/

                byte data[] = new byte[16384];

                //long total = 0;

                //long timer = System.nanoTime();
                while ((count = input.read(data)) != -1) {
                    //long diff = System.nanoTime() - timer;
                    byteCount += count;
                    //byteCount++;
                    //double mbs = ((double)byteCount / (((double)diff / 1000000.0) / 1000.0))/1000000.0;

                    //Log.d("SPEED_TEST", "Samp: " + mbs);
                    //timer = System.nanoTime();
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    //publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    //output.write(data, 0, count);
                }

                //downloadTime = System.currentTimeMillis() - timer;
                // flushing output
                //output.flush();

                // closing streams
                //output.close();
                //input.close();

                /*File outputDir = getCacheDir();
                File outputFile = File.createTempFile("prefix", "extension", outputDir);

                FileOutputStream fos = new FileOutputStream(outputFile);
                byteCount = (int)fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);*/
                /*ByteBuffer buffer = java.nio.ByteBuffer.allocate(16384);

                while((count = rbc.read(buffer)) != -1)
                {
                    byteCount += count;
                }

                rbc.close();*/

                /*InputStream in = url.openStream();
                Files.copy(in, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);*/

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                byteCount = -1;
            }

            reportBytes(byteCount);
        }
    }

    /**
     * Background Async Task to download file
     * */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        private int byteCount = 0;

        /**
         * Before starting background thread Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //showDialog(progress_bar_type);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                //URLConnection conection = url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                //conection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                //int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = conn.getInputStream(); //new BufferedInputStream(url.openStream(),
                        //8192);

                // Output stream
                /*OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory().toString()
                        + "/2011.kml");*/

                byte data[] = new byte[8192];

                //long total = 0;

                while ((count = input.read(data)) != -1) {
                    byteCount += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    //publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    //output.write(data, 0, count);
                }

                // flushing output
                //output.flush();

                // closing streams
                //output.close();
                input.close();


            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                byteCount = -1;
            }

            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            //pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            //dismissDialog(progress_bar_type);
            reportBytes(byteCount);
        }

    }
}
