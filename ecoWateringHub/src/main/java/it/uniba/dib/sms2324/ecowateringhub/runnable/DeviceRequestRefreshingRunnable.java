package it.uniba.dib.sms2324.ecowateringhub.runnable;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.DeviceRequest;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.service.EcoWateringForegroundService;

public class DeviceRequestRefreshingRunnable implements Runnable {
    private static final int REQUEST_REFRESHING_REPEAT_INTERVAL = 2 * 1000;
    private static boolean deviceRequestsSolvedFlag = false;
    private final Context context;
    private final EcoWateringHub hub;

    public DeviceRequestRefreshingRunnable(@NonNull Context context, @NonNull EcoWateringHub hub) {
        this.context = context;
        this.hub = hub;
    }

    @Override
    public void run() {
        while (EcoWateringForegroundService.isRequestRefreshingServiceRunning) {
            deviceRequestsSolvedFlag = false;
            DeviceRequest.getDeviceRequestFromServer(Common.getThisDeviceID(context), (jsonResponse) -> {
                if(jsonResponse != null) {
                    ArrayList<DeviceRequest> deviceRequestList = DeviceRequest.getDeviceRequestList(jsonResponse);
                    if((deviceRequestList != null) && (!deviceRequestList.isEmpty())) {
                        for(DeviceRequest deviceRequest : deviceRequestList) {
                            new Thread(() -> solveDeviceRequest(context, this.hub, deviceRequest)).start();
                        }
                    }
                }
                deviceRequestsSolvedFlag = true;
            });
            while(!deviceRequestsSolvedFlag) {
                try { Thread.sleep(REQUEST_REFRESHING_REPEAT_INTERVAL); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
            try { Thread.sleep(REQUEST_REFRESHING_REPEAT_INTERVAL / 2); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private void solveDeviceRequest(@NonNull Context context, @NonNull EcoWateringHub hub, DeviceRequest deviceRequest) {
        // CHECK IS DEVICE REQUEST VALID
        if(!deviceRequest.isValidDeviceRequest()) {
            deviceRequest.delete();
        }
        // START BACKGROUND REFRESHING CASE
        else if(deviceRequest.getRequest().equals(DeviceRequest.REQUEST_START_DATA_OBJECT_REFRESHING)) {
            hub.getEcoWateringHubConfiguration().setIsDataObjectRefreshing(context, true, (response) -> {
                if(response.equals(EcoWateringForegroundService.SUCCESS_RESPONSE_SET_IS_DATA_OBJECT_REFRESHING)) {
                    EcoWateringForegroundService.dataObjectRefreshingThread = new Thread(new DataObjectRefreshingRunnable(this.context, this.hub));
                    EcoWateringForegroundService.dataObjectRefreshingThread.start();
                    deviceRequest.delete();
                }
            });
        }
        // STOP BACKGROUND REFRESHING
        else if(deviceRequest.getRequest().equals(DeviceRequest.REQUEST_STOP_DATA_OBJECT_REFRESHING)) {
            hub.getEcoWateringHubConfiguration().setIsDataObjectRefreshing(context, false, (response) -> {
                if(response.equals(EcoWateringForegroundService.SUCCESS_RESPONSE_SET_IS_DATA_OBJECT_REFRESHING)) {
                    EcoWateringForegroundService.dataObjectRefreshingThread.interrupt();
                    deviceRequest.delete();
                }
            });
        }
    }
}
