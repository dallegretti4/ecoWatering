package it.uniba.dib.sms2324.ecowateringhub.service;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.Set;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SharedPreferencesHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystem;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystemScheduling;

public class IrrigationSystemManualSetWorker extends Worker {
    private final Context context;

    public IrrigationSystemManualSetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(Common.LOG_SERVICE, "-------> doWork IrrigationSystemManualSetWorker");
        Set<String> tags = getTags();
        if(HttpHelper.isDeviceConnectedToInternet(this.context)) {
            //  NORMAL EXECUTION
            EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(context), (jsonResponse -> {
                EcoWateringHub hub = new EcoWateringHub(jsonResponse);
                if(hub.getIrrigationSystem().getIrrigationSystemScheduling() != null) {
                    hub.getIrrigationSystem().setState(Common.getThisDeviceID(context), Common.getThisDeviceID(context), tags.contains(EcoWateringForegroundHubService.TAG_IRRIGATION_SYSTEM_MANUAL_START));
                    //  SWITCH OFF CASE
                    if(tags.contains(EcoWateringForegroundHubService.TAG_IRRIGATION_SYSTEM_MANUAL_STOP))
                        IrrigationSystem.setScheduling(this.context, null, null);
                    //  SWITCH ON CASE
                    else {
                        SharedPreferencesHelper.writeIntOnSharedPreferences(this.context, SharedPreferencesHelper.IRR_SYS_MANUAL_SCHEDULING_FILENAME, String.valueOf(Calendar.HOUR_OF_DAY), 0);
                        SharedPreferencesHelper.writeIntOnSharedPreferences(this.context, SharedPreferencesHelper.IRR_SYS_MANUAL_SCHEDULING_FILENAME, String.valueOf(Calendar.MINUTE), 0);
                    }
                }
            }));
        }
        else {  // NO INTERNET CONNECTION CASE
            if(tags.contains(EcoWateringForegroundHubService.TAG_IRRIGATION_SYSTEM_MANUAL_STOP))
                //  STOP WORKER POST DELAYED
                EcoWateringForegroundHubService.sendOneTimeWorkRequest(
                        this.context,
                        (30 * 1000),
                        EcoWateringForegroundHubService.TAG_IRRIGATION_SYSTEM_MANUAL_STOP,
                        EcoWateringForegroundHubService.NAME_IRRIGATION_SYSTEM_MANUAL_STOP
                );
            else {  // START WORKER POST DELAYED
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.add(Calendar.MINUTE, 1);
                int[] startingDate = {calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH)};
                int[] startingTime = {calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)};
                int[] irrigationDuration = {
                        SharedPreferencesHelper.readIntFromSharedPreferences(this.context, SharedPreferencesHelper.IRR_SYS_MANUAL_SCHEDULING_FILENAME, String.valueOf(Calendar.HOUR_OF_DAY)),
                        SharedPreferencesHelper.readIntFromSharedPreferences(this.context, SharedPreferencesHelper.IRR_SYS_MANUAL_SCHEDULING_FILENAME, String.valueOf(Calendar.MINUTE))
                };
                Bundle b = new Bundle();
                b.putIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_STARTING_DATE, startingDate);
                b.putIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_STARTING_TIME, startingTime);
                b.putIntArray(IrrigationSystemScheduling.BO_IRR_SYS_SCHEDULING_IRRIGATION_DURATION, irrigationDuration);
                EcoWateringForegroundHubService.scheduleManualIrrSysWorker(this.context, b);
            }
        }
        return Result.success();
    }
}
