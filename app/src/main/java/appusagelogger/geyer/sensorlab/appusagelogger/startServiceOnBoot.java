package appusagelogger.geyer.sensorlab.appusagelogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class startServiceOnBoot extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                if (Build.VERSION.SDK_INT < 26) {
                    Intent initiateServices = new Intent(context, phonerestarted.class);
                    context.startService(initiateServices);
                } else {
                    Intent initiateServices = new Intent(context, phonerestarted.class);
                    context.startForegroundService(initiateServices);
                }
            }
        }
}
