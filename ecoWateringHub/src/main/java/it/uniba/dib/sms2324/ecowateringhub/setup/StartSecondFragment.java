package it.uniba.dib.sms2324.ecowateringhub.setup;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import it.uniba.dib.sms2324.ecowateringcommon.models.IrrigationSystem;
import it.uniba.dib.sms2324.ecowateringhub.R;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;

public class StartSecondFragment extends Fragment {
    private IrrigationSystem discoveredIrrigationSystem;
    private OnSecondStartFinishCallback onSecondStartFinishCallback;

    public interface OnSecondStartFinishCallback {
        void onSecondStartFinish(@NonNull IrrigationSystem irrigationSystem);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnSecondStartFinishCallback) {
            onSecondStartFinishCallback = (OnSecondStartFinishCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onSecondStartFinishCallback = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start_second, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Button nextButton = view.findViewById(R.id.secondStartFinishButton);
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        TextView modelTextView = view.findViewById(R.id.modelTextView);
        discoveredIrrigationSystem = IrrigationSystem.discoverIrrigationSystem(MainActivity.isSimulation);
        if(discoveredIrrigationSystem == null) {
            modelTextView.setVisibility(View.GONE);
            titleTextView.setText(getString(R.string.irrigation_system_not_founded_case));
            nextButton.setEnabled(false);
            nextButton.setBackgroundColor(getResources().getColor(R.color.ew_primary_color_80, requireActivity().getTheme()));
        }
        else {
            // SIMULATION CASE
            modelTextView.setText(discoveredIrrigationSystem.getModel());
        }
        nextButton.setOnClickListener((v) -> onSecondStartFinishCallback.onSecondStartFinish(discoveredIrrigationSystem));
    }
}
