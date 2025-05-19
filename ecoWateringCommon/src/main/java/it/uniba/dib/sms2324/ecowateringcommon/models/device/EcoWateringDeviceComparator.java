package it.uniba.dib.sms2324.ecowateringcommon.models.device;

import java.util.Comparator;
public class EcoWateringDeviceComparator implements Comparator<EcoWateringDevice> {
    @Override
    public int compare(EcoWateringDevice device, EcoWateringDevice t1) {
        if(device.getName().equals(t1.getName())) {
            return device.getDeviceID().compareTo(t1.getDeviceID());
        }
        else {
            return device.getName().compareTo(t1.getName());
        }
    }
}
