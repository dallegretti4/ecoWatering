package it.uniba.dib.sms2324.ecowateringhub.service;

import android.app.Application;
import android.os.Build;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class EcoWateringApplication extends Application {
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
                return processName.endsWith(":persistent_service");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return processName != null && processName.endsWith(":persistent_service");
    }
}
