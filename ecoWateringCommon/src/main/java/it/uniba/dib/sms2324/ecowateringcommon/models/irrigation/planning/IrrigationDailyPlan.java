package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import it.uniba.dib.sms2324.ecowateringcommon.R;
import it.uniba.dib.sms2324.ecowateringcommon.models.WeatherInfo;

public class IrrigationDailyPlan {
    private static final String LANGUAGE_ENGLISH = "en";
    private static final String ENGLISH_SUFFIX_ORDINAL_NUMBER_ST = "st";
    private static final String ENGLISH_SUFFIX_ORDINAL_NUMBER_ND = "nd";
    private static final String ENGLISH_SUFFIX_ORDINAL_NUMBER_RD = "rd";
    private static final String ENGLISH_SUFFIX_ORDINAL_NUMBER_TH = "th";
    private static final String DATE_SPLITTER = "-";
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

    protected String getDay() {
        return this.day;
    }

    protected int getSpecificDay() {
        return Integer.parseInt(this.day.split(DATE_SPLITTER)[2]);
    }

    protected int getSpecificMonth() {
        return Integer.parseInt(this.day.split(DATE_SPLITTER)[1]);
    }

    protected int getSpecificYear() {
        return Integer.parseInt(this.day.split(DATE_SPLITTER)[0]);
    }

    protected double getAmbientTemperature() {
        return this.ambientTemperature;
    }

    protected double getIndexUV() {
        return this.indexUV;
    }

    protected double getRelativeHumidity() {
        return this.relativeHumidity;
    }

    protected int getWeatherCode() {
        return this.weatherCode;
    }

    protected double getPrecipitation() {
        return this.precipitation;
    }

    protected double getIrrigationMinutesPlan() {
        return this.irrigationMinutesPlan;
    }


    public View drawIrrigationDailyPlan(@NonNull Context context, LinearLayout container, int primary_color_50) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_irrigation_daily_plan, container, false);
        String day = String.valueOf(this.getSpecificDay());
        if(context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(LANGUAGE_ENGLISH)) {
            day = concatDayEnglishLanguage(this.getSpecificDay(), day);  // ENGLISH LANGUAGE CASE
        }
        String month = context.getResources().getStringArray(R.array.month_names)[this.getSpecificMonth()-1];
        int year = this.getSpecificYear();
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
        int quantity;
        if(this.getIrrigationMinutesPlan() > 0 && this.getIrrigationMinutesPlan() <1) {
            quantity = 1;
        }
        else if(this.getIrrigationMinutesPlan() > 1 && this.getIrrigationMinutesPlan() < 2) {
            quantity = 2;
        }
        else {
            quantity = (int) this.getIrrigationMinutesPlan();
        }
        ((TextView) view.findViewById(R.id.irrigationMinutesPlanLabelTextView)).setText(
                context.getResources().getQuantityString(
                        R.plurals.irrigation_minutes_plan_plurals,
                        quantity));
        return view;
    }

    private String concatDayEnglishLanguage(int intDay, String day) {
        switch (intDay) {
            case 1:
            case 21:
            case 31:
                return day.concat(ENGLISH_SUFFIX_ORDINAL_NUMBER_ST);

            case 2:
            case 22:
                return day.concat(ENGLISH_SUFFIX_ORDINAL_NUMBER_ND);

            case 3:
            case 23:
                return day.concat(ENGLISH_SUFFIX_ORDINAL_NUMBER_RD);

            default:
                return day.concat(ENGLISH_SUFFIX_ORDINAL_NUMBER_TH);
        }
    }
}
