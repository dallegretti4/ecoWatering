package it.uniba.dib.sms2324.ecowateringhub.connection;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.DeviceRequest;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.service.EcoWateringForegroundHubService;

public class DeviceRequestRefreshingRunnable implements Runnable {
    private final Context context;
    private final EcoWateringHub hub;

    public DeviceRequestRefreshingRunnable(@NonNull Context context, @NonNull EcoWateringHub hub) {
        this.context = context;
        this.hub = hub;
    }

    @Override
    public void run() {
        DeviceRequest.getDeviceRequestFromServer(Common.getThisDeviceID(this.context), (jsonResponse) -> {
            if(jsonResponse != null) {
                ArrayList<DeviceRequest> deviceRequestList = DeviceRequest.getDeviceRequestList(jsonResponse);
                if((deviceRequestList != null) && (!deviceRequestList.isEmpty())) {
                    for(DeviceRequest deviceRequest : deviceRequestList) {
                        new Thread(() -> solveDeviceRequest(deviceRequest)).start();
                    }
                }
            }
        });

    }

    private void solveDeviceRequest(DeviceRequest deviceRequest) {
        deviceRequest.delete(); // FOR FIRST, TO BE SURE, NEXT DeviceRequestsRefreshingRunnable CAN'T FIND THIS REQUEST
        // CHECK IS DEVICE REQUEST VALID
        if(deviceRequest.isValidDeviceRequest()) {
            switch (deviceRequest.getRequest()) {
                // SWITCH ON IRRIGATION SYSTEM CASE
                case DeviceRequest.REQUEST_SWITCH_ON_IRRIGATION_SYSTEM:
                    this.hub.getIrrigationSystem().setState(deviceRequest.getCaller(), Common.getThisDeviceID(this.context), true);
                    break;
                // SWITCH OFF IRRIGATION SYSTEM CASE
                case DeviceRequest.REQUEST_SWITCH_OFF_IRRIGATION_SYSTEM:
                    this.hub.getIrrigationSystem().setState(deviceRequest.getCaller(), Common.getThisDeviceID(this.context), false);
                    break;
                // START BACKGROUND REFRESHING CASE
                case DeviceRequest.REQUEST_START_DATA_OBJECT_REFRESHING:
                    this.hub.setIsDataObjectRefreshing(this.context, true, (response) -> {
                        if (response.equals(EcoWateringHub.SET_IS_DATA_OBJECT_REFRESHING_SUCCESS_RESPONSE))
                            EcoWateringForegroundHubService.checkEcoWateringForegroundServiceNeedToBeStarted(this.context, this.hub);
                    });
                    break;
                // STOP BACKGROUND REFRESHING CASE
                case DeviceRequest.REQUEST_STOP_DATA_OBJECT_REFRESHING:
                    Log.i(Common.LOG_NORMAL, "------------> caseeeee");
                    this.hub.setIsDataObjectRefreshing(this.context, false, (response) -> {
                        if ((response.equals(EcoWateringHub.SET_IS_DATA_OBJECT_REFRESHING_SUCCESS_RESPONSE)) &&
                                ((this.hub.getRemoteDeviceList() == null) || this.hub.getRemoteDeviceList().isEmpty()))
                            EcoWateringForegroundHubService.checkEcoWateringForegroundServiceNeedToBeStarted(this.context, this.hub);
                    });
                    break;
                //  ENABLE AUTOMATE SYSTEM CASE
                case DeviceRequest.REQUEST_ENABLE_AUTOMATE_SYSTEM:
                    this.hub.setIsAutomated(true, (response -> {
                        if(response.equals(EcoWateringHub.SET_IS_AUTOMATED_SUCCESS_RESPONSE)) {
                            if(this.hub.getIrrigationPlan() != null) EcoWateringForegroundHubService.checkEcoWateringSystemNeedToBeAutomated(this.context, this.hub);
                            else {
                                EcoWateringHub.getEcoWateringHub(this.hub.getDeviceID(), (jsonResponse -> {
                                    EcoWateringHub tmpHub = new EcoWateringHub(jsonResponse);
                                    EcoWateringForegroundHubService.checkEcoWateringSystemNeedToBeAutomated(this.context, tmpHub);
                                }));
                            }
                        }
                    }));
                    break;
                //  DISABLE AUTOMATE SYSTEM CASE
                case DeviceRequest.REQUEST_DISABLE_AUTOMATE_SYSTEM:
                    this.hub.setIsAutomated(false, (response -> {}));
                    break;

                default: break;
            }
        }
    }
}
