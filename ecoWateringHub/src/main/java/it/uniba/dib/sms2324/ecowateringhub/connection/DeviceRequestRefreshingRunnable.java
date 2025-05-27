package it.uniba.dib.sms2324.ecowateringhub.connection;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.DeviceRequest;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.service.EcoWateringForegroundService;

public class DeviceRequestRefreshingRunnable implements Runnable {
    private final Context context;
    private final EcoWateringHub hub;

    public DeviceRequestRefreshingRunnable(@NonNull Context context, @NonNull EcoWateringHub hub) {
        this.context = context;
        this.hub = hub;
    }

    @Override
    public void run() {
        DeviceRequest.getDeviceRequestFromServer(Common.getThisDeviceID(context), (jsonResponse) -> {
            if(jsonResponse != null) {
                ArrayList<DeviceRequest> deviceRequestList = DeviceRequest.getDeviceRequestList(jsonResponse);
                if((deviceRequestList != null) && (!deviceRequestList.isEmpty())) {
                    for(DeviceRequest deviceRequest : deviceRequestList) {
                        new Thread(() -> solveDeviceRequest(context, this.hub, deviceRequest)).start();
                    }
                }
            }
        });

    }

    private void solveDeviceRequest(@NonNull Context context, @NonNull EcoWateringHub hub, DeviceRequest deviceRequest) {
        deviceRequest.delete(); // FOR FIRST, TO BE SURE, NEXT DeviceRequestsRefreshingRunnable CAN'T FIND THIS REQUEST
        // CHECK IS DEVICE REQUEST VALID
        if(deviceRequest.isValidDeviceRequest()) {
            switch (deviceRequest.getRequest()) {
                // SWITCH ON IRRIGATION SYSTEM CASE
                case DeviceRequest.REQUEST_SWITCH_ON_IRRIGATION_SYSTEM:
                    hub.getIrrigationSystem().setState(deviceRequest.getCaller(), Common.getThisDeviceID(context), true);
                    break;
                // SWITCH OFF IRRIGATION SYSTEM CASE
                case DeviceRequest.REQUEST_SWITCH_OFF_IRRIGATION_SYSTEM:
                    hub.getIrrigationSystem().setState(deviceRequest.getCaller(), Common.getThisDeviceID(context), false);
                    break;
                // START BACKGROUND REFRESHING CASE
                case DeviceRequest.REQUEST_START_DATA_OBJECT_REFRESHING:
                    hub.setIsDataObjectRefreshing(context, true, (response) -> {
                        if (response.equals(EcoWateringHub.SET_IS_DATA_OBJECT_REFRESHING_SUCCESS_RESPONSE)) {
                            EcoWateringForegroundService.startEcoWateringForegroundService(this.context, this.hub);
                        }
                    });
                    break;
                // STOP BACKGROUND REFRESHING
                case DeviceRequest.REQUEST_STOP_DATA_OBJECT_REFRESHING:
                    hub.setIsDataObjectRefreshing(context, false, (response) -> {
                        if ((response.equals(EcoWateringHub.SET_IS_DATA_OBJECT_REFRESHING_SUCCESS_RESPONSE)) &&
                                ((this.hub.getRemoteDeviceList() == null) || this.hub.getRemoteDeviceList().isEmpty())) {
                            EcoWateringForegroundService.stopEcoWateringForegroundService(this.context);
                        }
                    });
                    break;

                default: break;
            }
        }
    }
}
