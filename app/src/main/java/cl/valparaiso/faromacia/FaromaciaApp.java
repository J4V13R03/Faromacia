package cl.valparaiso.faromacia;

import android.app.Application;
import android.os.Process;
import androidx.appcompat.app.AppCompatDelegate;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FaromaciaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                File dir = getExternalFilesDir(null);
                if (dir == null) dir = getFilesDir();
                File crashFile = new File(dir, "crash_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt");
                FileWriter fw = new FileWriter(crashFile);
                PrintWriter pw = new PrintWriter(fw);
                pw.println("=== CRASH REPORT ===");
                pw.println("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
                pw.println("Android: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")");
                pw.println("Time: " + new Date().toString());
                pw.println();
                throwable.printStackTrace(pw);
                pw.close();
                fw.close();
            } catch (Exception ignored) {
            }
            Process.killProcess(Process.myPid());
            System.exit(1);
        });
    }
}
