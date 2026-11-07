package io.github.mmarkimark.llartv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeatherPlaceTest {
    static Config girona(boolean auto) {
        Config k = new Config();
        k.set("location", "41.98,2.82");
        k.set("location_name", "Girona");
        k.set("location_auto", auto ? "on" : "off");
        return k;
    }

    @Test public void withoutAutomaticTheCityIsUsedEvenWhenTheTvKnowsWhereItIs() {
        WeatherPlace p = WeatherPlace.resolve(girona(false), new double[]{40.4168, -3.7038});
        assertEquals(41.98, p.lat, 1e-9);
        assertEquals(2.82, p.lon, 1e-9);
        assertEquals("Girona", p.label);
        assertFalse(p.fromTv);
    }

    @Test public void automaticUsesTheTvLocationAndNamesOnlyTheSource() {
        WeatherPlace p = WeatherPlace.resolve(girona(true), new double[]{40.4168, -3.7038});
        assertEquals(40.4168, p.lat, 1e-9);
        assertEquals(-3.7038, p.lon, 1e-9);
        assertEquals("", p.label);   // no reverse geocoding: the card shows "Open-Meteo"
        assertTrue(p.fromTv);
    }

    @Test public void automaticWithoutATvLocationFallsBackToTheCity() {
        WeatherPlace p = WeatherPlace.resolve(girona(true), null);
        assertEquals(41.98, p.lat, 1e-9);
        assertEquals(2.82, p.lon, 1e-9);
        assertEquals("Girona", p.label);
        assertFalse(p.fromTv);
    }

    @Test public void automaticWithoutATvLocationOrACityIsBarcelona() {
        Config k = new Config();
        k.set("location_auto", "on");
        WeatherPlace p = WeatherPlace.resolve(k, null);
        assertEquals(Config.DEFAULT_LAT, p.lat, 1e-9);
        assertEquals(Config.DEFAULT_LON, p.lon, 1e-9);
        assertEquals("Barcelona", p.label);
    }

    @Test public void theScreensaverShowsOnlyWeatherOfTheConfiguredCity() throws Exception {
        Weather barcelona = Weather.parse(WeatherTest.JSON, 0).at(Config.DEFAULT_LAT, Config.DEFAULT_LON);
        assertTrue(WeatherPlace.screensaverShows(barcelona, new Config()));
        assertFalse(WeatherPlace.screensaverShows(barcelona, girona(false)));
        assertFalse(WeatherPlace.screensaverShows(Weather.parse(WeatherTest.JSON, 0), new Config()));   // no place
    }

    @Test public void inAutomaticTheScreensaverTrustsWhatHomeFetched() throws Exception {
        Weather madrid = Weather.parse(WeatherTest.JSON, 0).at(40.4168, -3.7038);
        assertTrue(WeatherPlace.screensaverShows(madrid, girona(true)));
    }
}
