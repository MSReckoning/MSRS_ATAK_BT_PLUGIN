package com.atakmap.android.msrsbluetooth;

import android.content.Intent;
import android.os.SystemClock;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapData;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.Date;

import gnu.nmea.Geocoordinate;
import gnu.nmea.Packet;
import gnu.nmea.PacketGGA;
import gnu.nmea.PacketRMC;
import gnu.nmea.SentenceHandler;
import java.nio.charset.StandardCharsets;


public class NMEAProcessor {
    private final MapView mapView;
    private final String MSRS = "MSRS";
    final private String SOURCE_COLOR = "#FFAFFF00";
    private final String TAG = "MSRSNmeaProcessor";

    private PacketRMC _rmc;
    private PacketGGA _gga;


    public NMEAProcessor(MapView mapView) {
        this.mapView = mapView;
    }


    public void processPacket(byte[] byteStream) {
        String ascii = new String(byteStream, StandardCharsets.UTF_8);
        processPacket(ascii);
    }

    public void processPacket(String ascii) {
        try {
            Log.d(TAG, "Processing" + " " + ascii);
            Packet packet = SentenceHandler.makePacket(ascii, false);
            if (packet instanceof PacketGGA) {
                _gga = (PacketGGA) packet;
            } else if (packet instanceof PacketRMC) {
                _rmc = (PacketRMC) packet;
                if (_rmc != null && _gga != null) {
                    fireGPSUpdate(_rmc, _gga);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Invalid NMEA message: " + e.getMessage());
        }
    }


    private void fireGPSUpdate(final PacketRMC rmc, final PacketGGA gga) {
        updateMapData(rmc, gga);
        sendGPSIntent();
    }
    private void sendGPSIntent() {
        Intent gpsReceived = new Intent();
        gpsReceived.setAction("com.atakmap.android.map.WR_GPS_RECEIVED");
        AtakBroadcast.getInstance().sendBroadcast(gpsReceived);
    }

    private void updateMapData(final PacketRMC rmc, final PacketGGA gga) {
        Marker item = this.mapView.getSelfMarker();
        if (item != null) {
            Geocoordinate pos = rmc.getPosition();
            Date time = rmc.getDate();
            double hae = EGM96.getHAE(pos.getLatitudeDegrees(), pos.getLongitudeDegrees(),
                    gga.getAltitude());

            final GeoPoint gp = new GeoPoint(pos.getLatitudeDegrees(),
                    pos.getLongitudeDegrees(), hae);

            final float dilution = (float) gga.getDilution();
            final float trackAngle = (float) rmc.getTrackAngle();
            final int fixQuality = gga.getFixQuality();

            final MapData data = this.mapView.getMapData();
            data.putString(MSRS + "LocationSource", MSRS);
            data.putDouble(MSRS + "LocationSpeed", (rmc.getGroundSpeed() / 1.85200));
            data.putFloat(MSRS + "LocationAccuracy", dilution);
            data.putFloat(MSRS + "LocationBearing", trackAngle);
            data.putString("locationSourcePrefix", MSRS);
            data.putBoolean(MSRS + "LocationAvailable", true);
            data.putString(MSRS + "LocationSource", MSRS);
            data.putString(MSRS + "LocationSourceColor", SOURCE_COLOR);
            data.putBoolean(MSRS + "LocationCallsignValid", true);
            data.putParcelable(MSRS + "Location", gp);
            data.putLong(MSRS + "LocationTime", SystemClock.elapsedRealtime());
            data.putLong(MSRS + "GPSTime", new CoordinatedTime().getMilliseconds());
            data.putInt(MSRS + "FixQuality", fixQuality);
            Log.d(TAG, "Mappy data updated for MSRS: " + gp + " with a fix quality: " + fixQuality);
        }
    }
}
