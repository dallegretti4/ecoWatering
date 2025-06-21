package it.uniba.dib.sms2324.ecowateringcommon.models;

import android.content.ContentValues;
import android.content.Context;
import android.hardware.Sensor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SqlDbHelper;

public class SensorsInfo implements Parcelable {
    public static final String CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE = "ambient temperature sensor";
    public static final String CONFIGURE_SENSOR_TYPE_LIGHT = "light sensor";
    public static final String CONFIGURE_SENSOR_TYPE_RELATIVE_HUMIDITY = "relative humidity sensor";
    public static final String EW_SENSOR_ID_SEPARATOR = " - ";
    private static final long IS_SENSOR_VALUE_VALID_TOLERANCE = 5; // minute
    private ArrayList<String> ambientTemperatureSensorList;
    private String ambientTemperatureChosenSensor;
    private double ambientTemperatureSensorValue;
    private String ambientTemperatureLastUpdate;
    private ArrayList<String> lightSensorList;
    private String lightChosenSensor;
    private double lightSensorValue;
    private String lightLastUpdate;
    private ArrayList<String> relativeHumiditySensorList;
    private String relativeHumidityChosenSensor;
    private double relativeHumiditySensorValue;
    private String relativeHumidityLastUpdate;

    public SensorsInfo(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            //AMBIENT TEMPERATURE SENSOR
            this.ambientTemperatureSensorList = new ArrayList<>();
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_LIST_COLUMN_NAME)) {
                JSONArray jsonArray = new JSONArray(jsonOBJ.getString(SqlDbHelper.TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_LIST_COLUMN_NAME));
                for(int i=0; i<jsonArray.length(); i++) {
                    this.ambientTemperatureSensorList.add(jsonArray.getString(i));
                }
            }
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_CHOSEN_SENSOR_COLUMN_NAME))
                this.ambientTemperatureChosenSensor = jsonOBJ.getString(SqlDbHelper.TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_CHOSEN_SENSOR_COLUMN_NAME);
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_VALUE_COLUMN_NAME))
                this.ambientTemperatureSensorValue = jsonOBJ.getDouble(SqlDbHelper.TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_VALUE_COLUMN_NAME);
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_LAST_UPDATE_COLUMN_NAME))
                this.ambientTemperatureLastUpdate = jsonOBJ.getString(SqlDbHelper.TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_LAST_UPDATE_COLUMN_NAME);
            // LIGHT SENSOR
            this.lightSensorList = new ArrayList<>();
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_LIGHT_SENSOR_LIST_COLUMN_NAME)) {
                JSONArray jsonArray = new JSONArray(jsonOBJ.getString(SqlDbHelper.TABLE_SENSORS_INFO_LIGHT_SENSOR_LIST_COLUMN_NAME));
                for(int i=0; i<jsonArray.length(); i++) {
                    this.lightSensorList.add(jsonArray.getString(i));
                }
            }
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_LIGHT_CHOSEN_SENSOR_COLUMN_NAME))
                this.lightChosenSensor = jsonOBJ.getString(SqlDbHelper.TABLE_SENSORS_INFO_LIGHT_CHOSEN_SENSOR_COLUMN_NAME);
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_LIGHT_SENSOR_VALUE_COLUMN_NAME))
                this.lightSensorValue = jsonOBJ.getDouble(SqlDbHelper.TABLE_SENSORS_INFO_LIGHT_SENSOR_VALUE_COLUMN_NAME);
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_LIGHT_LAST_UPDATE_COLUMN_NAME))
                this.lightLastUpdate = jsonOBJ.getString(SqlDbHelper.TABLE_SENSORS_INFO_LIGHT_LAST_UPDATE_COLUMN_NAME);
            // RELATIVE HUMIDITY SENSOR
            this.relativeHumiditySensorList = new ArrayList<>();
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_LIST_COLUMN_NAME)) {
                JSONArray jsonArray = new JSONArray(jsonOBJ.getJSONArray(SqlDbHelper.TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_LIST_COLUMN_NAME));
                for(int i=0; i<jsonArray.length(); i++) {
                    this.relativeHumiditySensorList.add(jsonArray.getString(i));
                }
            }
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_CHOSEN_SENSOR_COLUMN_NAME))
                this.relativeHumidityChosenSensor = jsonOBJ.getString(SqlDbHelper.TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_CHOSEN_SENSOR_COLUMN_NAME);
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_VALUE_COLUMN_NAME))
                this.relativeHumiditySensorValue = jsonOBJ.getDouble(SqlDbHelper.TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_VALUE_COLUMN_NAME);
            if(!jsonOBJ.isNull(SqlDbHelper.TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_LAST_UPDATE_COLUMN_NAME))
                this.relativeHumidityLastUpdate = jsonOBJ.getString(SqlDbHelper.TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_LAST_UPDATE_COLUMN_NAME);
            Log.i(Common.LOG_NORMAL, "SensorsInfo -> ambTemp:" + this.ambientTemperatureSensorValue + ", light:" + this.lightSensorValue + ", relHum: " + this.relativeHumiditySensorValue);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getAmbientTemperatureSensorList() {
        return this.ambientTemperatureSensorList;
    }

    public String getAmbientTemperatureChosenSensor() {
        return this.ambientTemperatureChosenSensor;
    }

    public double getAmbientTemperatureSensorValue() {
        return this.ambientTemperatureSensorValue;
    }

    public String getAmbientTemperatureLastUpdate() {
        return this.ambientTemperatureLastUpdate;
    }

    public ArrayList<String> getLightSensorList() {
        return this.lightSensorList;
    }

    public String getLightChosenSensor() {
        return this.lightChosenSensor;
    }

    public double getLightSensorValue() {
        return this.lightSensorValue;
    }

    public String getLightLastUpdate() {
        return this.lightLastUpdate;
    }

    public ArrayList<String> getRelativeHumiditySensorList() {
        return this.relativeHumiditySensorList;
    }

    public String getRelativeHumidityChosenSensor() {
        return this.relativeHumidityChosenSensor;
    }

    public double getRelativeHumiditySensorValue() {
        return this.relativeHumiditySensorValue;
    }

    public String getRelativeHumidityLastUpdate() {
        return this.relativeHumidityLastUpdate;
    }

    public static String getSensorId(Sensor sensor) {
        return sensor.getStringType() + EW_SENSOR_ID_SEPARATOR + sensor.getName() + EW_SENSOR_ID_SEPARATOR + sensor.getVendor() + EW_SENSOR_ID_SEPARATOR + sensor.getVersion();
    }

    public boolean isLastUpdateValid(String lastUpdateTimeStamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Common.DATE_FORMAT_STRING, Locale.getDefault());
        simpleDateFormat.setLenient(false);
        try {
            Date parsedDate = simpleDateFormat.parse(lastUpdateTimeStamp);
            long currentMillis = System.currentTimeMillis();
            if(parsedDate != null) {
                long parsedMillis = parsedDate.getTime();
                long diff = currentMillis - parsedMillis;
                return ((diff >= 0) && (diff <= TimeUnit.MINUTES.toMillis(IS_SENSOR_VALUE_VALID_TOLERANCE)));
            }
            else {
                return false;
            }
        }
        catch(ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void addNewSensor(@NonNull Context context, String hubID, String sensorID, String sensorType, Common.OnStringResponseGivenCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_SENSORS_INFO_ID_COLUMN_NAME, hubID);
        contentValues.put(HttpHelper.REMOTE_DEVICE_PARAMETER, Common.getThisDeviceID(context));
        contentValues.put(HttpHelper.SENSOR_TYPE_PARAMETER, sensorType);
        contentValues.put(HttpHelper.SENSOR_ID_PARAMETER, sensorID);
        SqlDbHelper.addNewSensor(contentValues, (callback));
    }

    public static void updateSensorList(@NonNull Context context, String hubID, String sensorType, ArrayList<Sensor> sensorArrayList) {
        StringBuilder jsonSensorArrayList;
        if((sensorArrayList != null) && (!sensorArrayList.isEmpty())) {
            jsonSensorArrayList = new StringBuilder("[");
            for(int i=0; i<sensorArrayList.size(); i++) {
                if(i != (sensorArrayList.size()-1)) jsonSensorArrayList.append("\\\"").append(getSensorId(sensorArrayList.get(i))).append("\\\",");
                else jsonSensorArrayList.append("\\\"").append(getSensorId(sensorArrayList.get(i))).append("\\\"]");
            }
        }
        else
            jsonSensorArrayList = new StringBuilder(Common.NULL_STRING_VALUE);

        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_SENSORS_INFO_ID_COLUMN_NAME, hubID);
        contentValues.put(HttpHelper.REMOTE_DEVICE_PARAMETER, Common.getThisDeviceID(context));
        contentValues.put(HttpHelper.SENSOR_TYPE_PARAMETER, sensorType);
        contentValues.put(HttpHelper.VALUE_PARAMETER, jsonSensorArrayList.toString());
        SqlDbHelper.updateSensorList(contentValues);
    }

    public void detachSelectedSensor(@NonNull Context context, String hubID, String sensorType, Common.OnStringResponseGivenCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, hubID);
        contentValues.put(HttpHelper.REMOTE_DEVICE_PARAMETER, Common.getThisDeviceID(context));
        contentValues.put(HttpHelper.SENSOR_TYPE_PARAMETER, sensorType);
        SqlDbHelper.detachSelectedSensor(contentValues, (callback));
    }


    // PARCELABLE IMPLEMENTATION

    protected SensorsInfo(Parcel in) {
        ambientTemperatureSensorList = in.createStringArrayList();
        ambientTemperatureChosenSensor = in.readString();
        ambientTemperatureSensorValue = in.readDouble();
        ambientTemperatureLastUpdate = in.readString();
        lightSensorList = in.createStringArrayList();
        lightChosenSensor = in.readString();
        lightSensorValue = in.readDouble();
        lightLastUpdate = in.readString();
        relativeHumiditySensorList = in.createStringArrayList();
        relativeHumidityChosenSensor = in.readString();
        relativeHumiditySensorValue = in.readDouble();
        relativeHumidityLastUpdate = in.readString();
    }

    public static final Creator<SensorsInfo> CREATOR = new Creator<SensorsInfo>() {
        @Override
        public SensorsInfo createFromParcel(Parcel in) {
            return new SensorsInfo(in);
        }

        @Override
        public SensorsInfo[] newArray(int size) {
            return new SensorsInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(ambientTemperatureSensorList);
        dest.writeString(ambientTemperatureChosenSensor);
        dest.writeDouble(ambientTemperatureSensorValue);
        dest.writeString(ambientTemperatureLastUpdate);
        dest.writeStringList(lightSensorList);
        dest.writeString(lightChosenSensor);
        dest.writeDouble(lightSensorValue);
        dest.writeString(lightLastUpdate);
        dest.writeStringList(relativeHumiditySensorList);
        dest.writeString(relativeHumidityChosenSensor);
        dest.writeDouble(relativeHumiditySensorValue);
        dest.writeString(relativeHumidityLastUpdate);
    }
}
