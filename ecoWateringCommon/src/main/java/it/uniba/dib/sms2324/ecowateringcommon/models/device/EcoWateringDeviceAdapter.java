package it.uniba.dib.sms2324.ecowateringcommon.models.device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.List;

import it.uniba.dib.sms2324.ecowateringcommon.R;

public class EcoWateringDeviceAdapter extends ArrayAdapter<EcoWateringDevice> {
    public static final String CALLED_FROM_HUB = "CALLED_FROM_HUB";
    public static final String CALLED_FROM_DEVICE = "CALLED_FROM_DEVICE";
    private final Context context;
    private final List<EcoWateringDevice> ecoWateringDeviceList;
    private final String calledFrom;

    public EcoWateringDeviceAdapter(@NonNull Context context, @NonNull List<EcoWateringDevice> ecoWateringDeviceList, String calledFrom) {
        super(context, 0, ecoWateringDeviceList);
        this.context = context;
        this.ecoWateringDeviceList = ecoWateringDeviceList;
        this.calledFrom = calledFrom;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        EcoWateringDevice device = ecoWateringDeviceList.get(position);
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_ecowatering_device, parent, false);
        }
        TextView deviceNameTextView = convertView.findViewById(R.id.ecoWateringDeviceName);
        deviceNameTextView.setText(device.getName());
        TextView deviceIDTextView = convertView.findViewById(R.id.ecoWateringDeviceID);
        deviceIDTextView.setText(device.getDeviceID());
        if(this.calledFrom.equals(CALLED_FROM_HUB)) {
            convertView.findViewById(R.id.ecoWateringDeviceNameContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(this.context.getResources(), R.color.ew_primary_color_50_from_hub, this.context.getTheme()));
        }
        else {
            convertView.findViewById(R.id.ecoWateringDeviceNameContainer).setBackgroundTintList(ResourcesCompat.getColorStateList(this.context.getResources(), R.color.ew_primary_color_50_from_device, this.context.getTheme()));
        }
        return convertView;
    }
}
