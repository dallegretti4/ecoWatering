package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning;

import android.content.ContentValues;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.HttpHelper;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SqlDbHelper;

public class IrrigationPlan implements Parcelable {
    protected static final int FORECAST_DAYS = 7; // MAX 16
    public static final double BASE_DAILY_IRRIGATION_MINUTES = 20.0;
    public static final String BO_IRRIGATION_PLAN_COLUMN_NAME = "irrigationPlan";
    public static final String BO_FORECAST_DAYS_COLUMN_NAME = "forecast_days";
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
                String daysString = jsonObject.getString(SqlDbHelper.TABLE_IRRIGATION_PLAN_DAYS_COLUMN_NAME);
                String ambientTemperatureString = jsonObject.getString(SqlDbHelper.TABLE_IRRIGATION_PLAN_AMBIENT_TEMPERATURE_COLUMN_NAME);
                String indexUVString = jsonObject.getString(SqlDbHelper.TABLE_IRRIGATION_PLAN_UV_INDEX_COLUMN_NAME);
                String relativeHumidityString = jsonObject.getString(SqlDbHelper.TABLE_IRRIGATION_PLAN_RELATIVE_HUMIDITY_COLUMN_NAME);
                String weatherCodeString = jsonObject.getString(SqlDbHelper.TABLE_IRRIGATION_PLAN_WEATHER_CODE_COLUMN_NAME);
                String precipitationString = jsonObject.getString(SqlDbHelper.TABLE_IRRIGATION_PLAN_PRECIPITATION_COLUMN_NAME);
                String irrigationMinutesPlan = jsonObject.getString(SqlDbHelper.TABLE_IRRIGATION_PLAN_IRRIGATION_MINUTES_PLAN_COLUMN_NAME);

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

    public String getNextIrrigationActionTime(@NonNull Context context, boolean state) {
        StringBuilder returnString;
        int hours = getIrrigationSystemStartingHours();
        int intMinutes = getIrrigationSystemStartingMinutes();
        if(state) {
            returnString = new StringBuilder(context.getString(R.string.next_stopping_label));
            if(((int) this.irrigationMinutesPlan[0]) >= 60) {
                hours += ((int) this.irrigationMinutesPlan[0]) / 60;
                intMinutes = ((int) this.irrigationMinutesPlan[0]) % 60;
            }
            else intMinutes = ((int) this.irrigationMinutesPlan[0]);
        }
        else returnString = new StringBuilder(context.getString(R.string.next_starting_label));

        String stringMinutes = Integer.toString(intMinutes);
        if(intMinutes < 10) {
            stringMinutes = "0" + intMinutes;
        }
        return returnString.append(" ").append(hours).append(":").append(stringMinutes).toString();
    }

    public int getIrrigationSystemStartingHours() {
        return 17;
    }

    public int getIrrigationSystemStartingMinutes() {
        return 0;
    }

    public int getIrrigationSystemStoppingHours(int indexDay) {
        int hours = getIrrigationSystemStartingHours();
        if(this.irrigationMinutesPlan[indexDay] >= 60) {
            hours += ((int) this.irrigationMinutesPlan[indexDay]) / 60;
        }
        return hours;
    }

    public int getIrrigationSystemStoppingMinutes(int indexDay) {
        int minutes = getIrrigationSystemStartingMinutes() + ((int) this.irrigationMinutesPlan[indexDay]);
        if(minutes >= 60) return (minutes % 60);
        else return minutes;
    }

    public void updateIrrigationPlanOnServer(@NonNull Context context, String hubID) {
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

        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, hubID);
        contentValues.put(HttpHelper.REMOTE_DEVICE_PARAMETER, Common.getThisDeviceID(context));
        contentValues.put(SqlDbHelper.TABLE_IRRIGATION_PLAN_DAYS_COLUMN_NAME, daysString.toString());
        contentValues.put(SqlDbHelper.TABLE_IRRIGATION_PLAN_AMBIENT_TEMPERATURE_COLUMN_NAME, ambientTemperatureString.toString());
        contentValues.put(SqlDbHelper.TABLE_IRRIGATION_PLAN_UV_INDEX_COLUMN_NAME, indexUVString.toString());
        contentValues.put(SqlDbHelper.TABLE_IRRIGATION_PLAN_RELATIVE_HUMIDITY_COLUMN_NAME, relativeHumidityString.toString());
        contentValues.put(SqlDbHelper.TABLE_IRRIGATION_PLAN_WEATHER_CODE_COLUMN_NAME, weatherCodeString.toString());
        contentValues.put(SqlDbHelper.TABLE_IRRIGATION_PLAN_PRECIPITATION_COLUMN_NAME, precipitationString.toString());
        contentValues.put(SqlDbHelper.TABLE_IRRIGATION_PLAN_IRRIGATION_MINUTES_PLAN_COLUMN_NAME, irrigationMinutesPlanString.toString());
        SqlDbHelper.updateIrrigationPlanOnServer(contentValues);
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
