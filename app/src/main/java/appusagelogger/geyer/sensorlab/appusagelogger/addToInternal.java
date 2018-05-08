package appusagelogger.geyer.sensorlab.appusagelogger;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class addToInternal {

        public void addStringBuilderToInternalStorage(StringBuilder result, Context context) {
            long timestamp = System.currentTimeMillis() / 1000;

            String dataEntry = timestamp + " - " +
                    result + ":" + "\n";

            Log.i("addToInternal data:", dataEntry);

                try {
                    File path = context.getFilesDir();
                    File file = new File(path, Constants.STATS_FILE_NAME);
                    FileOutputStream fos = new FileOutputStream(file, true);
                    fos.write(dataEntry.getBytes());
                    fos.close();
                } catch (Exception e) {
                    Log.e("addToInternal error", "Exception " + e);
                }
            }

        public void addStringToInternalStorage(String result, Context context){
            long timestamp = System.currentTimeMillis()/1000;

            String dataEntry = timestamp +" - " +
                    result +":" + "\n";

            Log.i("addToInternal data:", dataEntry);

                try {
                    File path = context.getFilesDir();
                    File file = new File(path, Constants.STATS_FILE_NAME);
                    FileOutputStream fos = new FileOutputStream(file, true);
                    fos.write(dataEntry.getBytes());
                    fos.close();
                } catch (Exception e) {
                    Log.e("addToInternal error", "Exception " + e);
                }
            }
        }




