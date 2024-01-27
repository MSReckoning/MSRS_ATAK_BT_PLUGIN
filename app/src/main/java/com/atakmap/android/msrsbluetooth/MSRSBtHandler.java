package com.atakmap.android.msrsbluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import com.atakmap.android.bluetooth.BluetoothASCIIClientConnection;
import com.atakmap.android.bluetooth.BluetoothConnection;
import com.atakmap.android.bluetooth.BluetoothCotManager;
import com.atakmap.android.bluetooth.BluetoothManager;
import com.atakmap.android.bluetooth.BluetoothReader;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.Collections;
import java.util.List;

@SuppressLint({"MissingPermission"})
public class MSRSBtHandler implements BluetoothManager.BluetoothReaderFactory {
    private static final String TAG = "MSRSBtHandler";
    private final List<String> deviceNames = Collections.singletonList("GNSS");
    private NMEAProcessor nmeaProcessor;

    public MSRSBtHandler(NMEAProcessor nmeaProcessor) {
        this.nmeaProcessor = nmeaProcessor;
    }

    public boolean matches(BluetoothDevice device) {
        Log.d(TAG, "Searching for external bluetooth device for: " + device);
        return deviceNames.contains(device.getName());
    }

    public BluetoothReader create(BluetoothDevice device) {
        Log.d(TAG, "Creating a device for" + " " + device.getName());
        return new ExternalBluetoothReader(device, nmeaProcessor);
    }
}

class ExternalBluetoothReader extends BluetoothReader {
    private final NMEAProcessor nmeaProcessor;

    public ExternalBluetoothReader(BluetoothDevice device, NMEAProcessor nmeaProcessor) {
        super(device);
        this.nmeaProcessor = nmeaProcessor;
    }

    @Override
    public void onRead(final byte[] data) {
        String ascii = new String(data);
        this.nmeaProcessor.processPacket(ascii);
    }

    @Override
    protected BluetoothConnection onInstantiateConnection(
            BluetoothDevice device) {
        return new BluetoothASCIIClientConnection(device,
                BluetoothConnection.MY_UUID_INSECURE);
    }

    @SuppressLint({"MissingPermission"})
    @Override
    public BluetoothCotManager getCotManager(MapView mapView) {
        BluetoothDevice device = connection.getDevice();
        return new BluetoothCotManager(this, mapView,
                device.getName().replace(" ", "").trim() + "."
                        + device.getAddress(),
                device.getName());
    }
}