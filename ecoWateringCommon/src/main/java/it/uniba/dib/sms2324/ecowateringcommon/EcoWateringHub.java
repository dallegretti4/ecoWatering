package it.uniba.dib.sms2324.ecowateringcommon;

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

public class EcoWateringHub implements Parcelable {
    public static final int HUB_FROM_HUB_CASE = 3000;
    public static final int HUB_FROM_DEVICE_CASE = 3001;
    public static final String HUB_EXISTS_RESPONSE = "0";
    public static final String TABLE_HUB_TABLE_NAME = "EcoWateringHub";
    public static final String TABLE_HUB_DEVICE_ID_COLUMN_NAME = "deviceID";
    public static final String TABLE_HUB_NAME_COLUMN_NAME = "name";
    public static final String TABLE_HUB_ADDRESS_COLUMN_NAME = "address";
    public static final String TABLE_HUB_CITY_COLUMN_NAME = "city";
    public static final String TABLE_HUB_COUNTRY_COLUMN_NAME = "country";
    public static final String TABLE_HUB_LATITUDE_COLUMN_NAME = "latitude";
    public static final String TABLE_HUB_LONGITUDE_COLUMN_NAME = "longitude";
    public static final String TABLE_HUB_REMOTE_DEVICE_LIST_COLUMN_NAME = "remoteDeviceList";
    private String deviceID;
    private String name;
    private String address;
    private String city;
    private String country;
    private double latitude;
    private double longitude;

    // NOT ON DATABASE
    private List<String> remoteDeviceList;
    private EcoWateringHubConfiguration ecoWateringHubConfiguration;
    private WeatherInfo weatherInfo;
    private SensorsInfo sensorInfo;

    public interface EcoWateringHubFromJsonStringCallback {
        void convert(String response);
    }
    public interface HubExistsCallback {
        void getResponse(String response);
    }
    public interface AddNewEcoWateringHubCallback {
        void canRestartApp();
    }
    public interface RemoveRemoteDeviceCallback {
        void getResponse(String response);
    }

    public interface OnIsAutomatedSetCallback {
        void onIsAutomatedSet(String response);
    }

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
            String tmpString = jsonOBJ.getString(EcoWateringHub.TABLE_HUB_REMOTE_DEVICE_LIST_COLUMN_NAME);
            if(!tmpString.equals("null")) {
                JSONArray jsonRemoteDeviceList = new JSONArray(tmpString);
                for(int i=0; i<jsonRemoteDeviceList.length(); i++) {
                    this.remoteDeviceList.add(jsonRemoteDeviceList.getString(i));
                }
            }
            // ECO WATERING HUB CONFIGURATION RECOVERING
            tmpString = "null";
            tmpString = jsonOBJ.getString(Common.BO_HUB_CONFIGURATION_COLUMN_NAME);
            if(!tmpString.equals("null")) {
                this.ecoWateringHubConfiguration = new EcoWateringHubConfiguration(tmpString);
            }
            // WEATHER INFO RECOVERING
            tmpString = "null";
            tmpString = jsonOBJ.getString(WeatherInfo.BO_WEATHER_INFO_OBJ_NAME);
            if(!tmpString.equals("null")) {
                this.weatherInfo = new WeatherInfo(tmpString);
            }
            // SENSORS INFO RECOVERING
            tmpString = "null";
            tmpString = jsonOBJ.getString(SensorsInfo.BO_SENSORS_INFO_OBJ_NAME);
            if(!tmpString.equals("null")) {
                this.sensorInfo = new SensorsInfo(tmpString);
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String deviceID;
     *  HubExistsCallback callback -> to avoid the caller to manage the response.
     * Get string response from database server about specific EcoWateringDevice instance existence.
     * 0 -> true;
     * 1 -> false (and any other character)
     */
    public static void exists(String deviceID, HubExistsCallback callback) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"MODE\":\"HUB_EXISTS\"}";
        Log.i(Common.THIS_LOG, "jsonString: " + jsonString);
        Thread hubExistsThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "hubExists response: " + response);
            callback.getResponse(response);
        });
        hubExistsThread.start();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String deviceID;
     *  {@code @NonNull} String hubName;
     *  {@code @NonNull} Address address;
     *  AddNewEcoWateringHubCallback callback -> to notify the caller, who can restart the app.
     * To add new EcoWateringHub into the database server
     */
    public static void addNewEcoWateringHub(@NonNull String deviceID, @NonNull String hubName, @NonNull Address address, @NonNull IrrigationSystem irrigationSystem, AddNewEcoWateringHubCallback callback) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID +
                "\",\"" + EcoWateringHub.TABLE_HUB_NAME_COLUMN_NAME + "\":\"" + hubName +
                "\",\"MODE\":\"ADD_NEW_HUB\",\"" + EcoWateringHub.TABLE_HUB_ADDRESS_COLUMN_NAME + "\":\"" +
                address.getThoroughfare() + "\",\"" + EcoWateringHub.TABLE_HUB_CITY_COLUMN_NAME + "\":\"" +
                address.getLocality() + "\",\"" + EcoWateringHub.TABLE_HUB_COUNTRY_COLUMN_NAME + "\":\"" +
                address.getCountryName() + "\",\"" + EcoWateringHub.TABLE_HUB_LATITUDE_COLUMN_NAME + "\":" +
                address.getLatitude() + ",\"" + EcoWateringHub.TABLE_HUB_LONGITUDE_COLUMN_NAME + "\":" +
                address.getLongitude() + ",\"" + IrrigationSystem.TABLE_IRRIGATION_SYSTEM_MODEL_COLUMN_NAME + "\":\"" + irrigationSystem.getModel() + "\"}";
        Thread addNewHubThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "addNewHub response: " + response);
            callback.canRestartApp();
        });
        addNewHubThread.start();
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String hubID;
     *  EcoWateringHubFromJsonStringCallback callback -> to avoid the caller to manage the response.
     * Get the json response from database server about specific EcoWateringHub instance.
     */
    public static void getEcoWateringHubJsonString(String hubID, EcoWateringHubFromJsonStringCallback callback) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + hubID + "\",\"MODE\":\"GET_HUB_OBJ\"}";
        Thread getHubObjThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "getEWHubObj response: " + response);
            callback.convert(response);
        });
        getHubObjThread.start();
    }

    public String addNewRemoteDevice(@NonNull Context context, @NonNull String remoteDeviceID) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + Common.getThisDeviceID(context) + "\",\"MODE\":\"ADD_REMOTE_DEVICE\",\"REMOTE_DEVICE\":\"" + remoteDeviceID + "\"}";
        String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
        Log.i(Common.THIS_LOG, "response addRemoteDevice: " + response);
        return response;
    }

    /**
     * {@code @param:}
     *  {@code @NonNull} String deviceID;
     *  {@code @NonNull} EcoWateringDevice remoteDevice -> to remove;
     *  RemoveRemoteDeviceCallback callback -> to avoid the caller to manage the response.
     * To remove a remote device from a specific EcoWateringHub instance, on database server.
     */
    public static void removeRemoteDevice(@NonNull String deviceID, @NonNull EcoWateringDevice remoteDevice, RemoveRemoteDeviceCallback callback) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + deviceID + "\",\"MODE\":\"REMOVE_REMOTE_DEVICE\",\"REMOTE_DEVICE\":\"" + remoteDevice.getDeviceID() + "\"}";
        Thread removeRemoteDeviceThread = new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "removeRemoteDevice response: " + response);
            callback.getResponse(response);
        });
        removeRemoteDeviceThread.start();
    }

    public void setIsAutomated(String remoteDeviceID, boolean isAutomated, OnIsAutomatedSetCallback callback) {
        String jsonString = "{\"" + EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + this.deviceID +
                "\",\"MODE\":\"AUTOMATE_SYSTEM\",\"REMOTE_DEVICE\":\"" + remoteDeviceID +
                "\",\"" + Common.TABLE_CONFIGURATION_IS_AUTOMATED_COLUMN_NAME + "\":" + isAutomated + "}";
        Log.i(Common.THIS_LOG, "setIsAutomated jQuery: " + jsonString);
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "setIsAutomated response: " + response);
            callback.onIsAutomatedSet(response);
        }).start();
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

    /**
     * Return am address string associated to the instance of the EcoWateringHub.
     */
    public String getPosition() {
        return this.address + ", " + this.city + " - " + this.country;
    }

    /**
     * Return latitude associated to the instance of the EcoWateringHub.
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * Return longitude associated to the instance of the EcoWateringHub.
     */
    public double getLongitude() {
        return this.longitude;
    }

    public EcoWateringHubConfiguration getEcoWateringHubConfiguration() {
        return this.ecoWateringHubConfiguration;
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

    @NonNull
    @Override
    public String toString() { return this.name + " - " + this.deviceID; }

    /**
     * Callable from EcoWateringHub module only.
     * @return double: ambient temperature value.
     */
    public double getAmbientTemperature() {
        if((this.ecoWateringHubConfiguration.getAmbientTemperatureSensor() != null) &&
                (this.ecoWateringHubConfiguration.getAmbientTemperatureSensor().getSensorID() != null) &&
                (this.sensorInfo.isLastUpdateValid(this.sensorInfo.getAmbientTemperatureLastUpdate()))) {
            Log.i(Common.THIS_LOG, "ambient temperature from Sensor");
            return this.ecoWateringHubConfiguration.getAmbientTemperatureSensor().currentValue;
        }
        else {
            Log.i(Common.THIS_LOG, "ambient temperature from weather info");
            return this.weatherInfo.getAmbientTemperature();
        }
    }

    /**
     * Callable from EcoWateringHub module only.
     * @return double: UV index.
     */
    public double getIndexUV() {
        int hourFromTimestamp = Integer.parseInt(this.weatherInfo.getTime().split("T")[1].split(":")[0]);
        boolean isInRangeTime = (hourFromTimestamp >= 7 && hourFromTimestamp < 16);
        if((this.weatherInfo.getWeatherCode() >= 0) &&
                (this.weatherInfo.getWeatherCode() <= 3) &&
                (this.ecoWateringHubConfiguration != null) &&
                (this.ecoWateringHubConfiguration.getLightSensor() != null) &&
                (this.ecoWateringHubConfiguration.getLightSensor().getSensorID() != null) &&
                (this.sensorInfo.isLastUpdateValid(this.sensorInfo.getLightLastUpdate())) &&
                isInRangeTime) {
            Log.i(Common.THIS_LOG, "index UV from Sensor");
            return (this.sensorInfo.getLightSensor() / 120);
        }
        else {
            Log.i(Common.THIS_LOG, "index UV from weatherInfo");
            return this.weatherInfo.getIndexUV();
        }
    }

    /**
     * Callable from EcoWateringHub module only.
     * @return double: relative humidity value.
     */
    public double getRelativeHumidity() {
        if((this.ecoWateringHubConfiguration != null) &&
                (this.ecoWateringHubConfiguration.getRelativeHumiditySensor() != null) &&
                (this.ecoWateringHubConfiguration.getRelativeHumiditySensor().getSensorID() != null) &&
                (this.sensorInfo.isLastUpdateValid(this.sensorInfo.getRelativeHumidityLastUpdate()))) {
            Log.i(Common.THIS_LOG, "relative humidity from Sensor");
            return this.ecoWateringHubConfiguration.getLightSensor().currentValue;
        }
        else {
            Log.i(Common.THIS_LOG, "relative humidity from weather info");
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
        ecoWateringHubConfiguration = in.readParcelable(EcoWateringHubConfiguration.class.getClassLoader());
        weatherInfo = in.readParcelable(WeatherInfo.class.getClassLoader());
        sensorInfo = in.readParcelable(SensorsInfo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceID);
        dest.writeString(name);
        dest.writeString(address);
        dest.writeString(city);
        dest.writeString(country);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeStringList(remoteDeviceList);
        dest.writeParcelable(ecoWateringHubConfiguration, flags);
        dest.writeParcelable(weatherInfo, flags);
        dest.writeParcelable(sensorInfo, flags);
    }

    @Override
    public int describeContents() {
        return 0;
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
}
