package io.github.mmarkimark.llartv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.List;

public class CitySearchTest {
    // Shape returned by Open-Meteo's geocoding API for name=Girona&language=ca (postcodes trimmed).
    static final String GIRONA = "{\"results\":[{\"id\":3121456,\"name\":\"Girona\",\"latitude\":41.98311,\"longitude\":2.82493,"
            + "\"elevation\":81.0,\"feature_code\":\"PPLA2\",\"country_code\":\"ES\",\"timezone\":\"Europe/Madrid\","
            + "\"population\":100266,\"postcodes\":[\"17001\"],\"country\":\"Espanya\",\"admin1\":\"Catalunya\","
            + "\"admin2\":\"Província de Girona\",\"admin3\":\"Girona\"}],\"generationtime_ms\":0.15}";

    @Test public void theUrlSearchesTheNameInTheInterfaceLanguage() {
        assertEquals("https://geocoding-api.open-meteo.com/v1/search?name=Sant%20Cugat&count=10&language=ca&format=json",
                CitySearch.url("  Sant Cugat ", "ca"));
        assertEquals("https://geocoding-api.open-meteo.com/v1/search?name=L%27Hospitalet%20%26%20co&count=10&language=en&format=json",
                CitySearch.url("L'Hospitalet & co", "en"));
    }

    @Test public void aSearchNeedsAtLeastTwoLetters() {
        assertFalse(CitySearch.searchable(null));
        assertFalse(CitySearch.searchable(""));
        assertFalse(CitySearch.searchable("  G  "));
        assertTrue(CitySearch.searchable(" Gi "));
    }

    @Test public void aResultHasItsNameRegionCountryAndCoordinates() throws Exception {
        List<CitySearch.City> cities = CitySearch.parse(GIRONA);
        assertEquals(1, cities.size());
        CitySearch.City c = cities.get(0);
        assertEquals("Girona", c.name);
        assertEquals("Girona, Catalunya, Espanya", c.label());
        assertEquals("41.98311,2.82493", c.location());
    }

    @Test public void theLocationWrittenIsTheOneConfigReads() throws Exception {
        CitySearch.City c = CitySearch.parse(GIRONA).get(0);
        Config k = new Config();
        k.set("location", c.location());
        k.set("location_name", c.name);
        assertEquals(41.98311, k.lat, 1e-9);
        assertEquals(2.82493, k.lon, 1e-9);
        assertEquals("Girona", k.locationLabel());
    }

    @Test public void noMatchIsAnEmptyList() throws Exception {
        assertTrue(CitySearch.parse("{\"generationtime_ms\":0.28}").isEmpty());
    }

    @Test public void aMissingRegionOrCountryIsLeftOutAndARepeatIsShownOnce() throws Exception {
        List<CitySearch.City> cities = CitySearch.parse("{\"results\":["
                + "{\"name\":\"Andorra la Vella\",\"latitude\":42.50779,\"longitude\":1.52109,\"admin1\":\"Andorra la Vella\",\"country\":\"Andorra\"},"
                + "{\"name\":\"Nowhere\",\"latitude\":1.5,\"longitude\":-2.25}]}");
        assertEquals("Andorra la Vella, Andorra", cities.get(0).label());
        assertEquals("Nowhere", cities.get(1).label());
        assertEquals("1.50000,-2.25000", cities.get(1).location());
    }

    @Test public void aResultWithoutNameOrCoordinatesIsSkipped() throws Exception {
        List<CitySearch.City> cities = CitySearch.parse("{\"results\":["
                + "{\"name\":\"No coordinates\"},"
                + "{\"latitude\":1.0,\"longitude\":2.0},"
                + "{\"name\":\"Girona\",\"latitude\":41.98311,\"longitude\":2.82493}]}");
        assertEquals(1, cities.size());
        assertEquals("Girona", cities.get(0).name);
    }

    @Test public void anOkResponseIsParsed() throws Exception {
        assertEquals("Girona", CitySearch.parse(CitySearch.read(new FakeConnection(200, GIRONA))).get(0).name);
    }

    @Test public void aBodyOver256KibFailsInsteadOfFillingTheMemory() throws Exception {
        assertEquals(256 * 1024, CitySearch.MAX_BYTES);
        try {
            CitySearch.read(FakeConnection.ofSize(CitySearch.MAX_BYTES + 1));
            fail("an oversized body must fail");
        } catch (java.io.IOException expected) { /* the search says it failed and writes nothing */ }
    }
}
