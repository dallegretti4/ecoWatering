package it.uniba.dib.sms2324.ecowateringhub.service;

import android.app.Application;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class EcoWateringApplication extends Application {
    private static final String PROCESS_NAME_ENDING = ":persistent_service";
    @Override
    public void onCreate() {
        super.onCreate();

        if (isPersistentServiceProcess()) {
            WorkManager.initialize(this, new Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.INFO)
                    .build()
            );
        }
    }

    private boolean isPersistentServiceProcess() {
        String processName = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            processName = Application.getProcessName();
        }
        else {
            int pid = android.os.Process.myPid();
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
                processName = reader.readLine().trim();
                reader.close();
                return processName.endsWith(PROCESS_NAME_ENDING);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return processName != null && processName.endsWith(PROCESS_NAME_ENDING);
    }
}
