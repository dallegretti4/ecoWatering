package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class EcoWateringHubAdapter extends ArrayAdapter<EcoWateringHub> {
    private final Context context;
    private final List<EcoWateringHub> ecoWateringHubList;

    public EcoWateringHubAdapter(@NonNull Context context, @NonNull List<EcoWateringHub> ecoWateringHubList) {
        super(context, 0, ecoWateringHubList);
        this.context = context;
        this.ecoWateringHubList = ecoWateringHubList;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        EcoWateringHub hub = ecoWateringHubList.get(position);
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_ecowatering_hub, parent, false);
        }
        ImageView weatherImageView = convertView.findViewById(R.id.weatherIconImageView);
        weatherImageView.setImageResource(hub.getWeatherInfo().getWeatherImageResourceId());
        TextView firstItemTextView = convertView.findViewById(R.id.firstItemTextView);
        firstItemTextView.setText(hub.getName());
        TextView secondItemTextView = convertView.findViewById(R.id.secondItemTextView);
        secondItemTextView.setText(hub.getPosition());
        return convertView;
    }
}
