package it.uniba.dib.sms2324.ecowateringhub.data;

import android.content.Context;
import android.hardware.Sensor;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SqlDbHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsInfo;

public class RelativeHumiditySensor extends EcoWateringSensor {
    public RelativeHumiditySensor(@NonNull Context context, String sensorID) {
        super(context, sensorID);
    }

    @Override
    protected void updateSensorValueOnServerDb(@NonNull Context context) {
        String jsonString = "{\"" +
                SqlDbHelper.TABLE_SENSORS_INFO_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_UPDATE_RELATIVE_HUMIDITY_SENSOR + "\",\"" +
                HttpHelper.VALUE_PARAMETER + "\":" + this.currentValue + "}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(jsonString);
            Log.i(LOG_SENSOR, "updateSensorRelHumValueOnServerDb response: " + response + " - value: " + this.currentValue);
        }).start();
    }

    @Override
    protected Sensor recoverSensorFromSensorID(@NonNull Context context) {
        String[] sensorInfo = this.sensorID.split(SensorsInfo.EW_SENSOR_ID_SEPARATOR);
        ArrayList<Sensor> deviceSensorsList = getConnectedSensorList(context, SensorsInfo.CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY);
        if(!deviceSensorsList.isEmpty()) {
            for(Sensor sensor : deviceSensorsList) {
                if((sensor.getStringType().equals(sensorInfo[0])) &&
                        (sensor.getName().equals(sensorInfo[1])) &&
                        (sensor.getVendor().equals(sensorInfo[2])) &&
                        (String.valueOf(sensor.getVersion()).equals(sensorInfo[3]))) {
                    return sensor;
                }
            }
        }
        return null;
    }
}
