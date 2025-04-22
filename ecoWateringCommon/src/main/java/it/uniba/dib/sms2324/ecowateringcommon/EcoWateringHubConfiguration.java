package it.uniba.dib.sms2324.ecowateringcommon;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class EcoWateringHubConfiguration implements Parcelable {
    private boolean isAutomated;
    private IrrigationSystem irrigationSystem;
    private AmbientTemperatureSensor ambientTemperatureSensor;
    private LightSensor lightSensor;
    private RelativeHumiditySensor relativeHumiditySensor;

    public EcoWateringHubConfiguration(String jsonString) {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);
            // IS AUTOMATED RECOVERING
            String tmpString = jsonOBJ.getString(Common.TABLE_CONFIGURATION_IS_AUTOMATED_COLUMN_NAME);
            this.isAutomated = tmpString.equals("1");
            tmpString = null;

            // IRRIGATION SYSTEM RECOVERING
            tmpString = jsonOBJ.getString(Common.BO_IRRIGATION_SYSTEM_COLUMN_NAME);
            if(!tmpString.equals("null")) {
                this.irrigationSystem = new IrrigationSystem(tmpString);
            }
            tmpString = null;


            // AMBIENT TEMPERATURE SENSOR RECOVERY
            tmpString = jsonOBJ.getString(Common.TABLE_CONFIGURATION_AMBIENT_TEMPERATURE_SENSOR_COLUMN_NAME);
            if(!tmpString.equals("") && !tmpString.equals("null")) {
                this.ambientTemperatureSensor = new AmbientTemperatureSensor(tmpString);
            }
            tmpString = null;

            // LIGHT SENSOR RECOVERY
            tmpString = jsonOBJ.getString(Common.TABLE_CONFIGURATION_LIGHT_SENSOR_COLUMN_NAME);
            if(!tmpString.equals("") && !tmpString.equals("null")) {
                this.lightSensor = new LightSensor(tmpString);
            }
            tmpString = null;

            // RELATIVE HUMIDITY SENSOR RECOVERY
            tmpString = jsonOBJ.getString(Common.TABLE_CONFIGURATION_RELATIVE_HUMIDITY_SENSOR_COLUMN_NAME);
            if(!tmpString.equals("") && !tmpString.equals("null")) {
                this.relativeHumiditySensor = new RelativeHumiditySensor(tmpString);
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isAutomated() {
        return isAutomated;
    }

    public IrrigationSystem getIrrigationSystem() {
        return this.irrigationSystem;
    }

    public AmbientTemperatureSensor getAmbientTemperatureSensor() {
        return this.ambientTemperatureSensor;
    }

    public LightSensor getLightSensor() {
        return this.lightSensor;
    }

    public RelativeHumiditySensor getRelativeHumiditySensor() {
        return this.relativeHumiditySensor;
    }

    // PARCELABLE IMPLEMENTATION
    protected EcoWateringHubConfiguration(Parcel in) {
        isAutomated = in.readByte() != 0;
        irrigationSystem = in.readParcelable(IrrigationSystem.class.getClassLoader());
        ambientTemperatureSensor = in.readParcelable(AmbientTemperatureSensor.class.getClassLoader());
        lightSensor = in.readParcelable(LightSensor.class.getClassLoader());
        relativeHumiditySensor = in.readParcelable(RelativeHumiditySensor.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isAutomated ? 1 : 0));
        dest.writeParcelable(irrigationSystem, flags);
        dest.writeParcelable(ambientTemperatureSensor, flags);
        dest.writeParcelable(lightSensor, flags);
        dest.writeParcelable(relativeHumiditySensor, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EcoWateringHubConfiguration> CREATOR = new Creator<EcoWateringHubConfiguration>() {
        @Override
        public EcoWateringHubConfiguration createFromParcel(Parcel in) {
            return new EcoWateringHubConfiguration(in);
        }

        @Override
        public EcoWateringHubConfiguration[] newArray(int size) {
            return new EcoWateringHubConfiguration[size];
        }
    };
}
