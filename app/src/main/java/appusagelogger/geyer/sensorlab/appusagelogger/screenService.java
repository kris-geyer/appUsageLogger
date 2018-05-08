package appusagelogger.geyer.sensorlab.appusagelogger;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class screenService extends Service {

    BroadcastReceiver screenReceiver;
    BroadcastReceiver appReceiver;
    BroadcastReceiver dateReceiver;

    Handler statsGeneratorHandler;
    String currentlyRunningApp;
    addToInternal addInternally;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRunningInForeground();
        addInternally = new addToInternal();
        documentServiceStart();
        registerReceivers();

        return START_STICKY;
    }

    private void startRunningInForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            if(Build.VERSION.SDK_INT > 26){
                String CHANNEL_ONE_ID = "sensor.example. geyerk1.inspect.screenservice";
                String CHANNEL_ONE_NAME = "Screen service";
                NotificationChannel notificationChannel = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                            CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_MIN);
                    notificationChannel.enableLights(true);
                    notificationChannel.setLightColor(Color.RED);
                    notificationChannel.setShowBadge(true);
                    notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.createNotificationChannel(notificationChannel);
                }

                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.jaunt_icon);
                Notification notification = new Notification.Builder(getApplicationContext())
                        .setChannelId(CHANNEL_ONE_ID)
                        .setContentTitle("Recording data")
                        .setSmallIcon(R.drawable.ic_screen_service)
                        .setLargeIcon(icon)
                        .build();

                Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

                statsGeneratorHandler = new Handler();
                currentlyRunningApp = "";
                startForeground(101, notification);
            }
            else{
                startForeground(101, updateNotification());

            }
        }else{
            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_screen_service)
                    .setContentTitle("Recording data")
                    .setContentIntent(pendingIntent).build();

            startForeground(101, notification);
        }

    }

    //---------------------------------notification for running service-----------------------------

    private Notification updateNotification() {

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        return new NotificationCompat.Builder(this)
                .setContentTitle("Activity log")
                .setTicker("Ticker")
                .setContentText("data recording a is on going")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
    }

    //--------------------------------------documenting service is running--------------------------

    private void documentServiceStart() {
        //logging that the service is running
        Log.i("screenService", "started running");

        //declaring service running
        SharedPreferences servicePreferences = getSharedPreferences("Data collection", Context.MODE_PRIVATE);

        //documenting first log on
        SharedPreferences.Editor serviceEditor = servicePreferences.edit();
        serviceEditor.putBoolean("screenService running", true);
        serviceEditor.apply();
    }

    //--------------------------------------register receivers--------------------------------------

    private void registerReceivers() {
        //registering screenReceiver
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String result = "";
                try {
                    switch (intent.getAction()) {
                        case Intent.ACTION_SCREEN_ON:
                            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                                startService(new Intent(getBaseContext(), collectingDataOld.class));
                            }
                            else if (Build.VERSION.SDK_INT < 26 && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                                startService(new Intent(getBaseContext(), collectingData.class));
                            } else if (Build.VERSION.SDK_INT == 26) {
                                startForegroundService(new Intent(getBaseContext(), collectingData.class));
                            } else if(Build.VERSION.SDK_INT == 27) {
                                statsGeneratorHandler.postDelayed(statsRunnable, 500);
                            }
                            result = "Screen on";
                            break;
                        case Intent.ACTION_SCREEN_OFF:
                            result = "Screen off";
                            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                                stopService(new Intent(getBaseContext(), collectingDataOld.class));
                            }
                            else if(Build.VERSION.SDK_INT  > 27 && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                                stopService(new Intent(getBaseContext(), collectingData.class));
                            }
                            else if (Build.VERSION.SDK_INT == 27){
                                statsGeneratorHandler.removeCallbacks(statsRunnable);
                            }
                            break;
                        case Intent.ACTION_USER_PRESENT:
                            result = "phone unlocked";
                            break;
                        default:
                            //do nothing
                    }
                    storeInternally(result);
                } catch (Exception e) {
                    Log.e("screenReceiver", "Error: " + e);
                }
            }
        };

        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenFilter.addAction(Intent.ACTION_USER_PRESENT);

        registerReceiver(screenReceiver, screenFilter);

        //registering appReceiver
        appReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String result = "";
                try {
                    switch (intent.getAction()) {
                        case Intent.ACTION_PACKAGE_ADDED:
                            Collection<String> entry = getNewApp();
                            result = "App added: " + entry;
                            updatePrefs();
                            break;
                        case Intent.ACTION_PACKAGE_REMOVED:
                            Collection<String> entry1 = getOldApp();
                            result = "App deleted: " + entry1;
                            updatePrefs();
                            break;
                    }
                    storeInternally(result);
                }
                catch (Exception e){
                    Log.e("AppReceiver", "Error: "+ e);
                }
            }
        };


        IntentFilter appFilter = new IntentFilter();
        appFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appFilter.addDataScheme("package");

        registerReceiver(appReceiver, appFilter);

    }

    //---------------------------------runnable for SKD version 27----------------------------------


    final Runnable statsRunnable = new Runnable() {
        @Override
        public void run() {
            printForegroundTask();
        }
    };

    private void printForegroundTask() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            String currentApp = "NULL";

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
                statsGeneratorHandler.postDelayed(statsRunnable, 10000);
                if(Objects.equals(currentApp,"appUsageLogger")) {
                    sendMessageToMain("Data collection ongoing");
                }
                if(!Objects.equals(currentApp, currentlyRunningApp)) {
                    storeInternally("App: " + currentApp);
                    currentlyRunningApp = currentApp;
                }
            }
            catch(Error error){
                sendMessageToMain("Error in collectingData service: " + error);
                storeInternally("Error in collectingData service: " + error);
                statsGeneratorHandler.postDelayed(statsRunnable, 60000);
            }
        }
    }

    //----------------------------handling change in installed apps---------------------------------

    private void updatePrefs() {

        SharedPreferences preferences = getSharedPreferences("Apps", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List pkgAppList = getPackageManager().queryIntentActivities(mainIntent,0);

        int appNum = 0;
        for (Object object: pkgAppList){
            ResolveInfo info = (ResolveInfo) object;
            appNum++;
            editor.putString("App number " + appNum, ""+ getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo));
        }
        editor.putInt("Number of apps", appNum);
        editor.apply();
    }

    private Collection<String> getNewApp() {
        SharedPreferences preferences = getSharedPreferences("Apps", Context.MODE_PRIVATE);
        int numOfApps = preferences.getInt("Number of apps", 0);
        Collection<String> appNames = new ArrayList<>();
        for (int i = 0; i < numOfApps; i++) {
            appNames.add(preferences.getString("App number " + (i + 1), "false"));
        }

        Collection<String> newApps = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List pkgAppList = getPackageManager().queryIntentActivities(mainIntent,0);

        for (Object object: pkgAppList){
            ResolveInfo info = (ResolveInfo) object;
            newApps.add(""+getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo));
        }

        newApps.removeAll(appNames);
        return newApps;
    }


    private Collection<String> getOldApp() {
        SharedPreferences preferences = getSharedPreferences("Apps", Context.MODE_PRIVATE);
        int numOfApps = preferences.getInt("Number of apps", 0);
        Collection<String> appNames = new ArrayList<>();
        for (int i = 0; i < numOfApps; i++) {
            appNames.add(preferences.getString("App number " + (i + 1), "false"));
        }

        Collection<String> newApps = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List pkgAppList = getPackageManager().queryIntentActivities(mainIntent,0);

        for (Object object: pkgAppList){
            ResolveInfo info = (ResolveInfo) object;
            newApps.add(""+getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo));
        }
        appNames.removeAll(newApps);
        return appNames;
    }


    //---------------------------------------store data---------------------------------------------


    private void storeInternally(String result) {

        Log.i("screenService dataentr", result);

        try {
            addInternally.addStringToInternalStorage(result,this);
        } catch (Exception e) {
            Log.d("CollectDa writing file", "Exception", e);
        }
    }


    //-------------------------------response to killing service------------------------------------

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(screenReceiver);
            Log.i("Receivers", "unregistered screen receiver");

        }
        catch (Exception e){
            Log.e("from screen receiver", "Error: " + e);
        }
        try {
            unregisterReceiver(appReceiver);
            Log.i("Receivers", "unregistered app receiver");
        }
        catch (Exception e){
            Log.e("from appReceiver", "Error: " + e);
        }
        try {
            unregisterReceiver(dateReceiver);
            Log.i("Receivers", "unregistered date receiver");
        }
        catch (Exception e){
            Log.e("from dateReceiver", "Error: " + e);
        }
        statsGeneratorHandler.removeCallbacks(statsRunnable);

        SharedPreferences preferences = getSharedPreferences("Data collection", Context.MODE_PRIVATE);
        Calendar cal = Calendar.getInstance();

        int initialDay = preferences.getInt("date started", 0);
        int currentDay= cal.get(Calendar.DAY_OF_YEAR);

        try {
            if ((currentDay - initialDay) < 10) {
                startService(new Intent(getBaseContext(), toRestartScreenService.class));
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(getBaseContext(), toRestartScreenService.class));
                }
            }
        }
        catch (Exception e){
            Log.e("killScreenService", "error "+ e);
        }
        Log.i("Kill intent", "sent");
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        //insert code
    }

    //------------------------------------returning data to main activity---------------------------
    public void sendMessageToMain(String toRelay) {
        Intent intent = new Intent("changeInService");
        intent.putExtra("locationDataCollectionBegan", true);
        intent.putExtra("Status",toRelay);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.i("collectingData", "data sent to main");
    }
}
