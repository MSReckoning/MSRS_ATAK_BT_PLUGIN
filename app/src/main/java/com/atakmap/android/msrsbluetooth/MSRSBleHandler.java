package com.atakmap.android.msrsbluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanResult;

import com.atakmap.android.bluetooth.BtLowEnergyManager;
import com.atakmap.android.bluetooth.BtLowEnergyManager.BluetoothLEConnectionStatus;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.Arrays;
import java.util.List;
public class MSRSBleHandler implements BtLowEnergyManager.BluetoothLEHandler {

    private final String TAG = "MSRSBleHandler";
    private NMEAProcessor nmeaProcessor;
    private BluetoothDevice device;

    private MapView view;
    private BluetoothGatt bluetoothGatt;
    private BluetoothLEConnectionStatus connectionStatus;
    private BluetoothGattCallback gattCallback; // Declare but do not initialize here

    private final List<String> deviceNames; // Declare deviceNames as an instance variable


    public MSRSBleHandler(NMEAProcessor nmeaProcessor) {
        deviceNames = Arrays.asList("Thermometer Device", "Thermometer", "Thermometer_Device");
        this.nmeaProcessor = nmeaProcessor;
        this.gattCallback = new MSRSBleGattCallback(this.nmeaProcessor); // Initialize here
    }


    @Override
    public boolean onScanResult(ScanResult scanResult) {
        if (scanResult != null && scanResult.getDevice() != null) {
            Log.d(TAG, scanResult.getDevice().getName() + " " + scanResult.getDevice().getAddress() + " " + scanResult.getDevice().getAlias());
            String name = scanResult.getDevice().getName();
            if (deviceNames.contains(name)) {
                this.device = scanResult.getDevice();
                Log.d(TAG, "Adding device" + " " + scanResult.getDevice().getName() + " " + scanResult.getDevice().getAddress() + " " + scanResult.getDevice().getAlias());
                return true;
            }
        }
        return false;
    }

    @Override
    public void connect() {
        if (device != null) {
            // Connect to the device. Context may need to be passed if required.
            Log.d(TAG, "Attempting to connect to " + device.getName() + " " + device.getUuids());
            bluetoothGatt = device.connectGatt(null, false, this.gattCallback);
        }
    }

    @Override
    public void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    @Override
    public void dispose() {
        close();
    }

    @Override
    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public void setConnectionListener(BluetoothLEConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
}
