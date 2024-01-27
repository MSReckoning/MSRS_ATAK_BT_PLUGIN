
package com.atakmap.android.msrsbluetooth;

import android.content.Context;

import com.atakmap.android.bluetooth.BtLowEnergyManager;
import com.atakmap.android.maps.MapView;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.coremap.log.Log;
import android.content.Intent;

import com.atakmap.android.bluetooth.BluetoothManager;


public class MSRSBluetoothComponent extends DropDownMapComponent {

    public static final String TAG = "MSRSBluetoothMapComponent";
    public Context pluginContext;
    public MapView view;

    public void onCreate(final Context context, Intent intent,
                         final MapView view) {
        context.setTheme(R.style.ATAKPluginTheme);
        if (pluginContext == null) {
            Log.d(TAG, "MSRSBluetooth loaded");
            super.onCreate(context, intent, view);
            pluginContext = context;
            this.view = view;

            NMEAProcessor nmeaProcessor = new NMEAProcessor(this.view);

            BluetoothManager.getInstance().addExternalBluetoothReader(
                    new MSRSBtHandler(nmeaProcessor));

            BtLowEnergyManager.getInstance().addExternalBtleHandler(
                    new MSRSBleHandler(nmeaProcessor));
        }
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }
}