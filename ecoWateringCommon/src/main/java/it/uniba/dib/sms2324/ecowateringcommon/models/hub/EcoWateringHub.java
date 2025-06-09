package it.uniba.dib.sms2324.ecowateringcommon.models.hub;

import android.content.ContentValues;
import android.content.Context;
import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SqlDbHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.device.EcoWateringDevice;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.IrrigationSystem;
import it.uniba.dib.sms2324.ecowateringcommon.models.WeatherInfo;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlan;
import it.uniba.dib.sms2324.ecowateringcommon.models.SensorsInfo;

public class EcoWateringHub implements Parcelable {
    public interface OnEcoWateringHubGivenCallback {
        void getEcoWateringHub(@NonNull EcoWateringHub ecoWateringHub);
    }
    private static final String IS_AUTOMATED_TRUE_VALUE = "1";
    private static final String IS_DATA_OBJECT_REFRESHING_TRUE_VALUE = "1";
    public static final String SET_IS_AUTOMATED_SUCCESS_RESPONSE = "ecoWateringHubIsAutomateSet";
    public static final String SET_IS_DATA_OBJECT_REFRESHING_SUCCESS_RESPONSE = "isDataObjectRefreshingSet";
    private static final String BO_IRRIGATION_SYSTEM_COLUMN_NAME = "irrigationSystem";
    public static final String HUB_NAME_CHANGED_RESPONSE = "hubNameSuccessfulChanged";
    public static final String DEVICE_HUB_ACCOUNT_RESPONSE = "hubAccountSuccessfulDeleted";
    public static final String TABLE_HUB_DEVICE_ID_COLUMN_NAME = "deviceID";
    private static final String TABLE_HUB_NAME_COLUMN_NAME = "name";
    private static final String TABLE_HUB_ADDRESS_COLUMN_NAME = "address";
    private static final String TABLE_HUB_CITY_COLUMN_NAME = "city";
    private static final String TABLE_HUB_COUNTRY_COLUMN_NAME = "country";
    public static final String TABLE_HUB_LATITUDE_COLUMN_NAME = "latitude";
    public static final String TABLE_HUB_LONGITUDE_COLUMN_NAME = "longitude";
    private static final String TABLE_HUB_REMOTE_DEVICE_LIST_COLUMN_NAME = "remoteDeviceList";
    public static final String TABLE_HUB_IS_AUTOMATED_COLUMN_NAME = "isAutomated";
    public static final String TABLE_HUB_IS_DATA_OBJECT_REFRESHING_COLUMN_NAME = "isDataObjectRefreshing";
    public static final String TABLE_HUB_BATTERY_PERCENT_COLUMN_NAME = "batteryPercent";
    private String deviceID;
    private String name;
    private String address;
    private String city;
    private String country;
    private double latitude;
    private double longitude;
    private List<String> remoteDeviceList;
    private boolean isAutomated;
    private boolean isDataObjectRefreshing;
    private int batteryPercent;

    // NOT ON DATABASE
    private IrrigationSystem irrigationSystem;
    private WeatherInfo weatherInfo;
    private SensorsInfo sensorInfo;
    private IrrigationPlan irrigationPlan;

    // CONSTRUCTOR
    public EcoWateringHub(String jsonString) {
        try{
            JSONObject jsonOBJ = new JSONObject(jsonString);
            this.deviceID = jsonOBJ.getString(EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME);
            this.name = jsonOBJ.getString(EcoWateringHub.TABLE_HUB_NAME_COLUMN_NAME);
            this.address = jsonOBJ.getString(EcoWateringHub.TABLE_HUB_ADDRESS_COLUMN_NAME);
            this.city = jsonOBJ.getString(EcoWateringHub.TABLE_HUB_CITY_COLUMN_NAME);
            this.country = jsonOBJ.getString(EcoWateringHub.TABLE_HUB_COUNTRY_COLUMN_NAME);
            this.latitude = jsonOBJ.getDouble(EcoWateringHub.TABLE_HUB_LATITUDE_COLUMN_NAME);
            this.longitude = jsonOBJ.getDouble(EcoWateringHub.TABLE_HUB_LONGITUDE_COLUMN_NAME);
            // REMOTE DEVICE LIST RECOVERING
            this.remoteDeviceList = new ArrayList<>();
            if(!jsonOBJ.getString(EcoWateringHub.TABLE_HUB_REMOTE_DEVICE_LIST_COLUMN_NAME).equals(Common.NULL_STRING_VALUE)) {
                JSONArray jsonRemoteDeviceList = new JSONArray(jsonOBJ.getString(EcoWateringHub.TABLE_HUB_REMOTE_DEVICE_LIST_COLUMN_NAME));
                for(int i=0; i<jsonRemoteDeviceList.length(); i++) {
                    this.remoteDeviceList.add(jsonRemoteDeviceList.getString(i));
                }
            }
            this.isAutomated = jsonOBJ.getString(TABLE_HUB_IS_AUTOMATED_COLUMN_NAME).equals(IS_AUTOMATED_TRUE_VALUE);
            this.isDataObjectRefreshing = jsonOBJ.getString(TABLE_HUB_IS_DATA_OBJECT_REFRESHING_COLUMN_NAME).equals(IS_DATA_OBJECT_REFRESHING_TRUE_VALUE);
            this.batteryPercent = jsonOBJ.getInt(TABLE_HUB_BATTERY_PERCENT_COLUMN_NAME);
            // IRRIGATION SYSTEM RECOVERING
            if(!jsonOBJ.getString(BO_IRRIGATION_SYSTEM_COLUMN_NAME).equals(Common.NULL_STRING_VALUE))
                this.irrigationSystem = new IrrigationSystem(jsonOBJ.getString(BO_IRRIGATION_SYSTEM_COLUMN_NAME));
            // WEATHER INFO RECOVERING
            if(!jsonOBJ.getString(WeatherInfo.BO_WEATHER_INFO_OBJ_NAME).equals(Common.NULL_STRING_VALUE))
                this.weatherInfo = new WeatherInfo(jsonOBJ.getString(WeatherInfo.BO_WEATHER_INFO_OBJ_NAME));
            // SENSORS INFO RECOVERING
            if(!jsonOBJ.getString(SensorsInfo.BO_SENSORS_INFO_OBJ_NAME).equals(Common.NULL_STRING_VALUE))
                this.sensorInfo = new SensorsInfo(jsonOBJ.getString(SensorsInfo.BO_SENSORS_INFO_OBJ_NAME));
            // IRRIGATION PLAN RECOVERING
            if(!jsonOBJ.getString(IrrigationPlan.BO_IRRIGATION_PLAN_COLUMN_NAME).equals(Common.NULL_STRING_VALUE))
                this.irrigationPlan = new IrrigationPlan(jsonOBJ.getString(IrrigationPlan.BO_IRRIGATION_PLAN_COLUMN_NAME));
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public static void addNewEcoWateringHub(@NonNull Context context, @NonNull String hubName, @NonNull Address address, @NonNull IrrigationSystem irrigationSystem, Common.OnMethodFinishCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, Common.getThisDeviceID(context));
        contentValues.put(SqlDbHelper.TABLE_HUB_NAME_COLUMN_NAME, hubName);
        contentValues.put(SqlDbHelper.TABLE_HUB_ADDRESS_COLUMN_NAME, address.getThoroughfare());
        contentValues.put(SqlDbHelper.TABLE_HUB_CITY_COLUMN_NAME, address.getLocality());
        contentValues.put(SqlDbHelper.TABLE_HUB_COUNTRY_COLUMN_NAME, address.getCountryName());
        contentValues.put(SqlDbHelper.TABLE_HUB_LATITUDE_COLUMN_NAME, address.getLatitude());
        contentValues.put(SqlDbHelper.TABLE_HUB_LONGITUDE_COLUMN_NAME, address.getLongitude());
        contentValues.put(IrrigationSystem.TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME, irrigationSystem.getModel());
        SqlDbHelper.addNewEcoWateringHub(contentValues, (callback));
    }

    public static void getEcoWateringHub(String hubID, Common.OnStringResponseGivenCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, hubID);
        SqlDbHelper.getEcoWateringHub(contentValues, (callback));
    }

    public String addNewRemoteDevice(@NonNull Context context, @NonNull String remoteDeviceID) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, Common.getThisDeviceID(context));
        contentValues.put(HttpHelper.REMOTE_DEVICE_PARAMETER, remoteDeviceID);
        return SqlDbHelper.addNewRemoteDevice(contentValues);
    }

    public static void removeRemoteDevice(@NonNull String deviceID, @NonNull EcoWateringDevice remoteDevice, Common.OnStringResponseGivenCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, deviceID);
        contentValues.put(HttpHelper.REMOTE_DEVICE_PARAMETER, remoteDevice.getDeviceID());
        SqlDbHelper.removeRemoteDevice(contentValues, (callback));
    }

    public static void setName(@NonNull Context context, String newName, Common.OnStringResponseGivenCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, Common.getThisDeviceID(context));
        contentValues.put(HttpHelper.NEW_NAME_PARAMETER, newName);
        SqlDbHelper.setName(contentValues, (callback));
    }

    public void setIsAutomated(boolean value, Common.OnStringResponseGivenCallback callback) {
        int intValue = 0;
        if(value)
            intValue = 1;
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, this.deviceID);
        contentValues.put(HttpHelper.VALUE_PARAMETER, intValue);
        SqlDbHelper.setIsAutomated(contentValues, (callback));
    }

    public void setIsDataObjectRefreshing(@NonNull Context context, boolean value, Common.OnStringResponseGivenCallback callback) {
        int valueInt = 0;
        if(value)
            valueInt = 1;
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, Common.getThisDeviceID(context));
        contentValues.put(HttpHelper.VALUE_PARAMETER, valueInt);
        SqlDbHelper.setIsDataObjectRefreshing(contentValues, (callback));
    }

    public void setBatteryPercent(@NonNull Context context, int batteryPercent, Common.OnStringResponseGivenCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, Common.getThisDeviceID(context));
        contentValues.put(HttpHelper.VALUE_PARAMETER, batteryPercent);
        SqlDbHelper.setBatteryPercent(contentValues, (callback));
    }

    public static void deleteAccount(@NonNull Context context, Common.OnStringResponseGivenCallback callback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, Common.getThisDeviceID(context));
        SqlDbHelper.deleteAccount(contentValues, (callback));
    }

    /**
     * Return the name associated to the instance of the EcoWateringHub.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return device's ID associated to the instance of the EcoWateringHub.
     */
    public String getDeviceID() {
        return this.deviceID;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    /**
     * Return am address string associated to the instance of the EcoWateringHub.
     */
    public String getPosition() {
        return this.address + ", " + this.city + " - " + this.country;
    }

    public WeatherInfo getWeatherInfo() {
        return this.weatherInfo;
    }

    /**
     * Return the remote device list associated to the instance of the EcoWateringHub.
     */
    public List<String> getRemoteDeviceList() {
        if(this.remoteDeviceList != null) {
            return this.remoteDeviceList;
        }
        return new ArrayList<>();
    }

    public boolean isAutomated() {
        return this.isAutomated;
    }
    public boolean isDataObjectRefreshing() {
        return this.isDataObjectRefreshing;
    }
    public int getBatteryPercent() {
        return this.batteryPercent;
    }

    public IrrigationSystem getIrrigationSystem() {
        return this.irrigationSystem;
    }

    public SensorsInfo getSensorInfo() {
        return this.sensorInfo;
    }

    public IrrigationPlan getIrrigationPlan() {
        return this.irrigationPlan;
    }

    public int getBatteryImageResourceId() {
        if(this.batteryPercent < 0)
            return R.drawable.battery_alert_icon;
        else if(this.batteryPercent == 0)
            return R.drawable.battery_0_icon;
        else if(this.batteryPercent <= 20)
            return R.drawable.battery_2_icon;
        else if(this.batteryPercent <= 40)
            return R.drawable.battery_4_icon;
        else if(this.batteryPercent <=80)
            return R.drawable.battery_6_icon;
        else
            return R.drawable.battery_full_icon;
    }

    @NonNull
    @Override
    public String toString() { return this.name + " - " + this.deviceID; }

    /**
     * Callable from EcoWateringHub module only.
     * @return double: ambient temperature value.
     */
    public double getAmbientTemperature() {
        if((this.sensorInfo != null) && (this.sensorInfo.getAmbientTemperatureChosenSensor() != null) &&
                (this.sensorInfo.getAmbientTemperatureLastUpdate() != null) &&
                (this.sensorInfo.isLastUpdateValid(this.sensorInfo.getAmbientTemperatureLastUpdate()))) {
            return this.sensorInfo.getAmbientTemperatureSensorValue();
        }
        else {
            return this.weatherInfo.getAmbientTemperature();
        }
    }

    /**
     * Callable from EcoWateringHub module only.
     * @return double: UV index.
     */
    public double getIndexUV() {
        int hourFromTimestamp = Integer.parseInt(this.weatherInfo.getTime().split("T")[1].split(":")[0]);
        boolean isInRangeTime = (hourFromTimestamp >= 7 && hourFromTimestamp < 20);
        if((this.weatherInfo.getWeatherCode() >= 0) && (this.weatherInfo.getWeatherCode() <= 3) &&
                (this.sensorInfo != null) && (this.sensorInfo.getLightChosenSensor() != null) &&
                (this.sensorInfo.getLightLastUpdate() != null) &&
                (this.sensorInfo.isLastUpdateValid(this.sensorInfo.getLightLastUpdate())) && isInRangeTime) {
            Log.i(Common.LOG_NORMAL, "index UV from Sensor");
            return (this.sensorInfo.getLightSensorValue() / 120);
        }
        else {
            Log.i(Common.LOG_NORMAL, "index UV from Open-Meteo");
            return this.weatherInfo.getIndexUV();
        }
    }

    /**
     * Callable from EcoWateringHub module only.
     * @return double: relative humidity value.
     */
    public double getRelativeHumidity() {
        if((this.sensorInfo != null) && (this.sensorInfo.getRelativeHumidityChosenSensor() != null) &&
                (this.sensorInfo.getRelativeHumidityLastUpdate() != null) &&
                (this.sensorInfo.isLastUpdateValid(this.sensorInfo.getRelativeHumidityLastUpdate()))) {
            return this.sensorInfo.getRelativeHumiditySensorValue();
        }
        else {
            return this.weatherInfo.getRelativeHumidity();
        }
    }

    // PARCELABLE IMPLEMENTATION

    protected EcoWateringHub(Parcel in) {
        deviceID = in.readString();
        name = in.readString();
        address = in.readString();
        city = in.readString();
        country = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        remoteDeviceList = in.createStringArrayList();
        isAutomated = in.readByte() != 0;
        isDataObjectRefreshing = in.readByte() != 0;
        batteryPercent = in.readInt();
        irrigationSystem = in.readParcelable(IrrigationSystem.class.getClassLoader());
        weatherInfo = in.readParcelable(WeatherInfo.class.getClassLoader());
        sensorInfo = in.readParcelable(SensorsInfo.class.getClassLoader());
        irrigationPlan = in.readParcelable(IrrigationPlan.class.getClassLoader());
    }

    public static final Creator<EcoWateringHub> CREATOR = new Creator<EcoWateringHub>() {
        @Override
        public EcoWateringHub createFromParcel(Parcel in) {
            return new EcoWateringHub(in);
        }

        @Override
        public EcoWateringHub[] newArray(int size) {
            return new EcoWateringHub[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(deviceID);
        dest.writeString(name);
        dest.writeString(address);
        dest.writeString(city);
        dest.writeString(country);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeStringList(remoteDeviceList);
        dest.writeByte((byte) (isAutomated ? 1 : 0));
        dest.writeByte((byte) (isDataObjectRefreshing ? 1 : 0));
        dest.writeInt(batteryPercent);
        dest.writeParcelable(irrigationSystem, flags);
        dest.writeParcelable(weatherInfo, flags);
        dest.writeParcelable(sensorInfo, flags);
        dest.writeParcelable(irrigationPlan, flags);
    }
}
