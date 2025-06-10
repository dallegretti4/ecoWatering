package it.uniba.dib.sms2324.ecowateringhub.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Set;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class IrrigationSystemManualSetWorker extends Worker {
    private final Context context;
    private final Set<String> tags;

    public IrrigationSystemManualSetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.tags = params.getTags();
    }

    @NonNull
    @Override
    public Result doWork() {
        EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this.context), (jsonResponse -> {
            EcoWateringHub hub = new EcoWateringHub(jsonResponse);
            if(this.tags.contains(EcoWateringForegroundHubService.TAG_IRRIGATION_SYSTEM_MANUAL_START))
                hub.getIrrigationSystem().setState(Common.getThisDeviceID(this.context), Common.getThisDeviceID(this.context), true);
            else
                hub.getIrrigationSystem().setState(Common.getThisDeviceID(this.context), Common.getThisDeviceID(this.context), false);
        }));
        return Result.success();
    }
}
