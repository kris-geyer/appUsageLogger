package appusagelogger.geyer.sensorlab.appusagelogger;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    TextView result;
    List pkgAppList;
    StringBuilder builder;
    private static final int MY_PERMISSION_REQUEST_PACKAGE_USAGE_STATS = 101;

    //-------------------------------------------------determining flow of code ---------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        documentApps();
        initiate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            detectStateOfPackagePermissions();
        }
        else{
            operationCoachKitKat();
        }
    }

//  ----------------------------------------------------Initializing visible components----------------------------------------------------------------

    //initiating the components that user interacts with
    private void initiate() {
            //text view
        result = findViewById(R.id.tvResult);

            //Email button
        Button email = findViewById(R.id.btnEmail);
        email.setOnClickListener(this);


            //receiver of data collection starting
        LocalBroadcastManager.getInstance(this).registerReceiver(dataCollectionInitiated, new IntentFilter("changeInService"));

    }

    //handling button presses function
    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btnEmail:
                sendData(this);
                break;
        }
    }

    //---------------------------------------functions called by button presses-------------------------------------------

    private void sendData(Context context) {

        //returns an encrypted string
        String statsEncrypted = read_file(getApplicationContext(), Constants.STATS_FILE_NAME);

        //attempts to store the stats file to the file 'STATS_FILE_NAME_FINAL'
        try {
            File path = this.getFilesDir();
            File file = new File(path, Constants.STATS_FILE_NAME_FINAL);
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(statsEncrypted.getBytes());
            fos.close();
        } catch (Exception e) {
            Log.d("screenServ writing file", "Exception", e);
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String directory = (String.valueOf(context.getFilesDir()) + File.separator);
        Log.i("Directory", directory);

        File statsFile = new File(directory + File.separator + Constants.STATS_FILE_NAME_FINAL);


        if(statsFile.exists()){
            Log.i("File exists", "true");
        }
        else {
            Log.e("File exists", "False");
        }

        //Attempts to upload encrypted data
        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context, "appusagelogger.geyer.sensorlab.appusagelogger.fileprovider", statsFile);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        }
        catch (Exception e){
            Log.e("File upload error", "Error:" + e);
        }

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
        Log.d("from main", "Worked fine");
    }

    //reads the stored file.
    public String read_file(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception error) {
            return "";
        }
    }

    //determining if permissions are required
    //contained logic: if participant has permission and data should be collected start collecting data.
    //if participants has permission but should not be collecting data then uninstall the app
    //else request permissions
    private void detectStateOfPackagePermissions() {

            if (hasPermission()) {
                Log.i("From mainActivity", "going to start Service");
                if (Build.VERSION.SDK_INT < 26) {
                    startService(new Intent(this, screenService.class));
                }
                else {
                    startForegroundService(new Intent(this, screenService.class));
                }
                result.setText(getString(R.string.pleaseCloseScreen));
            }
            //if participant does not have permission to access the package then the participant will be prompted to give usage statistics permission
            else {
                requestPackagePermission(null);
            }
        }


    //detects if app has permission to access app usage.
    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            if (appOpsManager != null) {

                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
            }

            Log.i("From mainActivity", "Permission given: " + String.valueOf(mode == AppOpsManager.MODE_ALLOWED));
            return mode == AppOpsManager.MODE_ALLOWED;
        }
        else {
            return false;
        }
    }

    //request usage statistics permission
    private void requestPackagePermission(View view) {
        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Usage permission")
                .setMessage("To participate in this experiment you must enable usage stats permission")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), MY_PERMISSION_REQUEST_PACKAGE_USAGE_STATS);
                    }
                })
                .show();
    }


    //detects the results of requesting permission from the usage statistics
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_PACKAGE_USAGE_STATS:
                if(hasPermission()){
                    Log.i("From mainActivity", "going to start Service");
                    if (Build.VERSION.SDK_INT < 26) {
                        startService(new Intent(this, screenService.class));
                    }
                    else {
                        startForegroundService(new Intent(this, screenService.class));
                    }
                    result.setText(getString(R.string.pleaseCloseScreen));
                }
        }
    }

    //---------------------------------------------document installed apps------------------------------------------------

    private void documentApps() {
        SharedPreferences preferences = getSharedPreferences("Data collection", Context.MODE_PRIVATE);
        if(!preferences.getBoolean("apps documented", false)){
            documentAppsMain();
        }
    }

    private void documentAppsMain() {
        Log.i("main", "documentAppsMain - initiated");
        try{
            StringBuilder recordedApps = surveyApps();
            storeInternally(recordedApps);
        }catch (Exception e){
            Log.e("installedApps", "Exception: " + e);
        }
    }

    private void storeInternally(StringBuilder recordedApps) {
        Log.i("installedApps", String.valueOf(recordedApps));
        try {
            addToInternal addInternally = new addToInternal();
            addInternally.addStringBuilderToInternalStorage(recordedApps,this);
        } catch (Exception e) {
            Log.d("CollectDa writing file", "Exception", e);
        } finally {

        }
    }

    private StringBuilder surveyApps() {
        try{
            SharedPreferences preferences = getSharedPreferences("Apps", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            pkgAppList = getPackageManager().queryIntentActivities(mainIntent,0);
            builder = new StringBuilder();
            int appNum = 0;
            for (Object object: pkgAppList){
                ResolveInfo info = (ResolveInfo) object;
                if(appNum>0) {
                    builder.append(" -" + getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo) + "\n");
                }
                else{
                    builder.append(getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo) + "\n");
                }
                appNum++;
                editor.putString("App number " + appNum, ""+ getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo));
            }
            editor.putInt("Number of apps", appNum);
            editor.apply();

            SharedPreferences preferences1 = getSharedPreferences("Data collection", Context.MODE_PRIVATE);
            SharedPreferences.Editor editorImportant = preferences1.edit();
            editorImportant.putBoolean("apps documented", true);
            editorImportant.apply();
        }catch (Exception e){
            Log.e("document apps", "Error: " + e);
            result.setText("Error detected, please inform researcher that: " + e);
        }
        return builder;
    }



    //----------------------------------code for phone that haven't access to usage statistics---------------------------

    private void operationCoachKitKat (){
        SharedPreferences preferences = getSharedPreferences("Data collection", Context.MODE_PRIVATE);
        if(preferences.getBoolean("collect data", true)){
            startService(new Intent(this, screenService.class));
        }
    }


    //----------------------------------------Input from other activities/services of the app-----------------------------

    private BroadcastReceiver dataCollectionInitiated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("locationDataCollectionBegan", false)){
                String toRelay = intent.getStringExtra("Status");

                String toShow;
                SharedPreferences preferences = getSharedPreferences("Data collection", Context.MODE_PRIVATE);
                if(preferences.getBoolean("apps documented", false)) {
                    toShow = "Status: " + "\n" +
                            "Apps documented" + "\n" +
                            toRelay;

                    result.setText(toShow);
                    result.setGravity(Gravity.CENTER);
                }
                else{
                    toShow = "Status: " + "\n" +
                            "Apps not documented" + "\n" +
                            toRelay;

                    result.setText(toShow);
                    result.setGravity(Gravity.CENTER);
                }
            }
        }
    };
}