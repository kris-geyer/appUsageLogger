package appusagelogger.geyer.sensorlab.appusagelogger;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import static android.content.ContentValues.TAG;

public class collectingData extends Service {
    Handler statsHandler;
    String runningApp;
    String currentApp;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String currentApp = "NULL";
        runningApp = "";
        Log.i(TAG,"Service started");
        statsHandler = new Handler();
        statsHandler.postDelayed(runnable, 500);
        if (Build.VERSION.SDK_INT > 25) {
            startForeground(105, updateNotification());
        }
        return START_NOT_STICKY;
    }

    private Notification updateNotification() {


        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        return new NotificationCompat.Builder(this)
                .setContentTitle("Activity log")
                .setTicker("Ticker")
                .setContentText("recording of data is ongoing")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
    }

    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            printForegroundTask();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        statsHandler.removeCallbacks(runnable);
        Log.i(TAG, "Service destroyed");
    }

    private void printForegroundTask() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {


            @SuppressLint("WrongConstant") UsageStatsManager usm = (UsageStatsManager) this.getSystemService("usagestats");
            long time = System.currentTimeMillis();
            List<UsageStats> appList = null;

            try {
                appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);

                if (appList != null && appList.size() > 0) {
                    SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                    for (UsageStats usageStats : appList) {
                        mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                    }
                    if (mySortedMap != null && !mySortedMap.isEmpty()) {
                        currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                        PackageManager packageManager = getApplicationContext().getPackageManager();
                        try {
                            currentApp = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(currentApp, PackageManager.GET_META_DATA));
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }
                statsHandler.postDelayed(runnable, 10000);

                if(!Objects.equals(currentApp, runningApp)) {
                    storeStats(currentApp);
                    runningApp = currentApp;
                }
            }
            catch(Error error){
                sendMessageToMain("Error in collectingData service: " + error);
                storeStats("Error in collectingData service: " + error);
                statsHandler.postDelayed(runnable, 60000);
            }
        }
    }

    private void storeStats(String currentApp) {

        String dataEntry = " - App: " +
                currentApp + ";";

        Log.i("collectData dataentr", dataEntry);
        try {
            addToInternal addInternally = new addToInternal();
            addInternally.addStringToInternalStorage(dataEntry,this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if(Objects.equals(currentApp, "appUsageLogger")){
                    sendMessageToMain("Data collection ongoing");
                }
            }else{
                sendMessageToMain("Data collection ongoing");
            }
        } catch (Exception e) {
            Log.d("CollectDa writing file", "Exception", e);
        }

    }

    public void sendMessageToMain(String toRelay) {
        Intent intent = new Intent("changeInService");
        intent.putExtra("locationDataCollectionBegan", true);
        intent.putExtra("Status",toRelay);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.i("collectingData", "data sent to main");
    }
}
