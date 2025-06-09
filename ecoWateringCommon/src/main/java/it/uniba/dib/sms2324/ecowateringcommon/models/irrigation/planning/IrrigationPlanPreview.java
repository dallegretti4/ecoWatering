package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SqlDbHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.WeatherInfo;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class IrrigationPlanPreview extends IrrigationPlan {
    private static final String BO_IRRIGATION_PLAN_AMBIENT_TEMPERATURE_PARAMETER = "temperature_2m_mean";
    private static final String BO_IRRIGATION_PLAN_INDEX_UV_PARAMETER = "uv_index_max";
    private static final String BO_IRRIGATION_PLAN_RELATIVE_HUMIDITY_PARAMETER = "relative_humidity_2m_mean";
    private static final String BO_IRRIGATION_PLAN_WEATHER_CODE_PARAMETER = "weather_code";
    private static final String BO_IRRIGATION_PLAN_PRECIPITATION_PARAMETER = "precipitation_sum";

    public IrrigationPlanPreview(String jsonString) {
        if(jsonString != null && !jsonString.equals(Common.NULL_STRING_VALUE)) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                JSONArray days = jsonObject.getJSONArray(WeatherInfo.BO_WEATHER_INFO_TIME_COLUMN_NAME);
                JSONArray ambientTemperature = jsonObject.getJSONArray(BO_IRRIGATION_PLAN_AMBIENT_TEMPERATURE_PARAMETER);
                JSONArray indexUV = jsonObject.getJSONArray(BO_IRRIGATION_PLAN_INDEX_UV_PARAMETER);
                JSONArray relativeHumidity = jsonObject.getJSONArray(BO_IRRIGATION_PLAN_RELATIVE_HUMIDITY_PARAMETER);
                JSONArray weatherCode = jsonObject.getJSONArray(BO_IRRIGATION_PLAN_WEATHER_CODE_PARAMETER);
                JSONArray precipitation = jsonObject.getJSONArray(BO_IRRIGATION_PLAN_PRECIPITATION_PARAMETER);
                for(int i=0; i<FORECAST_DAYS; i++) {
                    this.days[i] = days.getString(i);
                    this.ambientTemperature[i] = ambientTemperature.getDouble(i);
                    this.indexUV[i] = indexUV.getDouble(i);
                    this.relativeHumidity[i] = relativeHumidity.getDouble(i);
                    this.weatherCode[i] = weatherCode.getInt(i);
                    this.precipitation[i] = precipitation.getDouble(i);
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            this.irrigationMinutesPlan = generateIrrigationMinutesPlan();
        }
    }

    public static void getIrrigationPlanPreviewJsonString(@NonNull EcoWateringHub hub, String caller, Common.OnStringResponseGivenCallback callback) {
        String jsonString = "{\"" +
                SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + hub.getDeviceID() + "\",\"" +
                HttpHelper.REMOTE_DEVICE_PARAMETER + "\":\"" + caller + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_GET_IRRIGATION_PLAN_PREVIEW + "\",\"" +
                SqlDbHelper.TABLE_HUB_LATITUDE_COLUMN_NAME + "\":" + hub.getLatitude() + ",\"" +
                SqlDbHelper.TABLE_HUB_LONGITUDE_COLUMN_NAME + "\":" + hub.getLongitude() + ",\"" +
                BO_FORECAST_DAYS_COLUMN_NAME + "\":" + FORECAST_DAYS + "}";
        Log.i(Common.LOG_NORMAL, "------------------------>QUERY: " + jsonString);
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.LOG_NORMAL, "getIrrigationPlanPreview response: " + response);
            callback.getResponse(response);
        }).start();
    }

    private double[] generateIrrigationMinutesPlan() {
        double[] returnIrrigationPlan = new double[FORECAST_DAYS];
        for(int i=0; i<FORECAST_DAYS; i++) {
            returnIrrigationPlan[i] = generateIrrigationMinutesPerDay(i);
        }
        return returnIrrigationPlan;
    }

    private double generateIrrigationMinutesPerDay(int index) {
        double totalMinutes = BASE_DAILY_IRRIGATION_MINUTES;
        // NO IRRIGATION IF PRECIPITATION > 2
        if(this.precipitation[index] > 2.0) return 0;
        // +10% IF UV INDEX > 6
        if(this.indexUV[index] > 6) totalMinutes += (totalMinutes / 10);
        // +20% IF RELATIVE HUMIDITY < 30; -20% IF > 60
        if(this.relativeHumidity[index] < 30) totalMinutes += (totalMinutes * 2 / 10);
        else if(this.relativeHumidity[index] > 60) totalMinutes -= (totalMinutes * 2 / 10);
        // +50% IF AMBIENT TEMPERATURE > 30; -50% IF < 15
        if(this.ambientTemperature[index] > 30) totalMinutes += (totalMinutes * 5 / 10);
        else if(this.ambientTemperature[index] < 15) totalMinutes -= (totalMinutes * 5 / 10);
        // -30% IF WEATHER CODE > 3 (NOT CLEAR SKY)
        if(this.weatherCode[index] > 3) totalMinutes -= (totalMinutes * 3 / 10);
        // 0 IF TOTAL MINUTES < 0
        return Math.max(totalMinutes, 0);
    }
}
