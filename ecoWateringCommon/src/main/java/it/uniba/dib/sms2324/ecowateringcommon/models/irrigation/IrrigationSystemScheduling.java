package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;

public class IrrigationSystemScheduling implements Parcelable {
    public static final String BO_IRR_SYS_SCHEDULING_STARTING_DATE = "startingDate";
    public static final String BO_IRR_SYS_SCHEDULING_STARTING_TIME = "startingTime";
    public static final String BO_IRR_SYS_SCHEDULING_IRRIGATION_DURATION = "irrigationDuration";
    private int[] startingDate = new int[3];
    private int[] startingTime = new int[2];
    private int[] irrigationDuration = new int[2];

    public IrrigationSystemScheduling(String jsonString) {
        if((jsonString != null) && !jsonString.equals(Common.NULL_STRING_VALUE)) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                if(!jsonObject.isNull(BO_IRR_SYS_SCHEDULING_STARTING_DATE)) {
                    JSONArray jsonArray = jsonObject.getJSONArray(BO_IRR_SYS_SCHEDULING_STARTING_DATE);
                    for (int i = 0; i<3; i++) {
                        this.startingDate[i] = jsonArray.getInt(i);
                    }
                }
                if(!jsonObject.isNull(BO_IRR_SYS_SCHEDULING_STARTING_TIME)) {
                    JSONArray jsonArray = jsonObject.getJSONArray(BO_IRR_SYS_SCHEDULING_STARTING_TIME);
                    for (int i = 0; i<2; i++) {
                        this.startingTime[i] = jsonArray.getInt(i);
                    }
                }
                if(!jsonObject.isNull(BO_IRR_SYS_SCHEDULING_IRRIGATION_DURATION)) {
                    JSONArray jsonArray = jsonObject.getJSONArray(BO_IRR_SYS_SCHEDULING_IRRIGATION_DURATION);
                    for (int i = 0; i<2; i++) {
                        this.irrigationDuration[i] = jsonArray.getInt(i);
                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void draw(@NonNull Context context, @NonNull View view, int primary_color_50, View.OnClickListener closeClickListener, View.OnClickListener deleteSchedulingClickListener) {
        view.findViewById(R.id.schedulingTitle).setBackgroundTintList(ResourcesCompat.getColorStateList(context.getResources(), primary_color_50, context.getTheme()));
        // DATE SETUP
        String day = String.valueOf(this.startingDate[2]);
        if(context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(Common.LANGUAGE_ENGLISH))
            day = Common.concatDayEnglishLanguage(this.startingDate[2], day);  // ENGLISH LANGUAGE CASE
        String month = context.getResources().getStringArray(R.array.month_names)[this.startingDate[1]-1];
        String date = context.getString(R.string.date_builder_extended_label, month, day, this.startingDate[0]);
        ((TextView) view.findViewById(R.id.startingDateTextView)).setText(date);
        // STARTING TIME SETUP
        String startingTime = this.startingTime[0] + ":" + this.startingTime[1];
        ((TextView) view.findViewById(R.id.startingTimeTextView)).setText(startingTime);
        // DURATION SETUP
        if(this.irrigationDuration[0] > 0) {
            String hours = this.irrigationDuration[0] + "h";
            ((TextView) view.findViewById(R.id.durationHoursTextView)).setText(hours);
        }
        if(this.irrigationDuration[1] > 0) {
            String minutes = this.irrigationDuration[1] + "m";
            if(this.irrigationDuration[1] < 10)
                minutes = "0" + this.irrigationDuration[1] + "m";
            ((TextView) view.findViewById(R.id.durationMinutesTextView)).setText(minutes);
        }
        // CLOSE BUTTON SETUP
        ((Button) view.findViewById(R.id.deleteSchedulingButton)).setOnClickListener(deleteSchedulingClickListener);
        // DELETE SCHEDULING BUTTON SETUP
        ImageView closeSchedulingImageView = view.findViewById(R.id.closeSchedulingImageView);
        closeSchedulingImageView.setOnClickListener(closeClickListener);
    }


    //  PARCELABLE IMPLEMENTATION

    protected IrrigationSystemScheduling(Parcel in) {
        startingDate = in.createIntArray();
        startingTime = in.createIntArray();
        irrigationDuration = in.createIntArray();
    }

    public static final Creator<IrrigationSystemScheduling> CREATOR = new Creator<IrrigationSystemScheduling>() {
        @Override
        public IrrigationSystemScheduling createFromParcel(Parcel in) {
            return new IrrigationSystemScheduling(in);
        }

        @Override
        public IrrigationSystemScheduling[] newArray(int size) {
            return new IrrigationSystemScheduling[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeIntArray(startingDate);
        dest.writeIntArray(startingTime);
        dest.writeIntArray(irrigationDuration);
    }
}
