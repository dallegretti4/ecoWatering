package it.uniba.dib.sms2324.ecowateringcommon;

public interface OnConnectionFinishCallback {
    String START_TO_CONNECTION_CHOOSER = "START_TO_CONNECTION_CHOOSER";
    int MAX_TIME_CONNECTION = 30 * 1000;
    String BT_ALREADY_CONNECTED_DEVICE_RESPONSE = "remoteDeviceAlreadyExists";
    String BT_ERROR_RESPONSE = "error";
    String WIFI_ALREADY_CONNECTED_DEVICE_RESPONSE = "remoteDeviceAlreadyExists";
    String WIFI_CONNECTED_RESPONSE = "remoteDeviceAdded";
    String WIFI_ERROR_RESPONSE = "error";
    String CONNECTION_MODE_BLUETOOTH = "BLUETOOTH_CONNECTION_MODE";
    String CONNECTION_MODE_WIFI = "WIFI_CONNECTION_MODE";
    String WIFI_SOCKET_REQUEST_NAME = "ecoWateringWiFiRequest";
    int CONNECTION_ALREADY_CONNECTED_DEVICE_RESULT = 1005;
    int CONNECTION_CONNECTED_DEVICE_RESULT = 1004;
    int CONNECTION_ERROR_RESULT = 1010;
    void onConnectionFinish(int resultCode);
    void restartFragment(String connectionMode);
    void closeConnection();
}
