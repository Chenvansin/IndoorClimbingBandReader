package com.hepthst.indoorclimbingbandreader;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.util.Log;

/**
 * Created by hepthSt on 2017/10/11.
 */
class BluetoothIO extends BluetoothGattCallback {
    private static final String TAG = "BluetoothIO";
    BluetoothGatt gatt;

    public BluetoothDevice getDevice() {
        if (null == gatt) {
            Log.e(TAG, "connect to miband first");
            return null;
        }
        return gatt.getDevice();
    }

}
