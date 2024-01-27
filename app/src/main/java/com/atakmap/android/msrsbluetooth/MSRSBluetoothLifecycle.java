package com.atakmap.android.msrsbluetooth;


import com.atak.plugins.impl.AbstractPlugin;
import gov.tak.api.plugin.IServiceController;



public class MSRSBluetoothLifecycle extends AbstractPlugin {

   private final static String TAG = "MSRSBluetoothLifecycle";

   public MSRSBluetoothLifecycle(IServiceController serviceController) {
        super(serviceController, new MSRSBluetoothComponent());
    }
}

