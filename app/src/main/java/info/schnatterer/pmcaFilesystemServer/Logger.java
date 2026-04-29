package info.schnatterer.pmcaFilesystemServer;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {

    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public static File getFile() {
        // e.g. /storage/sdcard0/pmcaFilesystemServer/LOG.TXT
        return new File(Environment.getExternalStorageDirectory(), "pmcaFilesystemServer/LOG.TXT");
    }

    public static final String ACTION_NEW_LOG = "info.schnatterer.pmcaFilesystemServer.ACTION_NEW_LOG";
    public static final String EXTRA_MESSAGE = "message";

    private static Context context;

    public static void init(Context ctx) {
        context = ctx.getApplicationContext();
    }

    protected static void log(String msg) {
        try {
            getFile().getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(), true));
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.US);
            String timestampedMsg = sdf.format(new Date()) + " " + msg;
            writer.append(timestampedMsg);
            writer.newLine();
            writer.close();

            if (context != null) {
                Intent intent = new Intent(ACTION_NEW_LOG);
                intent.putExtra(EXTRA_MESSAGE, msg);
                context.sendBroadcast(intent);
            }
        } catch (IOException e) {
            Log.e("pmcaFilesystemServer", "Error writing log", e);
        }
    }
    protected static void log(String type, String msg) { log("[" + type + "] " + msg); }

    public static void info(String msg) { log("INFO", msg); }
    public static void error(String msg) { log("ERROR", msg); }
}
