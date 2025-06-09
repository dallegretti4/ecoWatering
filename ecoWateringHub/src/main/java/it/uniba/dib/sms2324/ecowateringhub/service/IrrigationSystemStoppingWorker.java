package it.uniba.dib.sms2324.ecowateringhub.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class IrrigationSystemStoppingWorker extends Worker {
    private final Context context;
    public IrrigationSystemStoppingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }
    @NonNull
    @Override
    public Result doWork() {
        if(HttpHelper.isDeviceConnectedToInternet(this.context))
            EcoWateringHub.getEcoWateringHubJsonString(Common.getThisDeviceID(this.context), (jsonResponse -> {
                EcoWateringHub hub = new EcoWateringHub(jsonResponse);
                if(hub.isAutomated() && hub.getIrrigationSystem().getState()) {
                    hub.getIrrigationSystem().setState(
                            Common.getThisDeviceID(this.context),
                            Common.getThisDeviceID(this.context),
                            false
                    );
                    EcoWateringForegroundHubService.scheduleIrrigationSystemStoppingWorker(this.context, hub, false); // REPEAT WORK
                }
            }));
        // NO INTERNET CONNECTION CASE
        else EcoWateringForegroundHubService.scheduleIrrigationSystemStoppingWorker(this.context, null, true);
        return Result.success();
    }
}
