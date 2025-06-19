package it.uniba.dib.sms2324.ecowateringhub.connection;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.DeviceRequest;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystem;
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
        DeviceRequest.getDeviceRequestFromServer(this.context, (jsonResponse) -> {
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
            Log.i(Common.LOG_SERVICE, "---> solve request:" + deviceRequest.getRequest());
            String requestParameter = deviceRequest.getRequest().split(DeviceRequest.REQUEST_PARAMETER_DIVISOR)[0];
            switch (requestParameter) {
                // SWITCH ON/OFF IRRIGATION SYSTEM CASE
                case DeviceRequest.REQUEST_SWITCH_ON_IRRIGATION_SYSTEM:
                case DeviceRequest.REQUEST_SWITCH_OFF_IRRIGATION_SYSTEM:
                    EcoWateringForegroundHubService.cancelIrrSysManualSchedulingWorker(this.context);
                    this.hub.getIrrigationSystem().setState(deviceRequest.getCaller(), Common.getThisDeviceID(this.context), deviceRequest.getRequest().equals(DeviceRequest.REQUEST_SWITCH_ON_IRRIGATION_SYSTEM));
                    IrrigationSystem.setScheduling(this.context, null, null, null);
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
                            EcoWateringForegroundHubService.cancelIrrSysManualSchedulingWorker(this.context);
                            IrrigationSystem.setScheduling(this.context, null, null, null);
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
                    this.hub.setIsAutomated(false, null);
                    break;

                //  SCHEDULE IRR SYS MANUALLY CASE
                case DeviceRequest.REQUEST_SCHEDULE_IRR_SYS:
                    Log.i(Common.LOG_SERVICE, "--->DeviceRequest.REQUEST_SCHEDULE_IRR_SYS: " + deviceRequest.getRequest());
                    String deviceRequestParameter = deviceRequest.getRequest().split(DeviceRequest.REQUEST_PARAMETER_DIVISOR)[1].replace("\\\"", "\"");
                    int[] startingDate = new int[3];
                    int[] startingTime = new int[2];
                    int[] irrigationDuration = new int[2];
                    try {
                        Log.i(Common.LOG_SERVICE, "--->DeviceRequest.REQUEST_SCHEDULE_IRR_SYS TRY");
                        JSONObject jsonObject = new JSONObject(deviceRequestParameter);
                        JSONArray startingDateJsonArray = jsonObject.getJSONArray(DeviceRequest.STARTING_DATE_PARAMETER);
                        startingDate[0] = startingDateJsonArray.getInt(0);
                        startingDate[1] = startingDateJsonArray.getInt(1);
                        startingDate[2] = startingDateJsonArray.getInt(2);

                        JSONArray startingTimeJsonArray = jsonObject.getJSONArray(DeviceRequest.STARTING_TIME_PARAMETER);
                        startingTime[0] = startingTimeJsonArray.getInt(0);
                        startingTime[1] = startingTimeJsonArray.getInt(1);

                        JSONArray irrigationDurationJsonArray = jsonObject.getJSONArray(DeviceRequest.IRRIGATION_DURATION_PARAMETER);
                        irrigationDuration[0] = irrigationDurationJsonArray.getInt(0);
                        irrigationDuration[1] = irrigationDurationJsonArray.getInt(1);
                        //  ENABLE IRR SYS MANUAL SCHEDULING
                        if(startingDate[0] != 0) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.YEAR, startingDate[0]);
                            calendar.set(Calendar.MONTH, startingDate[1]);
                            calendar.set(Calendar.DAY_OF_MONTH, startingDate[2]);
                            calendar.set(Calendar.HOUR_OF_DAY, startingTime[0]);
                            calendar.set(Calendar.MINUTE, startingTime[1]);
                            IrrigationSystem.setScheduling(this.context,  calendar, irrigationDuration, (response -> {
                                EcoWateringForegroundHubService.scheduleManualIrrSysWorker(this.context, calendar, irrigationDuration);
                            }));
                        }
                        else {  //  DISABLE IRR SYS MANUAL SCHEDULING
                            EcoWateringForegroundHubService.cancelIrrSysManualSchedulingWorker(this.context);
                            IrrigationSystem.setScheduling(this.context, null, null, null);
                        }
                        // DISABLE IRR SYS FOR SECURITY
                        this.hub.getIrrigationSystem().setState(deviceRequest.getCaller(), Common.getThisDeviceID(this.context), false);
                    }
                    catch (JSONException e) {
                        Log.i(Common.LOG_SERVICE, "------> DeviceRequest.REQUEST_SCHEDULE_IRR_SYS CATCH");
                        e.printStackTrace();
                    }
                    break;

                default: break;
            }
        }
    }
}
