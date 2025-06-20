package it.uniba.dib.sms2324.ecowateringcommon.models;

import android.content.ContentValues;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONException;
import org.json.JSONObject;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.helpers.SqlDbHelper;
import it.uniba.dib.sms2324.ecowateringcommon.models.hub.EcoWateringHub;

public class WeatherInfo implements Parcelable {
    public static final String BO_WEATHER_INFO_OBJ_NAME = "weatherInfo";
    public static final String BO_WEATHER_INFO_TIME_COLUMN_NAME = "time";
    private static final String BO_WEATHER_INFO_AMBIENT_TEMPERATURE_COLUMN_NAME = "ambientTemperature";
    private static final String BO_WEATHER_INFO_RELATIVE_HUMIDITY_COLUMN_NAME = "relativeHumidity";
    private static final String BO_WEATHER_INFO_PRECIPITATION_COLUMN_NAME = "precipitation";
    private static final String BO_WEATHER_INFO_WEATHER_CODE_COLUMN_NAME = "weatherCode";
    private static final String BO_WEATHER_INFO_UV_INDEX_COLUMN_NAME = "indexUV";
    private String time;
    private double ambientTemperature;
    private double relativeHumidity;
    private double precipitation;
    private int weatherCode;
    private double indexUV;

    public WeatherInfo(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            this.time = jsonOBJ.getString(WeatherInfo.BO_WEATHER_INFO_TIME_COLUMN_NAME);
            this.ambientTemperature = jsonOBJ.getDouble(WeatherInfo.BO_WEATHER_INFO_AMBIENT_TEMPERATURE_COLUMN_NAME);
            this.relativeHumidity = jsonOBJ.getDouble(WeatherInfo.BO_WEATHER_INFO_RELATIVE_HUMIDITY_COLUMN_NAME);
            this.precipitation = jsonOBJ.getDouble(WeatherInfo.BO_WEATHER_INFO_PRECIPITATION_COLUMN_NAME);
            this.weatherCode = jsonOBJ.getInt(WeatherInfo.BO_WEATHER_INFO_WEATHER_CODE_COLUMN_NAME);
            this.indexUV = jsonOBJ.getDouble(WeatherInfo.BO_WEATHER_INFO_UV_INDEX_COLUMN_NAME);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public static int getWeatherImageResourceId(int weatherCode) {
        switch(weatherCode) {
            // SUN CASE
            case 0:
            case 1:
            case 2:
            case 3:
                return R.drawable.weather_icon_sun;
            // CLOUD CASE
            case 45:
            case 48:
                return R.drawable.weather_icon_cloud;
            // SNOW CASE
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                return R.drawable.weather_icon_snow;
            // RAIN CASE
            case 51:
            case 53:
            case 55:
            case 56:
            case 57:
            case 61:
            case 63:
            case 65:
            case 66:
            case 67:
            case 80:
            case 81:
            case 82:
                return R.drawable.weather_icon_rain;
            // THUNDERSTORM CASE
            case 95:
            case 96:
            case 99:
                return R.drawable.weather_thunderstorm_icon;
            default:
                return R.drawable.refresh_icon;
        }
    }

    public static int getPrecipitationStringResourceId(double precipitation) {
        if(precipitation == 0.0) return R.string.precipitation_label_no_precipitation;
        else if(precipitation >= 0.1 && precipitation <= 1.0) return R.string.precipitation_label_chance_light_precipitation;
        else if(precipitation >= 1.1 && precipitation <= 3.0) return R.string.precipitation_label_light_precipitation;
        else if(precipitation >= 3.1 && precipitation <= 10.0) return R.string.precipitation_label_moderate_precipitation;
        else if(precipitation >= 10.1 && precipitation <= 25.0) return R.string.precipitation_label_heavy_precipitation;
        else return R.string.precipitation_label_intense_precipitation;
    }

    // BLOCKER
    public static void updateWeatherInfo(@NonNull Context context, @NonNull EcoWateringHub hub) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlDbHelper.TABLE_HUB_DEVICE_ID_COLUMN_NAME, Common.getThisDeviceID(context));
        contentValues.put(SqlDbHelper.TABLE_HUB_LATITUDE_COLUMN_NAME, hub.getLatitude());
        contentValues.put(SqlDbHelper.TABLE_HUB_LONGITUDE_COLUMN_NAME, hub.getLongitude());
        SqlDbHelper.updateWeatherInfo(contentValues);
    }

    public String getTime() {
        return this.time;
    }

    public double getAmbientTemperature() {
        return this.ambientTemperature;
    }

    public double getRelativeHumidity() {
        return relativeHumidity;
    }
    public double getPrecipitation() {
        return this.precipitation;
    }
    public int getWeatherCode() {
        return this.weatherCode;
    }

    public double getIndexUV() {
        return this.indexUV;
    }

    public void draw(@NonNull Context context, @NonNull View view, @NonNull final EcoWateringHub hub, final int primary_color_50) {
        // WEATHER IMAGE VIEW SETUP
        view.findViewById(R.id.weatherIconImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        ImageView weatherImageView = view.findViewById(R.id.weatherIconImageView);
        weatherImageView.setImageResource(WeatherInfo.getWeatherImageResourceId(this.getWeatherCode()));
        // AMBIENT TEMPERATURE TEXT VIEW SETUP
        TextView degreesTextView = view.findViewById(R.id.weatherStateFirstDegreesTextView);
        degreesTextView.setText(String.valueOf(((int) hub.getAmbientTemperature())));
        // HUB ADDRESS TEXT VIEW SETUP
        TextView addressTextView = view.findViewById(R.id.weatherStateAddressTextView);
        addressTextView.setText(hub.getPosition());
        // RELATIVE HUMIDITY PERCENT TEXT VIEW SETUP
        TextView relativeHumidityPercentTextView = view.findViewById(R.id.relativeHumidityPercentTextView);
        relativeHumidityPercentTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        relativeHumidityPercentTextView.setText(String.valueOf((int)(hub.getRelativeHumidity())));
        // UV INDEX TEXT VIEW SETUP
        TextView uvIndexTextView = view.findViewById(R.id.lightIndexTextView);
        uvIndexTextView.setText(String.valueOf((int)(hub.getIndexUV())));
        uvIndexTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        // PRECIPITATION CARD SETUP
        ((TextView) view.findViewById(R.id.precipitationValueTextView)).setText(String.valueOf(this.getPrecipitation()));
        view.findViewById(R.id.precipitationValueContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        ((TextView) view.findViewById(R.id.precipitationLabelTextView)).setText(context.getString(WeatherInfo.getPrecipitationStringResourceId(this.getPrecipitation())));
    }

    // PARCELABLE IMPLEMENTATION
    protected WeatherInfo(Parcel in) {
        time = in.readString();
        ambientTemperature = in.readDouble();
        relativeHumidity = in.readDouble();
        precipitation = in.readDouble();
        weatherCode = in.readInt();
        indexUV = in.readDouble();
    }

    public static final Creator<WeatherInfo> CREATOR = new Creator<WeatherInfo>() {
        @Override
        public WeatherInfo createFromParcel(Parcel in) {
            return new WeatherInfo(in);
        }

        @Override
        public WeatherInfo[] newArray(int size) {
            return new WeatherInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(time);
        parcel.writeDouble(ambientTemperature);
        parcel.writeDouble(relativeHumidity);
        parcel.writeDouble(precipitation);
        parcel.writeInt(weatherCode);
        parcel.writeDouble(indexUV);
    }
}
