package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;

public class IrrigationSystemActivityLogInstanceAdapter extends ArrayAdapter<IrrigationSystemActivityLogInstance> {
    private static final String ZERO_PERCENT_STRING_VALUE = "0%";
    private static final String DATE_DIVISOR = "T";
    private final Context context;
    private final ArrayList<IrrigationSystemActivityLogInstance> logInstanceArrayList;
    private final int primary_color;
    private final int primary_color_50;
    public IrrigationSystemActivityLogInstanceAdapter(@NonNull Context context, ArrayList<IrrigationSystemActivityLogInstance> logInstanceArrayList, String calledFrom) {
        super(context, 0, logInstanceArrayList);
        this.context = context;
        this.logInstanceArrayList = logInstanceArrayList;
        if(calledFrom.equals(Common.CALLED_FROM_HUB)) {
            this.primary_color = R.color.ew_primary_color_from_hub;
            this.primary_color_50 = R.color.ew_primary_color_50_from_hub;
        }
        else {
            this.primary_color = R.color.ew_primary_color_from_device;
            this.primary_color_50 = R.color.ew_primary_color_50_from_device;
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(this.context).inflate(R.layout.list_item_irrigation_system_activity_log_instance, parent, false);
        }
        IrrigationSystemActivityLogInstance activityLogInstance = this.logInstanceArrayList.get(position);
        TextView savedMinutesPercentValueTextView = convertView.findViewById(R.id.savedMinutesPercentValueTextView);
        savedMinutesPercentValueTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(this.context.getResources(), this.primary_color_50, this.context.getTheme()));
        // SAVED MINUTED PERCENT RECOVERING
        String percentValueString = ZERO_PERCENT_STRING_VALUE;
        if(activityLogInstance.getSavedMinutesPercent() > 0) {
            percentValueString = "+" + activityLogInstance.getSavedMinutesPercent() + "%";
        }
        else if(activityLogInstance.getSavedMinutesPercent() < 0) {
            percentValueString = activityLogInstance.getSavedMinutesPercent() + "%";
            savedMinutesPercentValueTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(this.context.getResources(), R.color.ew_secondary_color_30, this.context.getTheme()));
        }
        savedMinutesPercentValueTextView.setText(percentValueString);
        // DATE SETUP
        String parsedDate = activityLogInstance.getDate().split(DATE_DIVISOR)[0];
        String day = String.valueOf(Common.getSpecificDay(parsedDate)); // DAY RECOVERING
        if(context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(Common.LANGUAGE_ENGLISH))
            day = Common.concatDayEnglishLanguage(Common.getSpecificDay(parsedDate), day);  // ENGLISH LANGUAGE CASE
        // MONTH RECOVERING
        String month = context.getResources().getStringArray(R.array.month_names)[Common.getSpecificMonth(parsedDate)-1];
        int year = Common.getSpecificYear(parsedDate);  // YEAR RECOVERING
        TextView dateTextView = convertView.findViewById(R.id.dateTextView);
        dateTextView.setText(context.getString(R.string.date_builder_label, month, day, year));
        dateTextView.setBackgroundTintList(ResourcesCompat.getColorStateList(this.context.getResources(), this.primary_color, this.context.getTheme()));
        // WATERING MINUTES ON THIS DAY
        ((TextView) convertView.findViewById(R.id.wateringMinutesExecutedValueTextView)).setText(String.valueOf(activityLogInstance.getMinutes()));
        return convertView;
    }
}
