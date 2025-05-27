package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class IrrigationPlan implements Parcelable {
    protected static final int FORECAST_DAYS = 7; // MAX 16
    public static final String BO_IRRIGATION_PLAN_COLUMN_NAME = "irrigationPlan";
    private static final String TABLE_IRRIGATION_PLAN_DAYS_COLUMN_NAME = "days";
    private static final String TABLE_IRRIGATION_PLAN_AMBIENT_TEMPERATURE_COLUMN_NAME = "ambientTemperature";
    private static final String TABLE_IRRIGATION_PLAN_UV_INDEX_COLUMN_NAME = "indexUV";
    private static final String TABLE_IRRIGATION_PLAN_RELATIVE_HUMIDITY_COLUMN_NAME = "relativeHumidity";
    private static final String TABLE_IRRIGATION_PLAN_WEATHER_CODE_COLUMN_NAME = "weatherCode";
    private static final String TABLE_IRRIGATION_PLAN_PRECIPITATION_COLUMN_NAME = "precipitation";
    private static final String TABLE_IRRIGATION_PLAN_IRRIGATION_MINUTES_PLAN_COLUMN_NAME = "irrigationMinutesPlan";
    protected static final String BO_FORECAST_DAYS_COLUMN_NAME = "forecast_days";
    protected String[] days = new String[FORECAST_DAYS];
    protected double[] ambientTemperature = new double[FORECAST_DAYS];
    protected double[] indexUV = new double[FORECAST_DAYS];
    protected double[] relativeHumidity = new double[FORECAST_DAYS];
    protected int[] weatherCode = new int[FORECAST_DAYS];
    protected double[] precipitation = new double[FORECAST_DAYS];
    protected double[] irrigationMinutesPlan = new double[FORECAST_DAYS];

    protected IrrigationPlan() {}

    public IrrigationPlan(String jsonString) {
        if(jsonString != null && !jsonString.equals(Common.NULL_STRING_VALUE)) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                String daysString = jsonObject.getString(TABLE_IRRIGATION_PLAN_DAYS_COLUMN_NAME);
                String ambientTemperatureString = jsonObject.getString(TABLE_IRRIGATION_PLAN_AMBIENT_TEMPERATURE_COLUMN_NAME);
                String indexUVString = jsonObject.getString(TABLE_IRRIGATION_PLAN_UV_INDEX_COLUMN_NAME);
                String relativeHumidityString = jsonObject.getString(TABLE_IRRIGATION_PLAN_RELATIVE_HUMIDITY_COLUMN_NAME);
                String weatherCodeString = jsonObject.getString(TABLE_IRRIGATION_PLAN_WEATHER_CODE_COLUMN_NAME);
                String precipitationString = jsonObject.getString(TABLE_IRRIGATION_PLAN_PRECIPITATION_COLUMN_NAME);
                String irrigationMinutesPlan = jsonObject.getString(TABLE_IRRIGATION_PLAN_IRRIGATION_MINUTES_PLAN_COLUMN_NAME);

                JSONArray daysJsonArray = new JSONArray(daysString);
                JSONArray ambientTemperatureJsonArray = new JSONArray(ambientTemperatureString);
                JSONArray indexUVJsonArray = new JSONArray(indexUVString);
                JSONArray relativeHumidityJsonArray = new JSONArray(relativeHumidityString);
                JSONArray weatherCodeJsonArray = new JSONArray(weatherCodeString);
                JSONArray precipitationJsonArray = new JSONArray(precipitationString);
                JSONArray irrigationMinutesPlanJsonArray = new JSONArray(irrigationMinutesPlan);

                for(int i=0; i<FORECAST_DAYS; i++) {
                    this.days[i] = daysJsonArray.getString(i);
                    this.ambientTemperature[i] = ambientTemperatureJsonArray.getDouble(i);
                    this.indexUV[i] = indexUVJsonArray.getDouble(i);
                    this.relativeHumidity[i] = relativeHumidityJsonArray.getDouble(i);
                    this.weatherCode[i] = weatherCodeJsonArray.getInt(i);
                    this.precipitation[i] = precipitationJsonArray.getDouble(i);
                    this.irrigationMinutesPlan[i] = irrigationMinutesPlanJsonArray.getDouble(i);
                }
            }
            catch (JSONException ignored) {}
        }
    }

    public void updateIrrigationPlanOnServer(String hubID, String caller) {
        StringBuilder daysString = new StringBuilder("[");
        StringBuilder ambientTemperatureString = new StringBuilder("[");
        StringBuilder indexUVString = new StringBuilder("[");
        StringBuilder relativeHumidityString = new StringBuilder("[");
        StringBuilder weatherCodeString = new StringBuilder("[");
        StringBuilder precipitationString = new StringBuilder("[");
        StringBuilder irrigationMinutesPlanString = new StringBuilder("[");
        for(int i=0; i<FORECAST_DAYS; i++) {
            if(i != (FORECAST_DAYS - 1)) {
                daysString.append("\"").append(this.days[i]).append("\",");
                ambientTemperatureString.append(this.ambientTemperature[i]).append(",");
                indexUVString.append(this.indexUV[i]).append(",");
                relativeHumidityString.append(this.relativeHumidity[i]).append(",");
                weatherCodeString.append(this.weatherCode[i]).append(",");
                precipitationString.append(this.precipitation[i]).append(",");
                irrigationMinutesPlanString.append(this.irrigationMinutesPlan[i]).append(",");
            }
            else {
                daysString.append("\"").append(this.days[i]).append("\"]");
                ambientTemperatureString.append(this.ambientTemperature[i]).append("]");
                indexUVString.append(this.indexUV[i]).append("]");
                relativeHumidityString.append(this.relativeHumidity[i]).append("]");
                weatherCodeString.append(this.weatherCode[i]).append("]");
                precipitationString.append(this.precipitation[i]).append("]");
                irrigationMinutesPlanString.append(this.irrigationMinutesPlan[i]).append("]");
            }
        }
        String jsonString = "{\"" +
                EcoWateringHub.TABLE_HUB_DEVICE_ID_COLUMN_NAME + "\":\"" + hubID + "\",\"" +
                HttpHelper.MODE_PARAMETER + "\":\"" + HttpHelper.MODE_UPDATE_IRRIGATION_PLAN + "\",\"" +
                HttpHelper.REMOTE_DEVICE_PARAMETER + "\":\"" + caller + "\",\"" +
                TABLE_IRRIGATION_PLAN_DAYS_COLUMN_NAME + "\":" + daysString + ",\"" +
                TABLE_IRRIGATION_PLAN_AMBIENT_TEMPERATURE_COLUMN_NAME + "\":" + ambientTemperatureString + ",\"" +
                TABLE_IRRIGATION_PLAN_UV_INDEX_COLUMN_NAME + "\":" + indexUVString + ",\"" +
                TABLE_IRRIGATION_PLAN_RELATIVE_HUMIDITY_COLUMN_NAME + "\":" + relativeHumidityString + ",\"" +
                TABLE_IRRIGATION_PLAN_WEATHER_CODE_COLUMN_NAME + "\":" + weatherCodeString + ",\"" +
                TABLE_IRRIGATION_PLAN_PRECIPITATION_COLUMN_NAME + "\":" + precipitationString + ",\"" +
                TABLE_IRRIGATION_PLAN_IRRIGATION_MINUTES_PLAN_COLUMN_NAME + "\":" + irrigationMinutesPlanString + "}";
        new Thread(() -> {
            String response = HttpHelper.sendHttpPostRequest(Common.getThisUrl(), jsonString);
            Log.i(Common.THIS_LOG, "updateIrrigationPlan response: " + response);
        }).start();
    }

    public ArrayList<IrrigationDailyPlan> getIrrigationDailyPlanList() {
        ArrayList<IrrigationDailyPlan> returnArrayList = new ArrayList<>();
        for(int i=0; i<FORECAST_DAYS; i++) {
            returnArrayList.add(new IrrigationDailyPlan(
                    this.days[i],
                    this.ambientTemperature[i],
                    this.indexUV[i],
                    this.relativeHumidity[i],
                    this.weatherCode[i],
                    this.precipitation[i],
                    this.irrigationMinutesPlan[i])
            );
        }
        return returnArrayList;
    }


    // PARCELABLE IMPLEMENTATION

    protected IrrigationPlan(Parcel in) {
        days = in.createStringArray();
        ambientTemperature = in.createDoubleArray();
        indexUV = in.createDoubleArray();
        relativeHumidity = in.createDoubleArray();
        weatherCode = in.createIntArray();
        precipitation = in.createDoubleArray();
        irrigationMinutesPlan = in.createDoubleArray();
    }

    public static final Creator<IrrigationPlan> CREATOR = new Creator<IrrigationPlan>() {
        @Override
        public IrrigationPlan createFromParcel(Parcel in) {
            return new IrrigationPlan(in);
        }

        @Override
        public IrrigationPlan[] newArray(int size) {
            return new IrrigationPlan[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringArray(days);
        dest.writeDoubleArray(ambientTemperature);
        dest.writeDoubleArray(indexUV);
        dest.writeDoubleArray(relativeHumidity);
        dest.writeIntArray(weatherCode);
        dest.writeDoubleArray(precipitation);
        dest.writeDoubleArray(irrigationMinutesPlan);
    }
}
