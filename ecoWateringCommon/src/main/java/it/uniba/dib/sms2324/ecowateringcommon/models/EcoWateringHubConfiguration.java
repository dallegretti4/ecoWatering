package it.uniba.dib.sms2324.ecowateringcommon.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.AmbientTemperatureSensor;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.LightSensor;
import it.uniba.dib.sms2324.ecowateringcommon.models.sensors.RelativeHumiditySensor;

public class EcoWateringHubConfiguration implements Parcelable {
    private static final String BO_IRRIGATION_SYSTEM_COLUMN_NAME = "irrigationSystem";
    private static final String IS_AUTOMATED_TRUE_VALUE = "1";
    private static final String IS_DATA_OBJECT_REFRESHING_TRUE_VALUE = "1";
    public static final String TABLE_CONFIGURATION_IS_AUTOMATED_COLUMN_NAME = "isAutomated";
    public static final String TABLE_CONFIGURATION_IS_DATA_OBJECT_REFRESHING_COLUMN_NAME = "isDataObjectRefreshing";
    public static final String TABLE_CONFIGURATION_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME = "ambientTemperatureSensor";
    public static final String TABLE_CONFIGURATION_LIGHT_SENSOR_COLUMN_NAME = "lightSensor";
    public static final String TABLE_CONFIGURATION_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME = "relativeHumiditySensor";
    private boolean isAutomated;
    private boolean isDataObjectRefreshing;
    private IrrigationSystem irrigationSystem;
    private AmbientTemperatureSensor ambientTemperatureSensor;
    private LightSensor lightSensor;
    private RelativeHumiditySensor relativeHumiditySensor;

    public EcoWateringHubConfiguration(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            // IS AUTOMATED RECOVERING
            this.isAutomated = jsonOBJ.getString(TABLE_CONFIGURATION_IS_AUTOMATED_COLUMN_NAME).equals(IS_AUTOMATED_TRUE_VALUE);
            // IS BACKGROUND REFRESHING RECOVERING
            this.isDataObjectRefreshing = jsonOBJ.getString(TABLE_CONFIGURATION_IS_DATA_OBJECT_REFRESHING_COLUMN_NAME).equals(IS_DATA_OBJECT_REFRESHING_TRUE_VALUE);
            // IRRIGATION SYSTEM RECOVERING
            if(!jsonOBJ.getString(BO_IRRIGATION_SYSTEM_COLUMN_NAME).equals(Common.NULL_STRING_VALUE)) {
                this.irrigationSystem = new IrrigationSystem(jsonOBJ.getString(BO_IRRIGATION_SYSTEM_COLUMN_NAME));
            }
            // AMBIENT TEMPERATURE SENSOR RECOVERY
            if((!jsonOBJ.getString(TABLE_CONFIGURATION_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME).equals(Common.VOID_STRING_VALUE)) &&
                    (!jsonOBJ.getString(TABLE_CONFIGURATION_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME).equals(Common.NULL_STRING_VALUE))) {
                this.ambientTemperatureSensor = new AmbientTemperatureSensor(jsonOBJ.getString(TABLE_CONFIGURATION_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME));
            }
            // LIGHT SENSOR RECOVERY
            if((!jsonOBJ.getString(TABLE_CONFIGURATION_LIGHT_SENSOR_COLUMN_NAME).equals(Common.VOID_STRING_VALUE)) &&
                    (!jsonOBJ.getString(TABLE_CONFIGURATION_LIGHT_SENSOR_COLUMN_NAME).equals(Common.NULL_STRING_VALUE))) {
                this.lightSensor = new LightSensor(jsonOBJ.getString(TABLE_CONFIGURATION_LIGHT_SENSOR_COLUMN_NAME));
            }
            // RELATIVE HUMIDITY SENSOR RECOVERY
            if((!jsonOBJ.getString(TABLE_CONFIGURATION_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME).equals(Common.VOID_STRING_VALUE)) &&
                    (!jsonOBJ.getString(TABLE_CONFIGURATION_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME).equals(Common.NULL_STRING_VALUE))) {
                this.relativeHumiditySensor = new RelativeHumiditySensor(jsonOBJ.getString(TABLE_CONFIGURATION_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME));
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isAutomated() {
        return isAutomated;
    }
    public boolean isDataObjectRefreshing() {
        return this.isDataObjectRefreshing;
    }
    public void setIsDataObjectRefreshing(@NonNull Context context, boolean value, Common.OnStringResponseGivenCallback callback) {// CONVERT STATE
        int valueInt = 0;
        if(value) {
            valueInt = 1;
        }
        String jsonString = "{\"" +
                EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_SET_IS_DATA_OBJECT_REFRESHING + "\",\"" +
                HttpHelper.VALUE_PARAMETER + "\":\"" + valueInt + "\"}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "setIsDataObjectRefreshing response: " + response);
            callback.getResponse(response);
        }).start();
    }

    public IrrigationSystem getIrrigationSystem() {
        return this.irrigationSystem;
    }

    public AmbientTemperatureSensor getAmbientTemperatureSensor() {
        return this.ambientTemperatureSensor;
    }

    public LightSensor getLightSensor() {
        return this.lightSensor;
    }

    public RelativeHumiditySensor getRelativeHumiditySensor() {
        return this.relativeHumiditySensor;
    }

    // PARCELABLE IMPLEMENTATION
    protected EcoWateringHubConfiguration(Parcel in) {
        isAutomated = in.readByte() != 0;
        isDataObjectRefreshing = in.readByte() != 0;
        irrigationSystem = in.readParcelable(IrrigationSystem.class.getClassLoader());
        ambientTemperatureSensor = in.readParcelable(AmbientTemperatureSensor.class.getClassLoader());
        lightSensor = in.readParcelable(LightSensor.class.getClassLoader());
        relativeHumiditySensor = in.readParcelable(RelativeHumiditySensor.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isAutomated ? 1 : 0));
        dest.writeByte((byte) (isDataObjectRefreshing ? 1 : 0));
        dest.writeParcelable(irrigationSystem, flags);
        dest.writeParcelable(ambientTemperatureSensor, flags);
        dest.writeParcelable(lightSensor, flags);
        dest.writeParcelable(relativeHumiditySensor, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EcoWateringHubConfiguration> CREATOR = new Creator<EcoWateringHubConfiguration>() {
        @Override
        public EcoWateringHubConfiguration createFromParcel(Parcel in) {
            return new EcoWateringHubConfiguration(in);
        }

        @Override
        public EcoWateringHubConfiguration[] newArray(int size) {
            return new EcoWateringHubConfiguration[size];
        }
    };
}
