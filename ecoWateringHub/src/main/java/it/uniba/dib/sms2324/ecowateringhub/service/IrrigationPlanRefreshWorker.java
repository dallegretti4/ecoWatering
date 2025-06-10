package it.uniba.dib.sms2324.ecowateringhub.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlanPreview;

public class IrrigationPlanRefreshWorker extends Worker {
    private final Context context;
    public IrrigationPlanRefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        if(HttpHelper.isDeviceConnectedToInternet(this.context)) {
            EcoWateringHub.getEcoWateringHub(Common.getThisDeviceID(this.context), (jsonHub -> {
                EcoWateringHub hub = new EcoWateringHub(jsonHub);
                if(hub.isAutomated()) {
                    IrrigationPlanPreview.getIrrigationPlanPreview(hub, Common.getThisDeviceID(this.context), (jsonResponse -> {
                        IrrigationPlanPreview irrigationPlanPreview = new IrrigationPlanPreview(jsonResponse);
                        irrigationPlanPreview.updateIrrigationPlanOnServer(this.context, Common.getThisDeviceID(this.context));
                    }));
                    EcoWateringForegroundHubService.scheduleIrrigationPlanRefreshingWorker(this.context, false);   // REPEAT WORK
                }
            }));
        }
        else {
            EcoWateringForegroundHubService.scheduleIrrigationPlanRefreshingWorker(this.context, true);
        }
        return Result.success();
    }
}
