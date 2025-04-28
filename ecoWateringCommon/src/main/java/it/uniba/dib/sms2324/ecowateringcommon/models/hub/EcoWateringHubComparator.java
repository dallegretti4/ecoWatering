package it.uniba.dib.sms2324.ecowateringcommon.models.hub;

import java.util.Comparator;

public class EcoWateringHubComparator implements Comparator<EcoWateringHub> {
    @Override
    public int compare(EcoWateringHub ecoWateringHub, EcoWateringHub t1) {
        if(ecoWateringHub.getName().equals(t1.getName())) {
            return ecoWateringHub.getDeviceID().compareTo(t1.getDeviceID());
        }
        else {
            return ecoWateringHub.getName().compareTo(t1.getName());
        }
    }
}
