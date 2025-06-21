package it.uniba.dib.sms2324.ecowateringcommon.models;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.List;

import it.uniba.dib.sms2324.ecowateringcommon.Common;
import it.uniba.dib.sms2324.ecowateringcommon.R;

public class SensorsAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> stringList;
    private final int primary_color_50;
    private final int sensorIcon;
    public SensorsAdapter(@NonNull Context context, List<String> stringList, String sensorType, String calledFrom) {
        super(context, 0, stringList);
        this.context = context;
        this.stringList = stringList;
        if(calledFrom.equals(Common.CALLED_FROM_HUB)) this.primary_color_50 = R.color.ew_primary_color_50_from_hub;
        else this.primary_color_50 = R.color.ew_primary_color_50_from_device;
        switch (sensorType) {
            case SensorsInfo.CONFIGURE_SENSOR_TYPE_AMBIENT_TEMPERATURE:
                this.sensorIcon = R.drawable.sensor_ambient_temperature_icon;
                break;

            case SensorsInfo.CONFIGURE_SENSOR_TYPE_LIGHT:
                this.sensorIcon = R.drawable.sensor_light_icon;
                break;

            default:
                this.sensorIcon = R.drawable.sensor_relative_humidity_icon;
                break;
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(this.context).inflate(R.layout.card_sensor, parent, false);
        }
        // SENSOR INFO RECOVERING
        String sensorID = this.stringList.get(position);
        String name = sensorID.split(SensorsInfo.EW_SENSOR_ID_SEPARATOR)[1];
        String vendor = sensorID.split(SensorsInfo.EW_SENSOR_ID_SEPARATOR)[2];
        // SENSOR ICON BACKGROUND
        convertView.findViewById(R.id.sensorIconContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(this.context.getResources(), this.primary_color_50, context.getTheme()));
        ((ImageView)convertView.findViewById(R.id.sensorIconImageView)).setImageResource(this.sensorIcon); // SENSOR ICON
        ((TextView) convertView.findViewById(R.id.sensorNameTextView)).setText(name);   // SENSOR NAME
        ((TextView) convertView.findViewById(R.id.sensorVendorTextView)).setText(vendor);   // SENSOR VENDOR
        return convertView;
    }
}
