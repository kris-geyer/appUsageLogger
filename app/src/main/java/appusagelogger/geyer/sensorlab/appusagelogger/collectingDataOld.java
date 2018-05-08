package appusagelogger.geyer.sensorlab.appusagelogger;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;
import java.util.Objects;

public class collectingDataOld extends Service {

    Handler foregroundHandler;
    String runningApp;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("from old", "running");
        runningApp = "b";
        foregroundHandler = new Handler();
        foregroundHandler.postDelayed(surveyForeground, 500);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        foregroundHandler.removeCallbacks(surveyForeground);
    }

    final Runnable surveyForeground = new Runnable() {
        @Override
        public void run() {
            reportForeground();
        }
    };

    private void reportForeground() {
        ActivityManager am = (ActivityManager)this.getSystemService(getApplicationContext().ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
        String currentApp = tasks.get(0).processName;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if(!Objects.equals(runningApp, currentApp)) {
                runningApp = currentApp;
                storeInternally(currentApp);
            }
        }
        else{
            Character firstLetterNew = runningApp.charAt(0);
            Character firstLetterOld = currentApp.charAt(0);
            int newC = (int) firstLetterNew;
            int oldC = (int) firstLetterOld;

            Character lastLetterNew = runningApp.charAt(runningApp.length()-1);
            Character lastLetterOld = currentApp.charAt(currentApp.length()-1);
            int newC1 = (int) lastLetterNew;
            int oldC1 = (int) lastLetterOld;

            if (newC != oldC && newC1 != oldC1) {
                runningApp = currentApp;
                storeInternally(currentApp);
            }
        }
        foregroundHandler.postDelayed(surveyForeground, 10000);
    }

    public void storeInternally(String currentApp) {
        String dataEntry = "app: " +
                currentApp + ";";

        Log.i("collectData dataentr", dataEntry);

        try {
            addToInternal addInternally = new addToInternal();
            addInternally.addStringToInternalStorage(dataEntry,this);
            sendMessageToMain("Data collection ongoing");
        } catch (Exception e) {
            Log.d("CollectDa writing file", "Exception", e);
        }
    }

    public void sendMessageToMain(String toRelay) {
        Intent intent = new Intent("changeInService");
        intent.putExtra("locationDataCollectionBegan", true);
        intent.putExtra("Status",toRelay);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.i("collectingDataOld", "data sent to main");
    }
}
