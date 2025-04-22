package it.uniba.dib.sms2324.ecowateringhub.runnable;

import it.uniba.dib.sms2324.ecowateringcommon.EcoWateringHub;
import it.uniba.dib.sms2324.ecowateringhub.MainActivity;

public class RefreshHubRunnable implements Runnable {
    private final String hubID;
    public RefreshHubRunnable(String hubID) {
        this.hubID = hubID;
    }

    @Override
    public void run() {
        EcoWateringHub.getEcoWateringHubJsonString(this.hubID, (jsonResponse) -> {
            if((jsonResponse != null) && (jsonResponse.equals("null"))) {
                MainActivity.thisEcoWateringHub = new EcoWateringHub(jsonResponse);
            }
        });
    }
}
