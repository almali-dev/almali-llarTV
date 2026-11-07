package io.github.mmarkimark.llartv;

/**
 * Where the weather is fetched for. With location_auto=on it is the TV's Android location when Android has one,
 * and the configured city (location, location_name; Barcelona by default) otherwise. Pure, so the JVM tests pin it;
 * HomeActivity reads the TV location and passes it in.
 */
final class WeatherPlace {
    final double lat, lon;
    final String label;      // the place shown with the source; empty for a TV location (no reverse geocoding)
    final boolean fromTv;

    private WeatherPlace(double lat, double lon, String label, boolean fromTv) {
        this.lat = lat; this.lon = lon; this.label = label; this.fromTv = fromTv;
    }

    /** @param tv the TV's {latitude, longitude}, or null when Android has none (or LlarTV may not read it) */
    static WeatherPlace resolve(Config conf, double[] tv) {
        if (conf.locationAuto && tv != null && tv.length == 2) return new WeatherPlace(tv[0], tv[1], "", true);
        return new WeatherPlace(conf.lat, conf.lon, conf.locationLabel(), false);
    }

    /**
     * Whether the screensaver, which never goes online or reads the TV location, may show the cached weather: only
     * when it was fetched for the configured city, or, in automatic, whatever place Home fetched it for (a cache that
     * does not say where, from before the place was kept, waits for Home's next fetch).
     */
    static boolean screensaverShows(Weather w, Config conf) {
        if (w == null) return false;
        return conf.locationAuto ? !Double.isNaN(w.lat) && !Double.isNaN(w.lon) : w.isFor(conf.lat, conf.lon);
    }
}
