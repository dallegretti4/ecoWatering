package it.uniba.dib.sms2324.ecowateringcommon;

public interface OnConnectionFinishCallback {
    String BT_ALREADY_CONNECTED_DEVICE_RESPONSE = "remoteDeviceAlreadyExists";
    String BT_ERROR_RESPONSE = "error";
    String WIFI_ALREADY_CONNECTED_DEVICE_RESPONSE = "remoteDeviceAlreadyExists";
    String WIFI_CONNECTED_RESPONSE = "remoteDeviceAdded";
    String WIFI_ERROR_RESPONSE = "error";
    String CONNECTION_MODE_BLUETOOTH = "BLUETOOTH_CONNECTION_MODE";
    String CONNECTION_MODE_WIFI = "WIFI_CONNECTION_MODE";
    int CONNECTION_ALREADY_CONNECTED_DEVICE_RESULT = 1005;
    int CONNECTION_CONNECTED_DEVICE_RESULT = 1004;
    int CONNECTION_ERROR_RESULT = 1010;
    void onConnectionFinish(int resultCode);
    void restartFragment(String connectionMode);
}
