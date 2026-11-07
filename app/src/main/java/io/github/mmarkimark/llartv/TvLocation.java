package io.github.mmarkimark.llartv;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

/**
 * The TV's Android location for the weather when location_auto=on (WeatherPlace). An Android TV has no GPS and often
 * no network location provider either, so this is best effort: no permission, no provider or no known fix are all
 * null, and the weather then uses the configured city (location, location_name; Barcelona by default).
 * ACCESS_COARSE_LOCATION is the one permission LlarTV asks for, and the one it reads.
 */
final class TvLocation {
    private TvLocation() {}

    /** Whether LlarTV may read the TV's location. */
    static boolean allowed(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** The newest last known fix of the enabled providers as {latitude, longitude}; null when Android has none. */
    static double[] last(Context c) {
        if (!allowed(c)) return null;
        try {
            LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;
            Location best = null;
            for (String p : lm.getProviders(true)) {   // only the enabled ones: a disabled provider has no fix
                try {
                    Location l = lm.getLastKnownLocation(p);
                    if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                } catch (Exception ignored) {}   // a provider of a finer permission (GPS) is skipped, not all of them
            }
            return best == null ? null : new double[]{best.getLatitude(), best.getLongitude()};
        } catch (Exception e) { return null; }   // no location services on this TV: the configured city stays
    }

    /**
     * Asks once for a fix from the network provider, the only one an Android TV can have, and runs done on the main
     * thread when one arrives. Returns whether the ask was made: without the permission or an enabled provider there
     * is nothing to wait for, and HomeActivity keeps the configured city.
     */
    static boolean request(Context c, Runnable done) {
        try {
            if (!allowed(c)) return false;
            LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null || !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return false;
            lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                @Override public void onLocationChanged(Location location) { done.run(); }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            }, Looper.getMainLooper());
            return true;
        } catch (Exception e) { return false; }   // best effort: no fix, the configured city stays
    }
}
