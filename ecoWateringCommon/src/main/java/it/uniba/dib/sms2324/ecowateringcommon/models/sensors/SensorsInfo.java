package it.uniba.dib.sms2324.ecowateringcommon.models.sensors;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import it.uniba.dib.sms2324.ecowateringcommon.Common;

public class SensorsInfo implements Parcelable {
    public static final String BO_SENSORS_INFO_OBJ_NAME = "sensorsInfo";
    private static final String TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME = "ambientTemperatureSensor";
    private static final String TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_LAST_UPDATE_COLUMN_NAME = "ambientTemperatureLastUpdate";
    private static final String TABLE_SENSORS_INFO_LIGHT_SENSOR_COLUMN_NAME = "lightSensor";
    private static final String TABLE_SENSORS_INFO_LIGHT_LAST_UPDATE_COLUMN_NAME = "lightLastUpdate";
    private static final String TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME = "relativeHumiditySensor";
    private static final String TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_LAST_UPDATE_COLUMN_NAME = "relativeHumidityLastUpdate";
    private static final long IS_SENSOR_VALUE_VALID_TOLERANCE = 5; // minute
    private double ambientTemperatureSensor;
    private String ambientTemperatureLastUpdate;
    private double lightSensor;
    private String lightLastUpdate;
    private double relativeHumiditySensor;
    private String relativeHumidityLastUpdate;

    public SensorsInfo(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            if(!jsonOBJ.isNull(TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME)) {
                this.ambientTemperatureSensor = jsonOBJ.getDouble(TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME);
            }
            if(!jsonOBJ.isNull(TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_LAST_UPDATE_COLUMN_NAME)) {
                this.ambientTemperatureLastUpdate = jsonOBJ.getString(TABLE_SENSORS_INFO_AMBIENT_TEMPERATURE_LAST_UPDATE_COLUMN_NAME);
            }
            if(!jsonOBJ.isNull(TABLE_SENSORS_INFO_LIGHT_SENSOR_COLUMN_NAME)) {
                this.lightSensor = jsonOBJ.getDouble(TABLE_SENSORS_INFO_LIGHT_SENSOR_COLUMN_NAME);
            }
            if(!jsonOBJ.isNull(TABLE_SENSORS_INFO_LIGHT_LAST_UPDATE_COLUMN_NAME)) {
                this.lightLastUpdate = jsonOBJ.getString(TABLE_SENSORS_INFO_LIGHT_LAST_UPDATE_COLUMN_NAME);
            }
            if(!jsonOBJ.isNull(TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME)) {
                this.relativeHumiditySensor = jsonOBJ.getDouble(TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME);
            }
            if(!jsonOBJ.isNull(TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_LAST_UPDATE_COLUMN_NAME)) {
                this.relativeHumidityLastUpdate = jsonOBJ.getString(TABLE_SENSORS_INFO_RELATIVE_HUMIDITY_LAST_UPDATE_COLUMN_NAME);
            }
            Log.i(Common.THIS_LOG, "SensorsInfo -> ambTemp:" + this.ambientTemperatureSensor + ", light:" + this.lightSensor + ", relHum: " + this.relativeHumiditySensor);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public double getAmbientTemperatureSensor() {
        return this.ambientTemperatureSensor;
    }

    public String getAmbientTemperatureLastUpdate() {
        return this.ambientTemperatureLastUpdate;
    }

    public double getLightSensor() {
        return this.lightSensor;
    }

    public String getLightLastUpdate() {
        return this.lightLastUpdate;
    }

    public double getRelativeHumiditySensor() {
        return this.relativeHumiditySensor;
    }

    public String getRelativeHumidityLastUpdate() {
        return this.relativeHumidityLastUpdate;
    }

    public boolean isLastUpdateValid(String lastUpdateTimeStamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
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

    // PARCELABLE IMPLEMENTATION
    protected SensorsInfo(Parcel in) {
        ambientTemperatureSensor = in.readDouble();
        ambientTemperatureLastUpdate = in.readString();
        lightSensor = in.readDouble();
        lightLastUpdate = in.readString();
        relativeHumiditySensor = in.readDouble();
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
        dest.writeDouble(ambientTemperatureSensor);
        dest.writeString(ambientTemperatureLastUpdate);
        dest.writeDouble(lightSensor);
        dest.writeString(lightLastUpdate);
        dest.writeDouble(relativeHumiditySensor);
        dest.writeString(relativeHumidityLastUpdate);
    }
}
