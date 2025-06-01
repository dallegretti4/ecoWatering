package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.WeatherInfo;

public class IrrigationDailyPlan {
    private final String day;
    private final double ambientTemperature;
    private final double indexUV;
    private final double relativeHumidity;
    private final int weatherCode;
    private final double precipitation;
    private final double irrigationMinutesPlan;

    protected IrrigationDailyPlan(String day, double ambientTemperature, double indexUV, double relativeHumidity, int weatherCode, double precipitation, double irrigationMinutesPlan) {
        this.day = day;
        this.ambientTemperature = ambientTemperature;
        this.indexUV = indexUV;
        this.relativeHumidity = relativeHumidity;
        this.weatherCode = weatherCode;
        this.precipitation = precipitation;
        this.irrigationMinutesPlan = irrigationMinutesPlan;
    }

    private double getAmbientTemperature() {
        return this.ambientTemperature;
    }

    private double getIndexUV() {
        return this.indexUV;
    }

    private double getRelativeHumidity() {
        return this.relativeHumidity;
    }

    private int getWeatherCode() {
        return this.weatherCode;
    }

    private double getPrecipitation() {
        return this.precipitation;
    }

    public double getIrrigationMinutesPlan() {
        return this.irrigationMinutesPlan;
    }


    public View drawIrrigationDailyPlan(@NonNull Context context, LinearLayout container, int primary_color_50) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_irrigation_daily_plan, container, false);
        // DAY RECOVERING
        String day = String.valueOf(Common.getSpecificDay(this.day));
        if(context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(Common.LANGUAGE_ENGLISH))
            day = Common.concatDayEnglishLanguage(Common.getSpecificDay(this.day), day);  // ENGLISH LANGUAGE CASE
        // MONTH RECOVERING
        String month = context.getResources().getStringArray(R.array.month_names)[Common.getSpecificMonth(this.day)-1];
        int year = Common.getSpecificYear(this.day);  // YEAR RECOVERING

        view.findViewById(R.id.dateContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        ((TextView) view.findViewById(R.id.dateTextView)).setText(context.getString(R.string.date_builder_label, month, day, year));
        view.findViewById(R.id.weatherIconImageViewContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        ((ImageView) view.findViewById(R.id.weatherIconImageView)).setImageResource(WeatherInfo.getWeatherImageResourceId(this.getWeatherCode()));
        ((TextView) view.findViewById(R.id.weatherStateFirstDegreesTextView)).setText(String.valueOf(this.getAmbientTemperature()));
        view.findViewById(R.id.weatherStateAddressTextView).setVisibility(View.GONE);
        TextView relativeHumidityPercentTextView = view.findViewById(R.id.relativeHumidityPercentTextView);
        relativeHumidityPercentTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        relativeHumidityPercentTextView.setText(String.valueOf((int) this.getRelativeHumidity()));
        ((TextView) view.findViewById(R.id.relativeHumidityLabelTextView)).setText(context.getString(R.string.relative_humidity_mean_percent_label));
        TextView lightIndexTextView = view.findViewById(R.id.lightIndexTextView);
        lightIndexTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        lightIndexTextView.setText(String.valueOf((int) this.getIndexUV()));
        ((TextView) view.findViewById(R.id.lightIndexLabelTextView)).setText(context.getString(R.string.light_uv_index_max_label));
        view.findViewById(R.id.precipitationValueContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        ((TextView) view.findViewById(R.id.precipitationValueTextView)).setText(String.valueOf(this.getPrecipitation()));
        ((TextView) view.findViewById(R.id.precipitationLabelTextView)).setText(context.getString(WeatherInfo.getPrecipitationStringResourceId(this.getPrecipitation())));
        ((TextView) view.findViewById(R.id.irrigationMinutesPlanTextView)).setText(String.valueOf((int) this.getIrrigationMinutesPlan()));
        // IRRIGATION MINUTES SETUP
        int quantity;
        if(this.getIrrigationMinutesPlan() > 0 && this.getIrrigationMinutesPlan() <1)
            quantity = 1;
        else if(this.getIrrigationMinutesPlan() > 1 && this.getIrrigationMinutesPlan() < 2)
            quantity = 2;
        else
            quantity = (int) this.getIrrigationMinutesPlan();
        ((TextView) view.findViewById(R.id.irrigationMinutesPlanLabelTextView)).setText(context.getResources().getQuantityString(
                        R.plurals.irrigation_minutes_plan_plurals,
                        quantity));
        return view;
    }
}
