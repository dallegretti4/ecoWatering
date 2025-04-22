package it.uniba.dib.sms2324.ecowateringcommon;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

public class SharedPreferencesHelper {
    public static final String ECO_WATERING_HUB_SP_FILE_NAME = "eco_watering_hub_shared_preferences_file";
    public static final String THIS_ECO_WATERING_HUB_SP_KEY = "this_eco_watering_hub_shared_preferences_key";
    public static void saveThisEcoWateringHub(@NonNull Context context, String hubID) {
        EcoWateringHub.getEcoWateringHubJsonString(hubID, (jsonResponse) -> {
            SharedPreferences sharedPreferences = context.getSharedPreferences(ECO_WATERING_HUB_SP_FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(THIS_ECO_WATERING_HUB_SP_KEY, jsonResponse);
            editor.apply();
            Log.i(Common.THIS_LOG, "thisEcoWateringHub saved in SP");
        });
    }

    public static String getThisEcoWateringHub(@NonNull Context context, String hubID) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(ECO_WATERING_HUB_SP_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(THIS_ECO_WATERING_HUB_SP_KEY, "null");
    }
}
