package com.atakmap.android.msrsbluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;

import com.atakmap.coremap.log.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

class MSRSBleGattCallback extends BluetoothGattCallback {

    private final NMEAProcessor nmeaProcessor;
    List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    int index = 0;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 5;

    private final int initialBackoffMillis = 1000;


    private static final String TAG = "MSRSBluetoothGattCallback";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private static final String MSRS_CHARACTERISTIC_UUID = "00000002-710e-4a5b-8d75-3e5b444bc3cf";
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;

    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice device;


    public MSRSBleGattCallback(NMEAProcessor nmeaProcessor) {
        this.nmeaProcessor = nmeaProcessor;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        Log.d(TAG, "onConnectionStateChange: " + status + " " + newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "starting gatt discover services");
            gatt.discoverServices();
            retryCount = 0;
        } else if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            int backoffMillis = initialBackoffMillis * (int) Math.pow(2, retryCount - 1);
            Log.d(TAG, "reconnecting in " + backoffMillis + " milliseconds...");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!gatt.connect()) {
                    Log.d(TAG, "Reconnection attempt failed.");
                }
            }, backoffMillis);
        } else {
            Log.d(TAG, "Max retry attempts reached. Stopping reconnection attempts.");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onServicesDiscovered: not successful");
            return;
        }

        try {
            // BluetoothGatt gatt
            final Method refresh = gatt.getClass().getMethod("refresh");
            if (refresh != null) {
                Log.d(TAG, "Refreshing BLE service");
                refresh.invoke(gatt);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to refresh the BLE service");
        }

        final List<BluetoothGattService> gattServices = gatt.getServices();
        Log.d(TAG, "services #: " + gattServices.size());

        for (BluetoothGattService gattService : gattServices) {
            // Log service details
            Log.d(TAG, "service: " + gattService.getUuid().toString());



            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                // Log characteristic details
                Log.d(TAG, "\tcharacteristic (uuid): " + gattCharacteristic.getUuid().toString());
                Log.d(TAG, gattCharacteristic.getUuid().toString());
                Log.d(TAG, "Looking for a match above to " + MSRS_CHARACTERISTIC_UUID);

                if (gattCharacteristic.getUuid().toString().equals(MSRS_CHARACTERISTIC_UUID)) {
                    Log.d(TAG, "\tsetting up notification for: " + gattCharacteristic.getUuid().toString());
                    gatt.setCharacteristicNotification(gattCharacteristic, true);
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        byte[] data = characteristic.getValue();

        if (data != null && data.length > 0) {
            Log.d(TAG, Arrays.toString(data));
            this.nmeaProcessor.processPacket(data);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Log.d(TAG, "onCharacteristicWrite: " + gatt + " " + characteristic + " " + status);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        final byte[] data = characteristic.getValue();
        Log.d(TAG, "Received data" + " " + Arrays.toString(data) + " " + "from " + characteristic.getUuid().toString());

        if (index < chars.size()) {

            gatt.readCharacteristic(chars.get(index++));
        } else {
            index = 0;
            gatt.setCharacteristicNotification(chars.get(index), true);
            Log.d(TAG, "\tlistening for notification: "
                    + chars.get(index).getUuid().toString());
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt,
                                 BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        Log.d(TAG, "onDescriptorRead: " + gatt + " " + descriptor.getUuid().toString() + " " + status + Arrays.toString(descriptor.getValue()));
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt,
                                  BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.d(TAG, "onDescriptorWrite: " + gatt + " " + descriptor.getUuid().toString() + " " + status + Arrays.toString(descriptor.getValue()));

    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        Log.d(TAG, "onReliableWriteCompleted: " + gatt + " " + status);
    }

}
