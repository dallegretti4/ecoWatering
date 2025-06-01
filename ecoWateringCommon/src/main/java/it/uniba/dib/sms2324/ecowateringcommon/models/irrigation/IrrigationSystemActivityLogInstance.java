package it.uniba.dib.sms2324.ecowateringcommon.models.irrigation;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.models.irrigation.planning.IrrigationPlan;

public class IrrigationSystemActivityLogInstance implements Parcelable {
    private static final String BO_ACTIVITY_LOG_DATE = "date";
    private static final String BO_ACTIVITY_LOG_MINUTES = "minutes";
    private String date;
    private int minutes;
    protected IrrigationSystemActivityLogInstance(String jsonString) {
        if((jsonString != null) && (!jsonString.equals(Common.NULL_STRING_VALUE))) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                this.date = jsonObject.getString(BO_ACTIVITY_LOG_DATE);
                this.minutes = jsonObject.getInt(BO_ACTIVITY_LOG_MINUTES);
                Log.i(Common.LOG_NORMAL, "IrrigationSystemActivityLogInstance ---> date: " + this.date + ", minutes: " + this.minutes);
            }
            catch (JSONException e) { e.printStackTrace(); }
        }
    }

    protected IrrigationSystemActivityLogInstance(Parcel in) {
        date = in.readString();
        minutes = in.readInt();
    }

    public static final Creator<IrrigationSystemActivityLogInstance> CREATOR = new Creator<IrrigationSystemActivityLogInstance>() {
        @Override
        public IrrigationSystemActivityLogInstance createFromParcel(Parcel in) {
            return new IrrigationSystemActivityLogInstance(in);
        }

        @Override
        public IrrigationSystemActivityLogInstance[] newArray(int size) {
            return new IrrigationSystemActivityLogInstance[size];
        }
    };

    public String getDate() {
        return this.date;
    }

    protected int getMinutes() {
        return this.minutes;
    }

    public int getAutomatedSystemDays() {
        return Common.getDaysAgo(this.date);
    }

    public int getSavedMinutesPercent() {
        return ((int) (100 - ((this.minutes * 100) / IrrigationPlan.BASE_DAILY_IRRIGATION_MINUTES)));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(date);
        dest.writeInt(minutes);
    }
}
